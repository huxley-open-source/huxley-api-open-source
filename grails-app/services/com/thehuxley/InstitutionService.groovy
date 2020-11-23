package com.thehuxley

import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.web.multipart.commons.CommonsMultipartFile

class InstitutionService {

    def userService
    def grailsApplication
    def imageService

    def get(Long institutionId, User currentUser = null, Institution.Status status = null) {
        def institution = Institution.createCriteria().get {
            eq('id', institutionId)
            if (status) {
                eq('status', status)
            }
        }

        if (currentUser && institution) {
            if (currentUser) {
                institution.metaClass.role = UserInstitution.findByUserAndInstitution(currentUser, institution)?.role
            }
        }

        institution
    }

    def addToInstitution(User user, Institution institution, UserInstitution.Role role = UserInstitution.Role.STUDENT) {

        log.info("add-to-institution: { user: $user.id, institution: $institution.id, role: $role }")

        if (!UserInstitution.findByUserAndInstitution(user, institution)) {
            new UserInstitution(institution: institution, user: user, role: role).save()

            if (!user.institution) {
                user.institution = institution
                user.save()
            }

            userService.refreshRoles(user)

            true
        } else {
            changeRole(user, institution, role)
        }
    }

    def removeFromInstitution(User user, Institution institution) {

        log.info("remove-from-institution: { user: $user.id, institution: $institution.id }")

        def userInstitution = UserInstitution.findByUserAndInstitution(user, institution)

        if (userInstitution) {
            userInstitution.delete()
            userService.refreshRoles(user)

            Group.findAllByInstitution(institution).each {
                UserGroup.findAllByUserAndGroup(user, it)*.delete()
            }

            if (user.institution == institution) {
                user.institution = null
                user.save()
            }

            true
        } else {
            false
        }
    }

    def changeRole(User user, Institution institution, UserInstitution.Role role = UserInstitution.Role.STUDENT) {

        log.info("change-institution-role: { user: $user.id, institution: $institution.id, role: $role }")

        def userInstitution = UserInstitution.findByUserAndInstitution(user, institution)

        if (userInstitution) {
            userInstitution.role = role
            userInstitution.save()
            userService.refreshRoles(user)

            if (role == UserInstitution.Role.STUDENT) {
                Group.findAllByInstitution(institution).each {
                    UserGroup.findAllByUserAndGroup(user, it).each {
                        it.role == UserGroup.Role.STUDENT
                        it.save()
                    }
                }
            }

            if (role == UserInstitution.Role.TEACHER_ASSISTANT) {
                Group.findAllByInstitution(institution).each {
                    UserGroup.findAllByUserAndGroupAndRole(user, it, UserGroup.Role.TEACHER).each {
                        it.role == UserGroup.Role.TEACHER_ASSISTANT
                        it.save()
                    }
                }
            }

            true
        } else {
            false
        }
    }

    def normalizeInInstitution(User user, Institution institution, UserInstitution.Role role = UserInstitution.Role.STUDENT) {
        if (!UserInstitution.findByUserAndInstitution(user, institution)) {

            new UserInstitution(institution: institution, user: user, role: role).save()

            if (!user.institution) {
                user.institution = institution
                user.save()
            }

            userService.refreshRoles(user)

            true
        } else {
            changeRoleKeepGreatest(user, institution, role)
        }
    }

    def changeRoleKeepGreatest(User user, Institution institution, UserInstitution.Role role = UserInstitution.Role.STUDENT) {
        def userInstitution = UserInstitution.findByUserAndInstitution(user, institution)

        if (userInstitution) {

            def newRole = userInstitution.role

            if (role == UserInstitution.Role.ADMIN_INST) {
                newRole = UserInstitution.Role.ADMIN_INST
            } else if (role == UserInstitution.Role.TEACHER && userInstitution.role != UserInstitution.Role.ADMIN_INST) {
                newRole = UserInstitution.Role.TEACHER
            } else if (role == UserInstitution.Role.TEACHER_ASSISTANT &&
                    (userInstitution.role == UserInstitution.Role.STUDENT ||
                            userInstitution.role == UserInstitution.Role.TEACHER_ASSISTANT)) {
                newRole = UserInstitution.Role.TEACHER_ASSISTANT
            }

            if (newRole != userInstitution.role) {
                userInstitution.role = newRole
                userInstitution.save()
                userService.refreshRoles(user)
            }

            true
        } else {
            false
        }
    }

    def uploadImage(CommonsMultipartFile file) {
        String path = grailsApplication.config.huxleyFileSystem.institution.images.dir + System.getProperty("file.separator")

        imageService.uploadImage(path, file)
    }

    def cropImage(Institution institution, String filename, Integer x, Integer y, Integer width, Integer height) {
        String path = grailsApplication.config.huxleyFileSystem.institution.images.dir + System.getProperty("file.separator")

        institution.logo = imageService.crop(path, filename, x, y, width, height)
        institution.save()
    }

    def getImage(String key, Integer width = 0, Integer height = 0) {
        String path = grailsApplication.config.huxleyFileSystem.institution.images.dir + System.getProperty("file.separator")

        imageService.getImage(path, key, width, height)
    }

    def changeStatus(Institution institution, Institution.Status status) {
        institution.status = status
        institution.save(flush:true)

        def adminInst = UserInstitution.findByInstitutionAndRole(institution, UserInstitution.Role.ADMIN_INST)
        
        if (status == Institution.Status.APPROVED) {
            if (adminInst) {
                def user = adminInst.user;
                save(institution, user)
                changeRole(user, institution, UserInstitution.Role.ADMIN_INST)
                if (user.institution == null) {
                    user.setInstitution(institution);
                    userService.save(user);
                }
            }
        } else {
            if (adminInst) {
                if (!UserInstitution.findByUserAndRoleAndInstitutionNotEqual(adminInst.user, UserInstitution.Role.ADMIN_INST, institution)) {
                    if (adminInst.user.hasRole(Role.findByAuthority("ROLE_ADMIN_INST"))) {
                        adminInst.user.remove(Role.findByAuthority("ROLE_ADMIN_INST"))
                    }
                }
            }
        }

        institution
    }

    def save(Institution institution, User user = null) {
        def inst = institution.save()

        if (user) {
            new UserInstitution(user: user, institution: institution, role: UserInstitution.Role.ADMIN_INST).save()
        }

        inst
    }

    def list(Map params, Institution.Status status = null) {
        Institution.createCriteria().list([max: params.max, offset: params.offset], getCriteria(params, status))
    }

    def findByUser(Institution institution, User user, Institution.Status status = null) {
        def userInstitution = UserInstitution.findByInstitutionAndUser(institution, user)
        status ? (userInstitution?.institution?.status == status ? userInstitution.institution : null) : institution
    }

    def findAllByUser(User user, Map params, Institution.Status status = null) {
        Institution.createCriteria().list([max: params.max, offset: params.offset]) {
            createAlias('usersInstitution', 'ui')
            eq('ui.user', user)
            eq("status", status)
        }
    }

    Closure getCriteria(Map params, Institution.Status status) {
        return {
            and {
                or {
                    ilike("name", "%$params.q%")
                    ilike("acronym", "%$params.q%")
                }
                if (status) {
                    eq("status", status)
                }
            }

            order(params.sort ?: "name", params.order ?: "asc")
        }
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)
        params.q = params.q ?: ""

        return params
    }

    boolean isSortable(param) {
        [
                "id",
                "name",
                "phone",
                "photo",
                "status",
                "acronym"
        ].contains(param)
    }

}
