package com.thehuxley

import com.thehuxley.expcetion.DoNotLogException
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.sf.jmimemagic.Magic
import org.apache.commons.lang.LocaleUtils
import org.apache.tomcat.util.http.fileupload.FileUploadBase
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import java.nio.charset.StandardCharsets

class CurrentUserController {

    static responseFormats = ['json']
    static allowedMethods = []

    def springSecurityService
    def userService
    def groupService
    def submissionService
    def messageService
    def mailService
    def feedService
    def notificationService
    def MessageSource messageSource

    def avatarUrl = "${grailsApplication.config.huxley.avatarURL}"

    @Secured("isAuthenticated()")
    def getCurrentUser() {
        def user = springSecurityService.currentUser as User

        if (!user) {
            render HttpStatus.FORBIDDEN
            return
        }
        JSON.use('private', {
            render user as JSON
        })
    }

    @Secured("isAuthenticated()")
    def getUserData() {
        forward(controller: "User", action: "getUserData")
    }

    @Secured("isAuthenticated()")
    def getInstitutions(Long institutionId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getInstitutions", params: [userId: user.id, institutionId: institutionId]
    }

    @Secured("isAuthenticated()")
    def getGroups(Long groupId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getGroups", params: [userId: user.id, groupId: groupId]
    }

    @Secured("isAuthenticated()")
    def getProblemData(Long problemId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getProblemData", params: [userId: user.id, problemId: problemId]
    }

    @Secured("isAuthenticated()")
    def getProblems(Long problemId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getProblems", params: [userId: user.id, problemId: problemId]
    }

    @Secured("isAuthenticated()")
    def getProblemSubmissions(Long problemId, Long submissionId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getProblemSubmissions", params: [userId: user.id, problemId: problemId, submissionId: submissionId]
    }

    @Secured("isAuthenticated()")
    def getSubmissions(Long submissionId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getSubmissions", params: [userId: user.id, submissionId: submissionId]
    }

    @Secured("isAuthenticated()")
    def getQuestionnaires(Long questionnaireId) {
        def user = springSecurityService.currentUser as User

        if (!user) {
            render status: HttpStatus.UNAUTHORIZED
            return
        }

        forward controller: "User", action: "getQuestionnaires", params: [userId: user.id, questionnaireId: questionnaireId]
    }

    @Secured("isAuthenticated()")
    def getUserScores() {
        def user = springSecurityService.currentUser as User
        forward controller: "Questionnaire", action: "getUserScores", params: [userId: user.id, params: params]
    }

    @Secured("isAuthenticated()")
    def getQuestionnaireProblems(Long questionnaireId, Long problemId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getQuestionnaireProblems", params: [userId: user.id, questionnaireId: questionnaireId, problemId: problemId]
    }

    @Secured("isAuthenticated()")
    def getQuestionnaireProblemSubmissions(Long questionnaireId, Long problemId, Long submissionId) {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getQuestionnaireProblemSubmissions", [params: user.id, questionnaireId: questionnaireId, problemId: problemId, submissionId: submissionId]
    }

    @Secured("isAuthenticated()")
    def getProblemSuggestion() {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getProblemSuggestion", params: [userId: user.id]
    }

    @Secured("isAuthenticated()")
    def createSubmission(Long problemId) {
        def user = springSecurityService.currentUser as User

        Problem problem = Problem.get(problemId)
        boolean isAlgorithm = problem.problemType == Problem.ProblemType.ALGORITHM
        def submission

        if (isAlgorithm) {
            def originalFilename = 'HuxleyCode'

            def languageId
            def inputStream

            if (request instanceof MultipartHttpServletRequest) {
                languageId = params.long("language")

                def file = (MultipartFile) request.getFile('file')
                def mimes = null
                try {
                    def mime = Magic.getMagicMatch(file.getBytes())

                    if (mime) {
                        def mimeType = mime.getMimeType()
                        mimes = mimeType.split('/')
                    }
                } catch (Exception e) {
                    log.warn("Cannot detect mime type. File: ${file.originalFilename}")
                }

                if(mimes != null && !mimes[0].equals('text')) {
                    render status: HttpStatus.FORBIDDEN
                    return
                }
                if(file.getSize() > 100000) {
                    render status: HttpStatus.FORBIDDEN
                    return
                }

                originalFilename = file.originalFilename

                inputStream = file.getInputStream()
            } else {
                def json = request.JSON

                languageId = json["language"]["id"] as Long
                def sourceCode = json["code"] as String

                inputStream = new ByteArrayInputStream(sourceCode.getBytes(StandardCharsets.UTF_8))

            }

            Language language = Language.get(languageId)

            if (language == null) {
                render status: HttpStatus.BAD_REQUEST
                return
            }

            if (!originalFilename.endsWith(language.extension)) {
                originalFilename += language.extension.contains(".") ? language.extension : ".$language.extension"
            }

            submission = submissionService.createSubmission(
                    user,
                    problem,
                    language,
                    originalFilename,
                    inputStream
            )
        } else if (problem.problemType != Problem.ProblemType.FILL_THE_CODE) {

            def json = request.JSON
            def choices = json["choices"] as Long[]
            submission = submissionService.createChoiceSubmission(user, problem, choices)

        } else if (problem.problemType == Problem.ProblemType.FILL_THE_CODE) {
            def json = request.JSON

            def parts = json["codeParts"]
            def codeParts = []

            parts.each {
                def part = new CodePartSubmission([lineNumber: it["lineNumber"] as int, problemId: problem.id, code: it["code"]])
                codeParts.add(part)
            }

            def code = problem.baseCode

            def reader = new BufferedReader(new StringReader(code))
            def strLine
            int count = 1

            String newCode = ""

            while ((strLine = reader.readLine()) != null) {

                def found = false

                codeParts.each {
                    if (count == it.lineNumber) {
                        newCode += it.code
                        found = true
                    }
                }

                if (!found) {
                    newCode += strLine
                }

                newCode += '\n'

                count++
            }

            reader.close()

            def inputStream = new ByteArrayInputStream(newCode.getBytes(StandardCharsets.UTF_8))

            Language language = problem.baseLanguage

            def originalFilename = 'HuxleyBuiltCode' + (language.extension.contains(".") ? language.extension : ".$language.extension")

            submission = submissionService.createSubmission(
                    user,
                    problem,
                    language,
                    originalFilename,
                    inputStream)

            if (submission) {
                codeParts.each {
                    submission.addToParts(it)
                }
                submission.save()
            }
        }


        respond submission
    }

    @Secured("isAuthenticated()")
    def uploadAvatar() {

        MultipartFile file = (request as MultipartHttpServletRequest)?.getFile('file')

        if (file) {

            def kb = 1024
            def MIN_SIZE = 1 * kb
            def MAX_SIZE = 5 * (kb * kb)
            def ALLOWED_MIME_TYPE = ["image/jpg", "image/jpeg", "image/png"]

            def fileSize = file.size

            if (ALLOWED_MIME_TYPE.contains(file.contentType)) {
                if ((fileSize >= MIN_SIZE) && (fileSize <= MAX_SIZE)) {
                    def savedFile = userService.uploadAvatar(file)
                    respond(
                            ([
                                    _links: [
                                            self: "$avatarUrl/$savedFile.name"
                                    ],
                                    name  : savedFile.name
                            ])
                    )
                } else {
                    forward(controller: "Error", action: "invalidAvatarSize")
                }
            } else {
                forward(controller: "Error", action: "invalidAvatarMimeType")
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("isAuthenticated()")
    def cropAvatar() {

        def json = request.JSON

        if (json["filename"]) {
            User user = springSecurityService.currentUser as User
            respond userService.crop(
                    user,
                    json["filename"] as String,
                    json["x"] as Integer ?: 0,
                    json["y"] as Integer ?: 0,
                    json["width"] as Integer ?: 400,
                    json["height"] as Integer ?: 300
            )
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("isAuthenticated()")
    def getAvatar() {
        def user = springSecurityService.currentUser as User
        forward controller: "User", action: "getAvatar", params: [userId: user.id]
    }

    @Secured("isAuthenticated()")
    def update() {
        def user = springSecurityService.currentUser as User

        if (user) {
            params.id = user.id
            forward controller: "User", action: "update", params: params
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("isAuthenticated()")
    def updatePassword() {

        def currentUser = springSecurityService.currentUser as User
        User user = User.get(currentUser.id)
        def json = request.JSON

        def password = springSecurityService?.passwordEncoder ?
                springSecurityService.encodePassword(json["password"] as String) : json["password"] as String


        if (user.password == password) {
            def newPassword = json["newPassword"] as String

            if (newPassword.size() < 6 || newPassword.size() > 255) {
                forward(controller: "Error", action: "passwordInvalid")
            } else {

                user.password = newPassword

                if (user.validate()) {
                    respond userService.save(user)
                } else {
                    render status: HttpStatus.BAD_REQUEST
                }
            }
        } else {
            forward(controller: "Error", action: "passwordWrong")
        }
    }

    @Secured("isAuthenticated()")
    def getMessageCount() {
        def currentUser = springSecurityService.currentUser as User

        def countUnread = currentUser ? messageService.countUnread(currentUser) : 0

        respond(["unreadCount": countUnread])
    }

    @Secured("isAuthenticated()")
    def archiveOldMessages() {
        def currentUser = springSecurityService.currentUser as User

        int days = params.days ? params.days as Integer : 7
        def archiveCount = messageService.archiveOldMessages(currentUser, days)

        respond(["count": archiveCount])
    }

    @Secured("isAuthenticated()")
    def getMessages() {
        def currentUser = springSecurityService.currentUser as User

        def result = [:]

        if (params.messageGroup) {

            def messageGroup = MessageGroup.get(params.messageGroup as Long)
            if (!hasMessageGroupPermission(currentUser, messageGroup)) {
                render status: HttpStatus.FORBIDDEN
                return
            }

            result.messageGroup = messageGroup
            result.messages = Message.findAllByMessageGroupId(messageGroup.id, [sort: "dateCreated", order: "asc"])

            // salva data da visualização da conversa
            saveMessageGroupViewEvent(currentUser.id, messageGroup.id)

        } else {
            result.messages = messageService.findAllGrouped(currentUser, params)
        }

        if (params.fetch && !result.messages.isEmpty()) {
            if (params.fetch.contains("user")) {
                Set ids = result.messages.stream().map({ return it.senderId }).collect()
                Set ids2 = result.messageGroup ?
                        [result.messageGroup.userId, result.messageGroup.recipientId] : result.messages.stream().map({ return it.user_id }).collect()
                Set ids3 = result.messageGroup ? [] : result.messages.stream().map({ return it.recipient_id }).collect()
                ids.addAll(ids2)
                ids.addAll(ids3)

                result.users = User.findAllByIdInList(ids.asList()).stream().map({
                    return [id: it.id, name: it.name, avatar: "${avatarUrl}/thumbs/" + it.avatar]
                }).collect()
            }

            if (params.fetch.contains("problem")) {
                Set ids = result.messageGroup ?
                        [result.messageGroup.problemId] : result.messages.stream().map({ return it.problem_id }).collect()

                result.problems = Problem.findAllByIdInList(ids.asList()).stream().map({
                    return [id: it.id, name: it.name()]
                }).collect()
            }

            if (params.fetch.contains("group")) {
                Set ids = result.messageGroup ?
                        [result.messageGroup.groupId] : result.messages.stream().map({ return it.group_id }).collect()

                result.groups = Group.findAllByIdInList(ids.asList()).stream().map({
                    return [id: it.id, name: it.name, url: it.url]
                }).collect()
            }
        }

        respond result

    }

    @Secured("isAuthenticated()")
    def messageStats() {
        def currentUser = springSecurityService.currentUser as User
        respond messageService.messageStats(currentUser)
    }

    @Secured("isAuthenticated()")
    def sendMessage() {
        def currentUser = springSecurityService.currentUser as User
        def message = new Message()

        Long groupId = request.JSON["messageGroup"] as Long

        message.body = request.JSON["body"] as String
        message.senderId = currentUser.id

        if (!message.body) {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        def messageGroup

        if (groupId == null) {

            messageGroup = new MessageGroup([
                subject: request.JSON["subject"] as String,
                groupId: request.JSON["groupId"] as Long,
                problemId: request.JSON["problemId"] as Long,
                recipientId: request.JSON["recipientId"] as Long,
                locale: LocaleContextHolder.getLocale().toString(),
                userId: currentUser.id
            ])

            if (messageGroup.groupId && messageGroup.problemId) {
                messageGroup.type = MessageGroup.Type.PROBLEM_QUESTION
            } else if (messageGroup.recipientId) {
                messageGroup.type = MessageGroup.Type.DIRECT_MESSAGE
            } else if (request.JSON["type"]) {
                messageGroup.type = MessageGroup.Type.valueOf(request.JSON["type"])
            } else {
                render status: HttpStatus.BAD_REQUEST
                return
            }

            messageGroup.save()
            groupId = messageGroup.id

        } else {
            messageGroup = MessageGroup.get(groupId)
            if (!hasMessageGroupPermission(currentUser, messageGroup)) {
                render status: HttpStatus.FORBIDDEN
                return
            }
        }

        message.messageGroupId = groupId


        def result = messageService.save(message)
        saveMessageGroupViewEvent(currentUser.id, groupId)

        Problem problem = messageGroup.problemId ? Problem.get(messageGroup.problemId) : null

        def notificationType = messageSource.getMessage("notification.email.message.type." + messageGroup.type.toString(), null, LocaleUtils.toLocale(currentUser.locale))
        def notificationStatus = messageSource.getMessage("notification.email.message.status." + messageGroup.messageStatus.toString(), null, LocaleUtils.toLocale(currentUser.locale))

        def notificationParams = [ notificationType, currentUser.id, currentUser.name, message.dateCreated, message.body, groupId, notificationStatus ]

        List<Long> recipients = new ArrayList<>()

        if (messageGroup.groupId) {
            Group group = Group.get(messageGroup.groupId)
            def users = UserGroup.executeQuery("SELECT user.id FROM UserGroup WHERE group = :group AND role != :role",
                        ["group": group, "role": UserGroup.Role.STUDENT])
            messageService.cleanCountCache(users)
            notificationService.notify(currentUser, group, false, NotificationPreferences.Type.NEW_MESSAGE, notificationParams)
        } else if (messageGroup.recipientId && messageGroup.recipientId != currentUser.id) {
            recipients.add(messageGroup.recipientId)
        } else if (problem) {
            recipients.add(problem.userSuggest.id)
        }

        if (currentUser.id != messageGroup.userId) {
            recipients.add(messageGroup.userId)
        }

        if (!recipients.isEmpty()) {
            notificationService.notify(currentUser, recipients, NotificationPreferences.Type.NEW_MESSAGE, notificationParams)
        }

        messageService.cleanCountCache(recipients)

        if (messageGroup.messageStatus == MessageGroup.Status.ARCHIVED) {
            messageGroup.messageStatus = MessageGroup.Status.UNRESOLVED
            messageGroup.save()
        }

        respond result

    }

    @Secured("isAuthenticated()")
    def changeMessageStatus() {
        def currentUser = springSecurityService.currentUser as User

        if (!request.JSON["id"]) {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        long id = request.JSON["id"] as Long

        def status = MessageGroup.Status.valueOf(request.JSON["status"] as String)

        def messageGroup = MessageGroup.get(id)
        if (!hasMessageGroupPermission(currentUser, messageGroup)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        messageGroup.messageStatus = status
        messageGroup.merge()

        respond messageGroup
    }

    private void saveMessageGroupViewEvent(long userId, long messageGroupId) {
        messageService.saveMessageGroupViewEvent(userId, messageGroupId)
    }

    @Secured("isAuthenticated()")
    def sendContactEmail() {
        def currentUser = springSecurityService.currentUser as User

        if (request.JSON["subject"] && request.JSON["body"]) {
            if (currentUser) {

                def institutions = UserInstitution.findAllByUser(currentUser).institution.name
                def groups = UserGroup.findAllByUser(currentUser).group.name

                def header = """
					name: $currentUser.name
					username: $currentUser.username
					email: $currentUser.email
					institutions: ${institutions.join(", ")}
					groups: ${groups.join(", ")}
					roles: ${currentUser.authorities.authority.join(", ")}
				""".replaceAll("\t", "").trim()

                try {
                    mailService.sendMail {
                        to "support@thehuxley.com"
                        from currentUser.email
                        subject request.JSON["subject"]
                        body header + "\n\n" + request.JSON["body"]
                    }

                    render status: HttpStatus.NO_CONTENT
                } catch (Exception e) {
                    e.printStackTrace()
                    render status: HttpStatus.SERVICE_UNAVAILABLE
                }

            } else {
                render status: HttpStatus.NOT_FOUND
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }

    }

    @Secured("isAuthenticated()")
    def getUserFeed() {
        def currentUser = springSecurityService.currentUser as User ?: null

        render(contentType: "application/json", text: (feedService.findAllByUser(currentUser, params) as JSON) as String)
    }

    private boolean hasMessageGroupPermission(User user, MessageGroup group) {

        boolean isAdmin = user.authorities.authority.contains("ROLE_ADMIN")

        def fromUser = (group.userId && group.userId == user.id) || (group.recipientId && group.recipientId == user.id)

        if (!isAdmin && group.groupId
                && !groupService.isMember(group.groupId as Long, user.id)) {
            return false
        }

        if (!group.groupId && group.problemId && !fromUser) {
            if (user.id != Problem.get(group.problemId).userSuggestId) {
                return false
            }
        }

        return true
    }

    @Secured("isAuthenticated()")
    def saveNotificationPreferences() {
        def currentUser = springSecurityService.currentUser as User ?: null

        def type = NotificationPreferences.Type.valueOf(request.JSON['type'])

        def preferences = NotificationPreferences.findByUserAndNotificationType(currentUser, type)

        if (!preferences) {
            preferences = new NotificationPreferences([ user: currentUser, notificationType: type ])
        }

        preferences.digested = asBoolean(request, 'digested', preferences.digested)
        preferences.web = asBoolean(request, 'web', preferences.digested)
        preferences.email = asBoolean(request, 'email', preferences.digested)

        respond preferences.merge()
    }

    private boolean asBoolean(request, property, defaultValue) {
        return request.JSON[property] != null ? request.JSON[property] as boolean : defaultValue
    }

    @Secured("isAuthenticated()")
    def listNotificationPreferences() {
        def currentUser = springSecurityService.currentUser as User ?: null
        respond NotificationPreferences.findAllByUser(currentUser)
    }

    @Secured("isAuthenticated()")
    def listFailingStudents() {
        def currentUser = springSecurityService.currentUser as User ?: null

        def users = userService.failingStudents(currentUser, params)

        respond users
    }

    @Secured("isAuthenticated()")
    def changeLanguage() {
        def currentUser = springSecurityService.currentUser as User
        def json = request.JSON

        User user = User.get(currentUser.id)
        user.locale = json["locale"]

        respond userService.save(user)
    }

    def handleDoNotLogException(DoNotLogException e) {
        render status: HttpStatus.FORBIDDEN
    }

    def handleSizeLimitExceededException(FileUploadBase.SizeLimitExceededException e) {
        render status: HttpStatus.FORBIDDEN
    }

}
