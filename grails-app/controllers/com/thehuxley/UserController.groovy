package com.thehuxley

import com.thehuxley.error.ErrorReason
import com.thehuxley.error.ErrorResponse
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import grails.util.Environment
import org.hibernate.FetchMode
import org.springframework.http.HttpStatus
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.i18n.LocaleContextHolder as LCH

import java.text.MessageFormat

class UserController {

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET", save: "POST", update: "PUT"]

    def userService
    def institutionService
    def groupService
    def questionnaireService
    def problemService
    def submissionService
    def springSecurityService
    def mailService
    def messageSource
    
    @Secured('permitAll')
    def index() {
        if (params.sort && !userService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        def users = userService.list(userService.normalize(params))

        response.setHeader("total", users.totalCount as String)

        respond users
    }

    @Secured('permitAll')
    def show(Long id) {
        def user = User.get(id)

        if (user) {
            respond user
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured('permitAll')
    def getInstitutions(Long userId, Long institutionId) {

        def user = User.load(userId)

        if (institutionId) {
            respond institutionService.findByUser(Institution.load(institutionId), user, Institution.Status.APPROVED)
        } else {

            if (params.sort && !institutionService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }
            def institutions = institutionService.findAllByUser(user, institutionService.normalize(params), Institution.Status.APPROVED)

            response.setHeader("total", institutions.totalCount as String)

            respond institutions
        }
    }

    @Secured('permitAll')
    def getGroups(Long userId, Long groupId) {
        def user = User.load(userId)
        if (groupId) {
            respond groupService.findByUser(Group.load(groupId), user)
        } else {

            if (params.sort && !groupService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def groups = groupService.findAllByUser(user, groupService.normalize(params))

            response.setHeader("total", groups.totalCount as String)

            respond groups
        }
    }

    @Secured('permitAll')
    def getProblemData(Long userId, Long problemId) {
        respond problemService.getData(Problem.load(problemId), User.load(userId))
    }

    @Secured('permitAll')
    def getProblems(Long userId, Long problemId) {
        def user = User.get(userId)

        if (!user) {
            render status: HttpStatus.NOT_FOUND
            return
        }

        def currentUser = springSecurityService.currentUser as User

        Problem.Status status = Problem.Status.ACCEPTED
        if (currentUser) {
            def authorities = currentUser.getAuthorities().authority

            if ((authorities.contains("ROLE_TEACHER_ASSISTANT")
                    || authorities.contains("ROLE_TEACHER")
                    || authorities.contains("ROLE_ADMIN_INST")
                    || authorities.contains("ROLE_ADMIN"))) {
                status = params.status ? Problem.Status.valueOf(params.status as String) : null
            }
        }

        if (problemId) {
            respond problemService.findByUser(problemId, user, status)
        } else {

            if (params.sort && !problemService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }
            def problems = problemService.findAllByUser(user, problemService.normalize(params), status)

            response.setHeader("total", problems.totalCount as String)

            respond problems
        }
    }

    @Secured('permitAll')
    def getProblemSubmissions(Long userId, Long problemId, Long submissionId) {

        def user = User.load(userId)
        def problem = Problem.load(problemId)

        if (submissionId) {
            respond submissionService.findByUserAndProblem(Submission.load(submissionId), problem, user)
        } else {

            if (params.sort && !submissionService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def submissions = submissionService.findAllByUserAndProblem(problem, user, submissionService.normalize(params))

            submissions.each { Submission submission ->
                submission.metaClass.showTestCaseEvaluations = false
            }

            response.setHeader("total", submissions.totalCount as String)

            respond submissions
        }
    }

    @Secured('permitAll')
    def getSubmissions(Long userId, Long submissionId) {

        def user = User.load(userId)

        if (submissionId) {
            respond submissionService.findByUser(Submission.load(submissionId), user)
        } else {

            if (params.sort && !submissionService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def submissions = submissionService.findAllByUser(user, submissionService.normalize(params))

            response.setHeader("total", submissions.totalCount as String)

            respond submissions
        }
    }

    @Secured('permitAll')
    def getQuestionnaires(Long userId) {

        def user = User.load(userId)

        if (params.sort && !questionnaireService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        def parameters = questionnaireService.normalize(params)
        if (parameters.filter && parameters.filter.contains("OWN")) {

            parameters.groups = UserGroup.createCriteria().list {
                fetchMode("group", FetchMode.JOIN)
                eq('user', user)
                eq('role', UserGroup.Role.TEACHER)
            }.group

            def questionnaires = questionnaireService.list(parameters)

            response.setHeader("total", questionnaires.totalCount as String)

            respond questionnaires
        } else {
            parameters.groups = UserGroup.findAllByUser(user).group
            parameters.withScore = true
            parameters.userId = userId

            def questionnaires = questionnaireService.list(parameters)

            response.setHeader("total", questionnaires.totalCount as String)

            respond questionnaires
        }

    }

    @Secured('permitAll')
    def getQuestionnaireProblemSubmissions(Long userId, Long questionnaireId, Long problemId, Long submissionId) {

        def user = User.load(userId)
        def questionnaire = Questionnaire.load(questionnaireId)
        def problem = Problem.load(problemId)

        if (submissionId) {
            respond submissionService.findByUserAndQuestionnaireAndProblem(
                    Submission.load(submissionId),
                    user,
                    questionnaire,
                    problem)
        } else {

            if (params.sort && !submissionService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }
            def submissions = submissionService.findAllByUserAndQuestionnaireAndProblem(
                    user,
                    questionnaire,
                    problem,
                    submissionService.normalize(params))

            response.setHeader("total", submissions.totalCount as String)

            respond submissions
        }
    }

    @Secured('permitAll')
    def save() {
        User user = deserialize(request.JSON, false, null)

        def exists = User.createCriteria().list {
            or {
                ilike('username', user.username)
                ilike('email', user.email)
            }
        }.size() > 0

        if (exists) {
            def errors = []

            errors.add("Username or e-mail already exists.")

            def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, errors)

            respond errorResponse, [status: errorResponse.httpStatus]
            return
        }

        if (user?.hasErrors()) {
            invalidUser(user)
        } else {
            def result = userService.save(user)

            new NotificationPreferences([
                    user: user, notificationType: NotificationPreferences.Type.NEW_MESSAGE,
                    digested: false, web: true, email: true ]).merge()

            new NotificationPreferences([
                    user: user, notificationType: NotificationPreferences.Type.QUESTIONNAIRE_CREATED,
                    digested: false, web: true, email: true ]).merge()

            respond result
        }
    }

    @Secured('permitAll')
    def update(Long id) {

        def currentUser = springSecurityService.currentUser as User;

        User user = deserialize(request.JSON, true, id)

        def exists = User.createCriteria().list {
            or {
                ilike('username', user.username)
                ilike('email', user.email)
            }
        }

        exists.each {
            if (it.id != currentUser.id) {
                render status: HttpStatus.BAD_REQUEST
                return
            }
        }

        if (user.id == currentUser.id) {
            if (user?.hasErrors()) {
                invalidUser(user)
            } else {
                respond userService.save(user)
            }
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured('permitAll')
    def recoveryPassword() {
        def query = params.k ? params.k as String : request.JSON["k"] as String

        if (query) {
            User user = User.findByEmailIlikeOrUsernameIlike(query, query)
            if (user) {
                def key = (userService.generateRecoveryKey(user) as PendencyKey).hashKey

                def url = grailsApplication.config.huxley.baseURL

                try {
                    mailService.sendMail {
                        from "support@thehuxley.com"
                        to user.email
                        subject messageSource.getMessage("email.recoveryPassword.subject", [

                        ].toArray(), LCH.getLocale())
                        body messageSource.getMessage("email.recoveryPassword.body", [
                                user.name,
                                url,
                                key
                        ].toArray(), LCH.getLocale())
                    }

                    render status: HttpStatus.NO_CONTENT
                } catch (Exception e) {
                    render status: HttpStatus.SERVICE_UNAVAILABLE
                }

            } else {
                render status: HttpStatus.NOT_FOUND
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured('permitAll')
    def updatePassword(String key) {

        if (key) {
            def pendencyKey = PendencyKey.findByHashKey(key)
            User user

            if (pendencyKey && pendencyKey.type == PendencyKey.Type.CHANGE_PASSWORD) {
                user = User.get(pendencyKey.entity)

                if (user) {
                    def newPassword = request.JSON["newPassword"] as String

                    if (newPassword.size() < 6 || newPassword.size() > 255) {
                        forward(controller: "Error", action: "passwordInvalid")
                    } else {
                        user.password = newPassword

                        if (user.validate()) {
                            if (pendencyKey) {
                                pendencyKey.delete()
                            }
                            respond userService.save(user)
                        } else {
                            invalidUser(user)
                        }
                    }
                } else {
                    render status: HttpStatus.NOT_FOUND
                }
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured('permitAll')
    def getAvatar(Long userId) {
        User user = User.get(userId)

        if (user) {
            getAvatarByKey(user.avatar)
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured('permitAll')
    def getAvatarByKey(String key) {

        def width = params["width"] as Integer ?: 0
        def height = params["height"] as Integer ?: 0

        File file = userService.getAvatar(key, width, height)

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

    @Secured('ROLE_ADMIN')
    def anonymizer() {
        if (Environment.current == Environment.DEVELOPMENT) {
            User.list().each { User user ->
                if (user.id != 1L) {
                    def key = groupService.generateAccessKey(user.id).toLowerCase()

                    user.username = key
                    user.email = "contato+${key}@thehuxley.com"
                    user.name = "User $key"
                    user.password = key
                    user.avatar = "default.png"
                    user.save()
                }
            }

            render status: HttpStatus.NO_CONTENT
        } else {
            render status: HttpStatus.METHOD_NOT_ALLOWED
        }
    }

    @Secured('permitAll')
    def validate() {
        User user = springSecurityService.currentUser as User

        if (user) {
            user = deserialize(request.JSON, true, user.id)
        } else {
            user = deserialize(request.JSON, false, null)
        }

        user.discard()

        if (user.hasErrors()) {
            invalidUser(user)
        } else {
            render status: HttpStatus.ACCEPTED
        }
    }


    User deserialize(json, update, userId) {
        def user = update ? User.get(userId as Long) : new User()

        user.username = json["username"] as String ?: user.username
        user.email = json["email"] as String ?: user.email
        user.name = json["name"] as String ?: user.name
        user.locale = json["locale"] as String ?: user.locale

        if (!user.locale) {
            user.locale = LocaleContextHolder.getLocale().toString()
        }

        if (!update) {
            user.password = json["password"] as String ?: user.password
        }

        if (json["institution"] && update) {
            def institution = Institution.load(request.JSON["institution"]["id"] as Long)
            if (institution && UserInstitution.findByUserAndInstitution(user, institution)) {
                user.institution = institution
            }
        }

        user.validate()

        return user
    }

    def invalidUser(User user) {

        def errors = []

        user.errors.each {
            it.getAllErrors().each {
                if (it.arguments[0] == "email") {
                    if (it.code == "email.invalid") {
                        errors.add(ErrorReason.USER_EMAIL_IS_NOT_VALID.setParams(it.arguments[2]))
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.USER_EMAIL_CANNOT_BE_BLANK)
                    }

                    if (it.code == "unique" || it.code == "validator.invalid") {
                        errors.add(ErrorReason.USER_EMAIL_MUST_BE_UNIQUE.setParams(it.arguments[2]))
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.USER_EMAIL_CANNOT_BE_NULL)
                    }
                } else if (it.arguments[0] == "username") {
                    if (it.code == "unique" || it.code == "validator.invalid") {
                        errors.add(ErrorReason.USER_USERNAME_MUST_BE_UNIQUE.setParams(it.arguments[2]))
                    }

                    if (it.code == "matches.invalid") {
                        errors.add(ErrorReason.USER_USERNAME_NOT_MATCH.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.USER_USERNAME_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.USER_USERNAME_TOO_SMALL.setParams(it.arguments[2]))
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.USER_USERNAME_CANNOT_BE_BLANK)
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.USER_USERNAME_CANNOT_BE_NULL)
                    }
                } else if (it.arguments[0] == "password") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.USER_PASSWORD_CANNOT_BE_NULL)
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.USER_PASSWORD_CANNOT_BE_BLANK)
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.USER_PASSWORD_INVALID)
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.USER_PASSWORD_INVALID)
                    }
                } else if (it.arguments[0] == "name") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.USER_NAME_CANNOT_BE_NULL)
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.USER_NAME_CANNOT_BE_BLANK)
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
