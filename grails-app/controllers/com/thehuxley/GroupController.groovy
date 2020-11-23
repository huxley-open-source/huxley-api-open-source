package com.thehuxley

import com.thehuxley.error.ErrorReason
import com.thehuxley.error.ErrorResponse
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.grails.web.json.JSONArray
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.springframework.http.HttpStatus

import java.text.MessageFormat

class GroupController {

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET", save: "POST", update: "PUT"]

    def groupService
    def userService
    def questionnaireService
    def submissionService
    def springSecurityService

    @Secured("permitAll")
    def index() {
        if (params.sort && !groupService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        def currentUser = springSecurityService.currentUser as User ?: null

        if (currentUser == null || currentUser.authorities.authority.contains('ROLE_ADMIN')) {
            def groups = groupService.list(params)

            response.setHeader("total", groups.totalCount as String)

            respond groups
        } else {

            UserInstitution institution = UserInstitution.findByUserAndInstitution(currentUser, currentUser.institution)

            if (institution && (institution.role == UserInstitution.Role.TEACHER || institution.role == UserInstitution.Role.ADMIN_INST)) {
                def groups = groupService.findAllByInstitution(currentUser.institution, params)

                response.setHeader("total", groups.totalCount as String)

                respond groups
            } else {
                def groups = groupService.findAllByUser(currentUser, params)

                response.setHeader("total", groups.totalCount as String)

                respond groups
            }
        }

    }

    @Secured("permitAll")
    def show(String id) {
        def currentUser = springSecurityService.currentUser as User ?: null

        try {
            respond groupService.get(Long.valueOf(id), currentUser)
        } catch (NumberFormatException e) {
            respond groupService.get(id, currentUser)
        }
    }

    @Secured("permitAll")
    def getByGroup(String key) {
        def currentUser = springSecurityService.currentUser as User ?: null

        respond groupService.get(key, currentUser)
    }

    @Secured("permitAll")
    def getUsers(Long groupId, Long userId) {
        if (userId) {
            respond User.get(userId)
        } else {
            if (params.sort && !userService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def group = Group.load(groupId)

            if (!canView(group)) {
                render status: HttpStatus.FORBIDDEN
                return
            }

            def users = userService.findAllByGroup(group, [:])

            response.setHeader("total", users.totalCount as String)

            respond users
        }
    }

    @Secured("permitAll")
    def getQuestionnaires(Long groupId, Long questionnaireId) {
        def group = Group.get(groupId)

        if (!group) {
            render status: HttpStatus.NOT_FOUND
            return
        }

        if (!canView(group)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        if (questionnaireId) {
            respond questionnaireService.findByGroup(Questionnaire.load(questionnaireId), group)
        } else {

            if (params.sort && !questionnaireService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            respond questionnaireService.findAllByGroup(group, questionnaireService.normalize(params))
        }
    }

    @Secured("permitAll")
    def getSubmissions(Long groupId, Long submissionId) {

        def group = Group.get(groupId)

        if (!group) {
            render status: HttpStatus.NOT_FOUND
            return
        }

        if (!canUpdate(group)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        if (submissionId) {
            respond submissionService.findByGroup(Submission.load(submissionId), group)
        } else {

            if (params.sort && !submissionService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            respond submissionService.findAllByGroup(group, submissionService.normalize(params))
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def countResolvers(Long groupId) {
        JSONArray arr = (JSONArray) request.JSON['problems']
        long[] ids = new long[arr.size()]

        for (int i = 0; i < arr.size(); i++) {
            ids[i] = arr.get(i)
        }

        render groupService.countResolversByProblem(groupId, ids)
        //render status: org.apache.http.HttpStatus.SC_OK;


    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def save() {
        Group group = deserialize(request.JSON, false, null)
        User user = springSecurityService.currentUser as User

        if (group.hasErrors()) {
            invalidGroup(group)
        } else {
            respond groupService.save(group, user)
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def update(Long id) {
        Group group = deserialize(request.JSON, true, id)

        if (!canUpdate(group)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        if (group.hasErrors()) {
            invalidGroup(group)
        } else {
            respond groupService.save(group)
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addUsers(Long groupId) {

        def users = request.JSON["users"] ? request.JSON["users"] : []

        Group group = Group.get(groupId)

        if (!group) {
            render status: HttpStatus.NOT_FOUND
            return
        }

        if (!canUpdate(group)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        users.each {
            if (it["id"]) {
                def user = User.get(it["id"] as Long)
                if (user) {
                    groupService.addToGroup(user, group, it["role"] as UserGroup.Role ?: UserGroup.Role.STUDENT)
                }
            } else if (it["email"]) {
                def user = User.findByEmail(it["email"] as String)
                if (user) {
                    groupService.addToGroup(user, group, it["role"] as UserGroup.Role ?: UserGroup.Role.STUDENT)
                } else {
                    groupService.inviteToGroup(it["email"] as String, group)
                }
            } else if (it["username"]) {
                def user = User.findByUsername(it["username"] as String)
                if (user) {
                    groupService.addToGroup(user, group, it["role"] as UserGroup.Role ?: UserGroup.Role.STUDENT)
                }
            }
        }

        users = userService.findAllByGroup(group, userService.normalize(params))

        response.setHeader("total", users.totalCount as String)

        respond users
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addUser(Long groupId, Long userId) {
        Group group = Group.load(groupId)
        User user = User.load(userId)


        if (!canUpdate(group)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        try {
            UserGroup.Role role = params.role ? UserGroup.Role.valueOf(params.role as String) : UserGroup.Role.STUDENT
            if (groupService.addToGroup(user, group, role)) {
                if (!params.skipResponse) {
                    def users = userService.findAllByGroup(group, userService.normalize(params))
                    response.setHeader("total", users.totalCount as String)
                    respond users
                } else {
                    render status: HttpStatus.OK
                }
            } else {
                render status: HttpStatus.BAD_REQUEST
            }
        } catch (Exception e) {
            e.printStackTrace()
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("isAuthenticated()")
    def getByKey() {
        def key = params.key as String

        if (!key) {
            key = request.JSON["key"]
        }

        if (key) {
            def group = Group.findByAccessKey(key as String)

            if (!group) {
                render status: HttpStatus.NOT_FOUND
                return
            }

            respond groupService.get(group.id, springSecurityService.currentUser as User)
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("isAuthenticated()")
    def addByKey() {
        def key = params.key as String
        def currentUser = springSecurityService.currentUser as User

        if (!key) {
            key = request.JSON["key"]
        }

        if (key) {
            def group = Group.findByAccessKey(key as String)

            if (group) {
                if (!UserGroup.findByUserAndGroup(currentUser, group)) {
                    log.info("join-group: { user: $currentUser.id, group: $group.id")
                    groupService.addToGroup(currentUser, group)
                    render status: HttpStatus.NO_CONTENT
                } else {
                    log.info("already-member: $key")
                    render status: HttpStatus.FORBIDDEN
                }
            } else {
                log.info("no-group-for-key: $key")
                render status: HttpStatus.NOT_FOUND
            }
        } else {
            log.info("no-key-to-join: { user: $currentUser.id }")
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_TEACHER_ASSISTANT', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getKey(Long groupId) {
        Group group = Group.load(groupId)

        if (canUpdate(group)) {

            if (!group.accessKey) {
                group = groupService.refreshAccessKey(group)
            }

            render ([key: group.accessKey] as JSON)
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def refreshKey(Long groupId) {
        Group group = Group.load(groupId)

        if (canUpdate(group)) {

            groupService.refreshAccessKey(group)

            if (group.hasErrors()) {
                validate(groupId)
            } else {
                render ([key: group.accessKey] as JSON)
            }
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def removeUser(Long groupId, Long userId) {
        Group group = Group.load(groupId)
        User user = User.load(userId)

        if (!canUpdate(group)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        try {
            groupService.removeFromGroup(user, group)
            def users = userService.findAllByGroup(group, userService.normalize(params))
            response.setHeader("total", users.totalCount as String)

            respond users
        } catch (Exception e) {
            e.printStackTrace()
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def exportQuizesToExcel(Long groupId) {

        Group group = Group.load(groupId)

        if (!canUpdate(group)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        def title = "huxley-group-export"

        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "attachment; filename=${title}.xls")
        questionnaireService.exportQuizesToExcel(groupId, response.outputStream);

    }

    def canView(Group group) {
        if (springSecurityService.currentUser) {
            groupService.hasGroupAccess(springSecurityService.currentUser as User, group, false)
        } else {
            false
        }
    }

    def canUpdate(Group group) {
        groupService.hasGroupAccess(springSecurityService.currentUser as User, group, true)
    }

    private def hasGroupAccess(Group group, boolean write) {
        User user = springSecurityService.currentUser as User

        if (!user) {
            return true
        }

        UserInstitution userInstitution = UserInstitution.findByUserAndInstitution(user, group.institution)

        if (user.roles.authority.contains("ROLE_ADMIN")) {
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

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def validate(Long groupId) {
        def group

        if (groupId) {
            group = deserialize(request.JSON, true, groupId)
        } else {
            group = deserialize(request.JSON, false, null)
        }

        if (group.hasErrors()) {
            invalidGroup(group)
        } else {
            render status: HttpStatus.ACCEPTED
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getFailingStudents(Long groupId) {

        if (params.sort && !userService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        respond userService.failingStudents(Group.get(groupId), userService.normalize(params))
    }

    @Secured("permitAll")
    def getData(Long groupId) {
        respond groupService.getData(Group.load(groupId), params.key)
    }


    Group deserialize(json, update, groupId) {

        User user = springSecurityService.currentUser as User

        UserInstitution userInstitution = null
        Institution institution = null

        try {
            if (user && json["institution"] && json["institution"]["id"]) {
                userInstitution = UserInstitution.findByUserAndInstitution(user, Institution.load(json["institution"]["id"] as Long))
            }
        } catch (MissingPropertyException e) {

        }

        if (userInstitution?.role == UserInstitution.Role.TEACHER ||
                userInstitution?.role == UserInstitution.Role.ADMIN_INST) {
            institution = userInstitution.institution
        }


        def group

        if (json["id"]) {
            group = Group.get(json["id"] as Long)
        } else {
            group = update ? Group.get(groupId as Long) : new Group()
        }


        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()

        group.name = (json["name"] as String) ?: group.name
        group.url = json["url"] as String ?: group.url
        group.description = json["description"] as String ?: group.description
        group.institution = institution ?: group.institution
        try {
            group.startDate = json["startDate"] ? formatter.parseDateTime(json["startDate"] as String).toDate() : group.startDate
            group.endDate = json["endDate"] ? formatter.parseDateTime(json["endDate"] as String).toDate() : group.endDate
        } catch (IllegalArgumentException e) {

        }

        group.validate()

        group
    }

    def invalidGroup(Group group) {

        def errors = []

        group.errors.each {
            it.getAllErrors().each {
                if (it.arguments[0] == "name") {

                    if (it.code == "unique") {
                        errors.add(ErrorReason.GROUP_NAME_MUST_BE_UNIQUE.setParams(it.arguments[2]))
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.GROUP_NAME_CANNOT_BE_BLANK)
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.GROUP_NAME_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.GROUP_NAME_TOO_SMALL.setParams(it.arguments[2]))
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.GROUP_NAME_CANNOT_BE_NULL)
                    }

                } else if (it.arguments[0] == "url") {

                    if (it.code == "unique") {
                        errors.add(ErrorReason.GROUP_URL_MUST_BE_UNIQUE.setParams(it.arguments[2]))
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.GROUP_URL_CANNOT_BE_BLANK)
                    }

                    if (it.code == "matches.invalid") {
                        errors.add(ErrorReason.GROUP_URL_NOT_MATCH.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.GROUP_URL_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.GROUP_URL_TOO_SMALL.setParams(it.arguments[2]))
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.GROUP_URL_CANNOT_BE_NULL)
                    }

                } else if (it.arguments[0] == "institution") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.GROUP_INSTITUTION_CANNOT_BE_NULL)
                    }
                } else if (it.arguments[0] == "endDate") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.GROUP_END_DATE_CANNOT_BE_NULL)
                    }
                } else if (it.arguments[0] == "startDate") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.GROUP_START_DATE_CANNOT_BE_NULL)
                    }
                } else {
                    errors.add(ErrorReason.GENERIC.setParams(it.code + " - " + MessageFormat.format(it.defaultMessage, it.arguments)))
                }
            }
        }

        if (group.endDate < group.startDate) {
            errors.add(ErrorReason.GROUP_END_DATE_CANNOT_BE_EARLIER_THAN_START_DATA)
        }

        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, errors)

        respond errorResponse, [status: errorResponse.httpStatus]
    }


}
