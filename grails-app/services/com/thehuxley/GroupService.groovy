package com.thehuxley

import grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.SessionFactory
import org.joda.time.format.DateTimeFormatter
import org.springframework.security.crypto.codec.Hex
import org.joda.time.format.ISODateTimeFormat

import java.security.MessageDigest

class GroupService {

    def userService
    def institutionService
    SessionFactory sessionFactory

    def get(groupId, User currentUser = null) {
        def group = Group.createCriteria().get {
            if (groupId instanceof Long) {
                eq('id', groupId)
            } else {
                eq('url', groupId)
            }
        }

        def response = [ group: group ]

        if (group) {
            response.teachers = userService.findAllByGroupAndRole(group, UserGroup.Role.TEACHER, [:])
            response.teacherAssistants = userService.findAllByGroupAndRole(group, UserGroup.Role.TEACHER_ASSISTANT, [:])

            if (currentUser) {
                response.role = UserGroup.findByUserAndGroup(currentUser, group)?.role
            }
        }

        response
    }

    def changeRole(Group group, User user, UserGroup.Role role) {

        log.info("change-user-role: { user: $user.id, group: $group.id, role: $role }")

        def userGroup = UserGroup.findByUserAndGroup(user, group)

        if (userGroup) {
            userGroup.role = role
            userGroup.save()
        } else {
            new UserGroup(user: user, group: group, role: role).save()

            if (role == UserGroup.Role.STUDENT) {
                if (!UserInstitution.findByUserAndInstitution(user, group.institution)) {
                    institutionService.addToInstitution(user, group.institution, UserInstitution.Role.STUDENT)
                }
            }
        }
    }

    def addToGroup(User user, Group group, UserGroup.Role role = UserGroup.Role.STUDENT) {

        // carrega usuário do banco e não do cache
        user = User.get(user.id)
        def userInstitution = UserInstitution.findByUserAndInstitution(user, group.institution)

        if (role == UserGroup.Role.TEACHER) {

            if (userInstitution) {
                if (userInstitution.role != UserInstitution.Role.TEACHER) {
                    institutionService.changeRoleKeepGreatest(user, group.institution, UserInstitution.Role.TEACHER)
                }

            } else {
                institutionService.addToInstitution(user, group.institution, UserInstitution.Role.TEACHER)
            }

            changeRole(group, user, role)

            return true

        } else if (role == UserGroup.Role.TEACHER_ASSISTANT) {

            if (userInstitution) {
                if (userInstitution.role != UserInstitution.Role.TEACHER_ASSISTANT) {
                    institutionService.changeRoleKeepGreatest(user, group.institution, UserInstitution.Role.TEACHER_ASSISTANT)
                }
            } else {
                institutionService.addToInstitution(user, group.institution, UserInstitution.Role.TEACHER_ASSISTANT)
            }

            changeRole(group, user, role)
            return true

        } else {

            if (userInstitution) {
                if (userInstitution.role != UserInstitution.Role.STUDENT) {
                    institutionService.changeRoleKeepGreatest(user, group.institution, UserInstitution.Role.STUDENT)
                }
            } else {
                institutionService.addToInstitution(user, group.institution, UserInstitution.Role.STUDENT)
            }

            changeRole(group, user, role)
        }

        return true
    }

    def removeFromGroup(User user, Group group) {
        UserGroup.findAllByUserAndGroup(user, group)*.delete()
    }

    def save(Group group, User user = null) {

        log.info(group.id ? "group-update: $group.id" : "group-create: $group.name")
        group.save()

        if (user) {
            changeRole(group, user, UserGroup.Role.TEACHER)
        }

        group
    }

    def list(Map params) {
        Group.createCriteria().list([max: params.max, offset: params.offset], getCriteria(params))
    }

    def findByInstitution(Group group, Institution institution) {
        group.institution?.id == institution.id ? group : null
    }

    def findAllByInstitution(Institution institution, Map params) {

        params.institution = institution.id

        Group.createCriteria().list([max: params.max, offset: params.offset], getCriteria(params))
    }

    def findByUser(Group group, User user) {
        UserGroup.findByGroupAndUser(group, user)?.group
    }

    def findAllByUser(User user, Map params) {
        Group.createCriteria().list([max: params.max, offset: params.offset]) {
            createAlias('usersGroup', 'ug')
            eq("ug.user", user)
            and getCriteria(params)
        }
    }

    def findAllInTopCoder(Group group, Map params) {
        def users = UserGroup.findAllByGroup(group).user

        TopCoder.createCriteria().list([max: params.max, offset: params.offset]) {
            and {
                if (users && !users.empty) {
                    inList('user', users)
                }

                user {
                    or {
                        if (params.q) {
                            ilike("name", "%$params.q%")
                            ilike("email", "%$params.q%")
                            ilike("username", "%$params.q%")
                        }
                    }
                }
            }
            order("points", "desc")
        }
    }

    def countResolversByProblem(long groupId, long[] problems) {
        sessionFactory.currentSession.createSQLQuery(
                """select problem_id,count(distinct(user_id))
				from submission where evaluation = :eval and
				user_id in (select user_id from user_group uc where uc.group_id = :group and role = :role) and
				problem_id in (:problems) group by problem_id""")
				.setParameter("group", groupId)
				.setParameter("role", UserGroup.Role.STUDENT.ordinal())
				.setParameter("eval", Submission.Evaluation.CORRECT.ordinal())
				.setParameterList("problems", problems)
				.list()
    }

    def isMember(long groupId, long userId) {
        def count = sessionFactory.currentSession.createSQLQuery("""
                SELECT count(*) from user_group WHERE group_id = :group AND user_id = :user""")
                .setParameter("group", groupId)
                .setParameter("user", userId)
                .uniqueResult()
        return count > 0
    }

    def hasGroupAccess(User user, Group group, boolean write) {

        if (!user) return false;

        UserInstitution userInstitution = UserInstitution.findByUserAndInstitution(user, group.institution)

        if (user.getAuthorities().authority.contains("ROLE_ADMIN")) {
            return true
        }

        boolean institutionalAccess = userInstitution &&
                (userInstitution.role == UserInstitution.Role.ADMIN_INST ||
                        (!write && userInstitution.role == UserInstitution.Role.TEACHER));

        if (institutionalAccess) {
            return true;
        }

        UserGroup userGroup = UserGroup.findByUserAndGroup(user, group)

        if (!userGroup || (write && userGroup.role == UserGroup.Role.STUDENT)) {
            return false
        }

        return true
    }

    Closure getCriteria(Map params) {

        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()

        return {
            and {
                or {
                    if (params.q) {
                        ilike("name", "%$params.q%")
                        ilike("url", "%$params.q%")
                        ilike("description", "%$params.q%")
                    }
                }

                !params.institution ?: eq("institution", Institution.load(params.institution as Long))

                !params.startDate ?: eq("startDate", formatter.parseDateTime(params.startDate as String).toDate())
                !params.startDateGt ?: gt("startDate", formatter.parseDateTime(params.startDateGt as String).toDate())
                !params.startDateGe ?: ge("startDate", formatter.parseDateTime(params.startDateGe as String).toDate())
                !params.startDateLt ?: lt("startDate", formatter.parseDateTime(params.startDateLt as String).toDate())
                !params.startDateLe ?: le("startDate", formatter.parseDateTime(params.startDateLe as String).toDate())
                !params.startDateNe ?: ne("startDate", formatter.parseDateTime(params.startDateNe as String).toDate())

                !params.endDate ?: eq("endDate", formatter.parseDateTime(params.endDate as String).toDate())
                !params.endDateGt ?: gt("endDate", formatter.parseDateTime(params.endDateGt as String).toDate())
                !params.endDateGe ?: ge("endDate", formatter.parseDateTime(params.endDateGe as String).toDate())
                !params.endDateLt ?: lt("endDate", formatter.parseDateTime(params.endDateLt as String).toDate())
                !params.endDateLe ?: le("endDate", formatter.parseDateTime(params.endDateLe as String).toDate())
                !params.endDateNe ?: ne("endDate", formatter.parseDateTime(params.endDateNe as String).toDate())

                !params.dateCreated ?: eq("dateCreated", formatter.parseDateTime(params.dateCreated as String).toDate())
                !params.dateCreatedGt ?: gt("dateCreated", formatter.parseDateTime(params.dateCreatedGt as String).toDate())
                !params.dateCreatedGe ?: ge("dateCreated", formatter.parseDateTime(params.dateCreatedGe as String).toDate())
                !params.dateCreatedLt ?: lt("dateCreated", formatter.parseDateTime(params.dateCreatedLt as String).toDate())
                !params.dateCreatedLe ?: le("dateCreated", formatter.parseDateTime(params.dateCreatedLe as String).toDate())
                !params.dateCreatedNe ?: ne("dateCreated", formatter.parseDateTime(params.dateCreatedNe as String).toDate())

                !params.lastUpdated ?: eq("lastUpdated", formatter.parseDateTime(params.lastUpdated as String).toDate())
                !params.lastUpdatedGt ?: gt("lastUpdated", formatter.parseDateTime(params.lastUpdatedGt as String).toDate())
                !params.lastUpdatedGe ?: ge("lastUpdated", formatter.parseDateTime(params.lastUpdatedGe as String).toDate())
                !params.lastUpdatedLt ?: lt("lastUpdated", formatter.parseDateTime(params.lastUpdatedLt as String).toDate())
                !params.lastUpdatedLe ?: le("lastUpdated", formatter.parseDateTime(params.lastUpdatedLe as String).toDate())
                !params.lastUpdatedNe ?: ne("lastUpdated", formatter.parseDateTime(params.lastUpdatedNe as String).toDate())
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
                "url",
                "description",
                "startDate",
                "endDate",
                "dateCreated",
                "lastUpdated",
                "institution"
        ].contains(param)
    }

    def refreshAccessKey(Group group) {
        group.accessKey = generateAccessKey(group.id)
        group.save()
    }

    def generateAccessKey(Long id) {
        def hashKey = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest((new Random().nextInt() + new Date().toString()).bytes)))

        (toAlpha(id) + hashKey.substring(0, 4)).toUpperCase()
    }

    def toAlpha(Long n) {

        def alpha = ((0..9).collect { it as String }) + ('A'..'Z')
        def size = alpha.size()
        def result = []
        def div = (n / size).toInteger()
        result.push(n - (div * size))
        n = div

        while (n > size) {
            div = (n / size).toInteger()
            result.push(n - (div * size))
            n = div
        }

        if (div > 0) {
            result.push(div.toInteger())
        }

        def ret = ""

        result.each {
            ret += alpha[it as Integer]
        }

        ret.reverse()
    }

}
