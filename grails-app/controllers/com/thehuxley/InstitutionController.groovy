package com.thehuxley

import com.thehuxley.error.ErrorReason
import com.thehuxley.error.ErrorResponse
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.web.mapping.LinkGenerator
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.JSONException
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.commons.CommonsMultipartFile

import java.text.MessageFormat

class InstitutionController {

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET"]

    def institutionService
    def groupService
    def userService
    def springSecurityService
    LinkGenerator grailsLinkGenerator

    @Secured("permitAll")
    def index() {

        if (params.sort && !institutionService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        def currentUser = springSecurityService.currentUser as User ?: null
        Institution.Status status = Institution.Status.APPROVED

        if (currentUser) {
            def authorities = currentUser.getAuthorities().authority
            if (authorities.contains("ROLE_ADMIN")) {
                status = params.status ? Institution.Status.valueOf(params.status as String) : null
            }
        }

        def institutions = institutionService.list(institutionService.normalize(params), status)

        response.setHeader("total", institutions.totalCount as String)

        respond institutions
    }

    @Secured("permitAll")
    def show(Long id) {

        def currentUser = springSecurityService.currentUser as User ?: null
        Institution.Status status = Institution.Status.APPROVED
        Institution institution = Institution.get(id)

        if (currentUser) {
            if (UserInstitution.findByUserAndInstitutionAndRole(currentUser, institution, UserInstitution.Role.ADMIN_INST)
                    || currentUser.authorities.authority.contains("ROLE_ADMIN")) {
                status = null
            }
        }

        respond institutionService.get(id, currentUser, status)
    }

    @Secured("permitAll")
    def getGroups(Long institutionId, Long groupId) {

        def institution = Institution.load(institutionId)

        if (groupId) {
            respond groupService.findByInstitution(Group.load(groupId), institution)
        } else {

            if (params.sort && !groupService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def groups = groupService.findAllByInstitution(institution, params)

            response.setHeader("total", groups.totalCount as String)

            respond groups
        }
    }

    @Secured("permitAll")
    def getUsers(Long institutionId, Long userId) {

        def institution = Institution.load(institutionId)

        if (userId) {
            respond userService.findByInstitution(User.load(userId), institution)
        } else {

            if (params.sort && !userService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def users = userService.findAllByInstitution(institution, userService.normalize(params))

            response.setHeader("total", users.totalCount as String)

            respond users
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def changeStatus(Long institutionId) {
        def status = null

        try {
            status = params.status ? Institution.Status.valueOf(params.status as String) : null
        } catch (Exception e) {

        }

        try {
            if (!status) {
                status = request.JSON["status"] ? Institution.Status.valueOf(request.JSON["status"] as String) : null
            }
        } catch (Exception e) {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        if (status) {
            Institution institution = Institution.load(institutionId)
            onValid institution, {
                render institutionService.changeStatus(institution, status)
            }

        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("permitAll")
    def save() {
        User currentUser = springSecurityService.currentUser as User
        def institution = deserialize(false, null) as Institution

        onValid institution, {
            respond institutionService.save(institution, currentUser)
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def update(Long id) {
        User currentUser = springSecurityService.currentUser as User
        Institution institution = Institution.get(id)

        if (institution) {
            if (currentUser &&
                    (currentUser.authorities.authority.contains("ROLE_ADMIN") ||
                            UserInstitution.findByUserAndInstitutionAndRole(currentUser, institution, UserInstitution.Role.ADMIN_INST))) {
                institution = deserialize(true, institution.id) as Institution
                onValid institution, {
                    respond institutionService.save(institution, currentUser)
                }
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("permitAll")
    def validate() {
        onValid deserialize(false, null) as Institution, {
            render status: HttpStatus.ACCEPTED
        }
    }

    @Secured("permitAll")
    def getLogo(Long institutionId) {
        Institution institution = Institution.get(institutionId)

        if (institution) {
            getLogoByKey(institution.logo)
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("isAuthenticated()")
    def uploadLogo(Long institutionId) {

        User currentUser = springSecurityService.currentUser as User
        Institution institution = Institution.get(institutionId)

        if (params.file) {

            def kb = 1024
            def MIN_SIZE = 1 * kb
            def MAX_SIZE = 5 * (kb * kb)
            def ALLOWED_MIME_TYPE = ["image/jpg", "image/jpeg", "image/png"]

            def fileSize = (params.file as CommonsMultipartFile).size

            if (ALLOWED_MIME_TYPE.contains((params.file as CommonsMultipartFile).contentType)) {
                if ((fileSize >= MIN_SIZE) && (fileSize <= MAX_SIZE)) {
                    if (institution) {
                        if (currentUser.authorities.authority.contains("ROLE_ADMIN") ||
                                UserInstitution.findByInstitutionAndUser(institution, currentUser)?.role
                                == UserInstitution.Role.ADMIN_INST) {
                            def file = institutionService.uploadImage(params.file as CommonsMultipartFile)
                            respond(
                                    ([
                                            _links: [
                                                    self: grailsLinkGenerator.link(
                                                            controller: "institutions",
                                                            action: "logo",
                                                            absolute: true
                                                    ) + "/" + file.name
                                            ],
                                            name  : file.name
                                    ] as JSON) as String
                            )
                        } else {
                            render status: HttpStatus.FORBIDDEN
                        }
                    } else {
                        render status: HttpStatus.BAD_REQUEST
                    }
                } else {
                    forward(controller: "Error", action: "invalidLogoSize")
                }
            } else {
                forward(controller: "Error", action: "invalidLogoMimeType")
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("isAuthenticated()")
    def cropImage(Long institutionId) {

        def institution = Institution.get(institutionId)
        def currentUser = springSecurityService.currentUser as User

        if (institution) {
            if (currentUser.authorities.authority.contains("ROLE_ADMIN") ||
                    UserInstitution.findByInstitutionAndUser(institution, currentUser)?.role
                    == UserInstitution.Role.ADMIN_INST) {
                def json = request.JSON

                if (json["filename"]) {
                    respond institutionService.cropImage(
                            institution,
                            json["filename"] as String,
                            json["x"] as Integer ?: 0,
                            json["y"] as Integer ?: 0,
                            json["width"] as Integer ?: 400,
                            json["height"] as Integer ?: 300
                    )
                } else {
                    render status: HttpStatus.BAD_REQUEST
                }
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("permitAll")
    def getLogoByKey(String key) {

        def width = params["width"] as Integer ?: 0
        def height = params["height"] as Integer ?: 0

        File file = institutionService.getImage(key, width, height)

        if (file) {
            response.setContentType("image/png")
            response.setContentLength(file.bytes.length)
            response.setHeader("Content-disposition", "filename=${file.name}")
            response.outputStream << file.bytes
            response.outputStream.flush()
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addUser(Long institutionId, Long userId) {
        Institution institution = Institution.load(institutionId)
        User user = User.load(userId)

        if (!canUpdate(institution)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        try {
            UserInstitution.Role role = params.role ? UserInstitution.Role.valueOf(params.role as String) : UserInstitution.Role.STUDENT
            if (institutionService.addToInstitution(user, institution, role)) {
                if (!params.skipResponse) {
                    respond userService.findAllByInstitution(institution, userService.normalize(params))
                }
                render HttpStatus.OK
            } else {
                render status: HttpStatus.BAD_REQUEST
            }
        } catch (Exception e) {
            e.printStackTrace()
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addUsers(Long institutionId) {
        def users = request.JSON["users"] ? request.JSON["users"] : []
        Institution institution = Institution.load(institutionId)
        if (!canUpdate(institution)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        users.each {
            UserInstitution.Role role = it["role"] ? UserInstitution.Role.valueOf(it["role"] as String) : UserInstitution.Role.STUDENT
            if (it["id"]) {
                def user = User.get(it["id"] as Long)
                if (user) {
                    institutionService.addToInstitution(user, institution, role)
                }
            } else if (it["email"]) {
                def user = User.findByEmail(it["email"] as String)
                if (user) {
                    institutionService.addToInstitution(user, institution, role)
                }
            } else if (it["username"]) {
                def user = User.findByUsername(it["username"] as String)
                if (user) {
                    institutionService.addToInstitution(user, institution, role)
                }
            }

        }

        respond userService.findAllByInstitution(institution, userService.normalize(params))

    }

    @Secured("hasAnyRole('ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def removeUser(Long institutionId, Long userId) {
        Institution institution = Institution.load(institutionId)
        User user = User.load(userId)

        if (!canUpdate(institution)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        try {
            institutionService.removeFromInstitution(user, institution)
            render status: HttpStatus.NO_CONTENT
        } catch (Exception e) {
            e.printStackTrace()
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def normalizeRoles(Long institutionId) {
        try {
            def institution = Institution.read(institutionId)

            if (institution) {

                def instAdmins = UserInstitution.findAllByInstitutionAndRole(institution, UserInstitution.Role.ADMIN_INST).user

                UserInstitution.withNewTransaction {
                    UserInstitution.findAllByInstitution(institution)*.delete()
                }

                instAdmins.each {
                    institutionService.addToInstitution(it, institution, UserInstitution.Role.ADMIN_INST)
                }

                Group.findAllByInstitution(institution).each { Group group ->
                    UserGroup.findAllByGroup(group).each { UserGroup userGroup ->

                        def roleInInstitution = UserInstitution.Role.STUDENT

                        if (userGroup.role == UserGroup.Role.TEACHER) {
                            roleInInstitution = UserInstitution.Role.TEACHER
                        } else if (userGroup.role == UserGroup.Role.TEACHER_ASSISTANT) {
                            roleInInstitution = UserInstitution.Role.TEACHER_ASSISTANT
                        }


                        institutionService.normalizeInInstitution(userGroup.user, institution, roleInInstitution)
                    }
                }

                render status: HttpStatus.NO_CONTENT
                return
            }

            render status: HttpStatus.NOT_FOUND
        } catch (Exception e) {
            log.error(e.message, e)

            render status: HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    def canUpdate(Institution institution) {

        User user = springSecurityService.currentUser as User

        if (UserInstitution.findByUserAndInstitutionAndRole(user, institution, UserInstitution.Role.ADMIN_INST) ||
                user.getAuthorities().authority.contains("ROLE_ADMIN")) {
            return true
        }

        return false
    }


    def onValid(Institution institution, c) {
        if (institution.hasErrors()) {
            invalidInstitution(institution)
        } else {
            c()
        }
    }


    def deserialize(update, id) {
        def institution = update ? Institution.get(id as Long) : new Institution()

        def json = request.JSON

        institution.name = json["name"] as String ?: institution.name
        institution.acronym = json["acronym"] as String ?: institution.acronym
        institution.logo = json["logo"] as String ?: institution.logo

        institution.validate()

        return institution
    }

    def invalidInstitution(Institution institution) {

        def errors = []

        institution.errors.each {
            it.getAllErrors().each {
                if (it.arguments[0] == "name") {

                    if (it.code == "unique") {
                        errors.add(ErrorReason.INSTITUTION_NAME_MUST_BE_UNIQUE.setParams(it.arguments[2]))
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.INSTITUTION_NAME_CANNOT_BE_BLANK)
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.INSTITUTION_NAME_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.INSTITUTION_NAME_TOO_SMALL.setParams(it.arguments[2]))
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.INSTITUTION_NAME_CANNOT_BE_NULL)
                    }

                } else if (it.arguments[0] == "acronym") {
                    if (it.code == "unique") {
                        errors.add(ErrorReason.INSTITUTION_ACRONYM_MUST_BE_UNIQUE.setParams(it.arguments[2]))
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.INSTITUTION_ACRONYM_CANNOT_BE_BLANK)
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.INSTITUTION_ACRONYM_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.INSTITUTION_ACRONYM_TOO_SMALL.setParams(it.arguments[2]))
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.INSTITUTION_ACRONYM_CANNOT_BE_NULL)
                    }
                } else {
                    errors.add(ErrorReason.GENERIC.setParams(it.code + " - " + MessageFormat.format(it.defaultMessage, it.arguments)))
                }
            }
        }

        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, errors)

        respond errorResponse, [status: errorResponse.httpStatus]
    }

}
