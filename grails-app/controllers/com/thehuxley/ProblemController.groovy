package com.thehuxley

import com.google.common.base.Strings
import com.google.common.cache.CacheBuilder
import com.thehuxley.error.ErrorReason
import com.thehuxley.error.ErrorResponse
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import org.grails.web.json.JSONObject
import grails.web.mapping.LinkGenerator
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.commons.CommonsMultipartFile

import org.springframework.security.crypto.codec.Hex
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest

import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

import java.text.MessageFormat

class ProblemController {

    static final def testCaseDeleteRequest = new CacheBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build()

    static long MAX_TEST_CASE_SIZE = 20 * 1024 * 1024;

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET"]
    
    def problemService
    def submissionService
    def testCaseService
    def oracleService
    def springSecurityService
    def userTipVoteService
    def codeRunnerService
    def notificationService
    LinkGenerator grailsLinkGenerator

    @Secured("permitAll")
    def index() {

        if (params.sort && !problemService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        User user = springSecurityService.currentUser as User
        Problem.Status status = Problem.Status.ACCEPTED

        def atLeastTeacher = false

        if (user) {
            params.user = user.id
            def authorities = user.getAuthorities().authority
            atLeastTeacher = authorities.contains("ROLE_TEACHER_ASSISTANT") ||
                    authorities.contains("ROLE_TEACHER") ||
                    authorities.contains("ROLE_ADMIN_INST") ||
                    authorities.contains("ROLE_ADMIN")

            if (atLeastTeacher) {
                status = params.status ? Problem.Status.valueOf(params.status as String) : null
            }

        }

        if (!atLeastTeacher) {
            params.quizOnly = false
        }

        def problems = problemService.list(problemService.normalize(params), status)

        if (user) {
            problemService.bindEvaluations(user, problems)
        }

        response.setHeader("total", problems.totalCount as String)


        JSON.use('lowDetail', { respond problems })

    }

    @Secured("permitAll")
    def show(Long id) {
        User user = springSecurityService.currentUser as User
        Problem.Status status = Problem.Status.ACCEPTED

        def atLeastTeacher = false

        if (user) {
            def authorities = user.getAuthorities().authority

            atLeastTeacher = authorities.contains("ROLE_TEACHER_ASSISTANT") ||
                    authorities.contains("ROLE_TEACHER") ||
                    authorities.contains("ROLE_ADMIN_INST") ||
                    authorities.contains("ROLE_ADMIN")

        }

        def problem = problemService.get(id, atLeastTeacher ? null : status)

        if (problem) {
            if (user) {
                problemService.bindEvaluations(user, [problem])
            }

            if (atLeastTeacher) {
                JSON.use('atLeastTeacher', { respond problem })
                return
            } else {

                // se for quizOnly verificar se existe, em algum grupo do usuário,
                // algum questionario ja iniciado que contém esse problema
                if (problem.quizOnly && (!user || problemService.countOpenUserQuizzesWithProblem(id, user.id, new Date()) == 0)) {
                    render status: HttpStatus.FORBIDDEN
                    return
                }

            }
        }

        respond problem
    }

    @Secured("permitAll")
    def getData(Long problemId) {
        respond problemService.getData(Problem.load(problemId), (String) params.key)
    }

    @Secured("permitAll")
    def getStats() {
        if (params.test_cases) {
            respond testCaseService.testCaseNeedingTips()
        } else {
            render status: HttpStatus.NOT_FOUND
        }

    }

    @Secured("permitAll")
    def getOracleConsultSize(String hash) {
        def outputSize = oracleService.getResultSize(hash)

        if (outputSize) {
            render(contentType: "application/json", text: ([size: outputSize] as JSON) as String)
        } else {
            render(contentType: "application/json", text: ([status: "PENDING"] as JSON) as String)
        }
    }

    @Secured("permitAll")
    def getOracleConsult(String hash) {
        def output = oracleService.getResult(hash)

        if (output) {

            if (request.getHeader("Accept").equals("text/plain")) {
                response.setHeader "Content-disposition", "attachment; filename=${hash}.out"
                render(contentType: "text/plain", output)
            } else {
                render(contentType: "application/json", text: ([output: output] as JSON) as String)
            }

        } else {
            render(contentType: "application/json", text: ([status: "PENDING"] as JSON) as String)
        }

    }

    @Secured("permitAll")
    def runCode(Long problemId) {
        def user = springSecurityService.currentUser as User

        Problem problem = Problem.get(problemId)
        boolean isAlgorithm = problem.problemType == Problem.ProblemType.ALGORITHM

        if (isAlgorithm) {
            def json = request.JSON

            def languageId = json["language"] as Long
            def sourceCode = json["code"] as String
            def input = json["input"] as String

            Language language = Language.get(languageId)

            def filename = 'HuxleyCode'
            filename += language.extension.contains(".") ? language.extension : ".$language.extension"

            def hash = codeRunnerService.runCode(input, sourceCode, filename, language, problem)

            render(contentType: "application/json", text: ([hash: hash] as JSON) as String)
        } else {
            render status: HttpStatus.BAD_REQUEST
            return
        }
    }

    @Secured("permitAll")
    def getCodeResult(String hash) {
        if (hash) {
            def output = codeRunnerService.getResult(hash)

            if (output) {

                if (request.getHeader("Accept").equals("text/plain")) {
                    response.setHeader "Content-disposition", "attachment; filename=${hash}.out"
                    render(contentType: "text/plain", output)
                } else {
                    render(contentType: "application/json", text: ([output: output] as JSON) as String)
                }

            } else {
                render(contentType: "application/json", text: ([status: "PENDING"] as JSON) as String)
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("permitAll")
    def sendToOracle(Long problemId) {
        def JSON = request.JSON
        def input = JSON["input"] as String

        if (input) {
            def hash = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest((new Random().nextInt() + new Date().toString()).bytes)))

            oracleService.sendToOracle(hash, input, Problem.load(problemId))


            render(contentType: "application/json", text: ([hash: hash] as JSON) as String)
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def save() {
        def problem = deserialize(request.JSON, false, null)

        if (!checkProblemQuality(problem)) {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        User currentUser = springSecurityService.currentUser as User
        problem.userSuggest = currentUser

        def topics = problem.topics.clone()

        topics.each {
            def topic = Topic.get(it.id)
            problem.addToTopics(topic)
        }

        if (!problem.hasErrors()) {
            respond problemService.save(problem)
        } else {
            invalidProblem(problem)
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def update(Long id) {

        User currentUser = springSecurityService.currentUser as User
        def roles = currentUser.authorities.authority

        def problem = deserialize(request.JSON, true, id)

        if (!checkProblemQuality(problem)) {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        if (!problem.hasErrors()) {
            if (canUserEditProblem(currentUser, problem)) {
                def savedProblem = problemService.save(problem)

                if (problem.problemType != Problem.ProblemType.ALGORITHM && problem.problemType != Problem.ProblemType.FILL_THE_CODE) {
                    submissionService.reevaluateMultipleChoiceProblem(problem)
                }

                def notifications = problemService.getNotifications(problem.id)

                notifications.each {
                    def ids = it.userIds.split(';').collect { userId -> Long.parseLong(userId) }
                    def params = [problem.id, problem.name(), currentUser.id, currentUser.name, it.quizId, it.quizTitle, it.groupId, it.groupName]
                    notificationService.notify(currentUser, ids, NotificationPreferences.Type.PROBLEM_CHANGED, params)
                }

                respond savedProblem
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } else {
            invalidProblem(problem)
        }
    }

    @Secured("permitAll")
    def getSubmissions(Long problemId, Long submissionId) {

        def problem = Problem.load(problemId)

        if (submissionId) {
            respond submissionService.findByProblem(Submission.load(submissionId), problem)
        } else {

            if (params.sort && !submissionService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def submissions = submissionService.findAllByProblem(problem, submissionService.normalize(params))

            response.setHeader("total", submissions.totalCount as String)

            respond submissions
        }
    }

    @Secured("permitAll")
    def getExampleTestCases(Long problemId, Long testCaseId) {
        def problem = Problem.load(problemId)

        if (testCaseId) {
            if (request.getHeader("Accept").equals("application/zip")) {
                response.setHeader "Content-disposition", "attachment; filename=${testCaseId}.zip"
                response.setHeader 'Content-Type', 'application/zip'

                def testCase = TestCase.load(testCaseId)

                ZipOutputStream zos = new ZipOutputStream(response.outputStream)

                if (testCase.input) {
                    ZipEntry input = new ZipEntry("${testCaseId}.in")

                    zos.putNextEntry(input)
                    zos.write(testCase.input.getBytes("UTF-8"))
                    zos.closeEntry()
                }

                if (testCase.output) {
                    ZipEntry output = new ZipEntry("${testCaseId}.out");

                    zos.putNextEntry(output)
                    zos.write(testCase.output.getBytes("UTF-8"))
                    zos.closeEntry()
                }

                if (testCase.tip()) {
                    ZipEntry tip = new ZipEntry("${testCaseId}.tip");

                    zos.putNextEntry(tip)
                    zos.write(testCase.tip().getBytes("UTF-8"))
                    zos.closeEntry()
                }

                zos.close()
            } else {
                respond testCaseService.findByProblem(TestCase.load(testCaseId), problem, true)
            }
        } else {

            if (params.sort && !testCaseService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            respond testCaseService.findAllByProblem(problem, testCaseService.normalize(params), true)
        }
    }

    @Secured("isAuthenticated()")
    def getTestCases(Long problemId, Long testCaseId) {
        def problem = Problem.load(problemId)
        def currentUser = springSecurityService.currentUser as User
        def testCase = TestCase.get(testCaseId)
        def roles = currentUser.getAuthorities().collect { it.authority }

        def canViewTestCase = roles.contains('ROLE_ADMIN') || (testCase && testCase.example) || (problem.userSuggest == currentUser)

        if  (!canViewTestCase) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        if (testCaseId) {
            if (request.getHeader("Accept").equals("application/zip")) {
                response.setHeader "Content-disposition", "attachment; filename=${testCaseId}.zip"
                response.setHeader 'Content-Type', 'application/zip'

                testCase = TestCase.load(testCaseId)
                testCaseService.zip(testCase, response.outputStream)
            } else {
                respond testCaseService.findByProblem(testCase, problem)
            }
        } else {
            if (params.sort && !testCaseService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            respond testCaseService.findAllByProblem(problem, testCaseService.normalize(params))
        }

    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def getTestCaseList(Long problemId) {
        Problem problem = Problem.get(problemId)

        respond testCaseService.getTestCaseList(problem)
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def getInputTestCasePlainText(Long problemId, Long testCaseId) {
        Problem problem = Problem.get(problemId)
        TestCase testCase = TestCase.get(testCaseId)

        String input = testCaseService.findByProblemInputPlainText(testCase, problem)

        render input
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def getOutputTestCasePlainText(Long problemId, Long testCaseId) {
        Problem problem = Problem.get(problemId)
        TestCase testCase = TestCase.get(testCaseId)

        String output = testCaseService.findByProblemOutputPlainText(testCase, problem)

        render output
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def saveTestCase(Long problemId) {
        def problem = Problem.load(problemId)

        if (!canUserEditProblem(springSecurityService.currentUser as User, Problem.get(problemId))) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest mpr = (MultipartHttpServletRequest) request;
            MultipartFile file = (MultipartFile) mpr.getFile("file");

            if (file && !file.empty) {
                def zipInputStream = new ZipInputStream(file.getInputStream())

                def zipEntry;

                def files = [:]
                def testCaseNames = []

                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    StringBuilder fileContent = new StringBuilder();
                    byte[] buffer = new byte[1024];

                    int read = 0;
                    while ((read = zipInputStream.read(buffer, 0, 1024)) >= 0) {
                        fileContent.append(new String(buffer, 0, read));
                    }

                    testCaseNames << zipEntry.getName().replaceAll("\\.in|\\.out|\\.tip", "")
                    files[zipEntry.getName()] = fileContent.toString()
                }

                def testCases = []

                testCaseNames.unique().each { prefix ->
                    def testCase = new TestCase()

                    testCase.problem = problem
                    testCase.input = files["${prefix}.in"]
                    testCase.output = files["${prefix}.out"]
                    testCase.example = false

                    def size = testCase.input.length() + testCase.output.length()
                    if (size > MAX_TEST_CASE_SIZE) {
                        response.status = 400
                        respond ([ message: 'problem.testCase.sizeLimit', size: size, maxSize: MAX_TEST_CASE_SIZE ])
                        return
                    }

                    def locale = LocaleContextHolder.getLocale().toString()

                    def i18n = new TestCaseI18n(locale: locale)

                    i18n.tip = files["${prefix}.tip"]

                    testCase.addToI18ns(i18n)
                    testCaseService.save(testCase)
                    testCases << testCase.id
                }

                respond testCases
            }
        } else {
            def testCase = new TestCase()
            def JSON = request.JSON

            testCase.problem = problem
            testCase.output = JSON["output"] as String
            testCase.input = JSON["input"] as String
            testCase.example = JSON["example"] as Boolean

            def newI18n = { String locale ->
                new TestCaseI18n(locale: locale)
            }

            def locale = LocaleContextHolder.getLocale().toString()

            def i18n = newI18n(locale)

            i18n.tip = JSON["tip"] as String

            testCase.addToI18ns(i18n)

            respond testCaseService.save(testCase)
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def updateTestCase(Long problemId, Long testCaseId) {
        def testCase = TestCase.load(testCaseId)
        def JSON = request.JSON

        testCase.output = JSON["output"] as String ?: testCase.output
        testCase.input = JSON["input"] as String ?: testCase.input
        testCase.example = JSON["example"] != null ? JSON["example"] as Boolean : testCase.example

        def newI18n = { String locale ->
            new TestCaseI18n(locale: locale)
        }

        def locale = LocaleContextHolder.getLocale().toString()

        def i18n = newI18n(locale)

        def found = TestCaseI18n.createCriteria().get {
            eq('locale', locale)
            eq('testCase', testCase)
        }

        if (found) {
            i18n = found
        }

        i18n.tip = JSON["tip"]

        testCase.addToI18ns(i18n)

        if (testCase.problem.id == problemId) {

            if (!canUserEditProblem(springSecurityService.currentUser as User, Problem.get(problemId))) {
                render status: HttpStatus.FORBIDDEN
                return
            }

            respond testCaseService.update(testCase)
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def deleteTestCase(Long problemId, Long testCaseId) {

        log.info("delete-test-case: $testCaseId")

        def hasPendingRequest = testCaseDeleteRequest.getIfPresent(testCaseId)

        if (hasPendingRequest) {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        try {

            testCaseDeleteRequest.put(testCaseId, true)
            TestCase testCase = TestCase.get(testCaseId)

            if (testCase != null) {

                if (!canUserEditProblem(springSecurityService.currentUser as User, Problem.get(testCase.problemId))) {
                    render status: HttpStatus.FORBIDDEN
                    return
                }

                testCaseService.delete(testCaseId)
                notify "submission.evaluateProblemSubmissions", testCase.problemId
            } else {
                log.warn("test-case-do-not-exist: $testCaseId")
            }

        } catch (IllegalArgumentException ex) {
            render status: HttpStatus.BAD_REQUEST
            return
        } finally {
            testCaseDeleteRequest.invalidate(testCaseId)
        }

        render status: HttpStatus.NO_CONTENT
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def validate() {
        def problem = deserialize(request.JSON, false, null)

        if (problem.hasErrors()) {
            invalidProblem(problem)
        } else {
            render status: HttpStatus.ACCEPTED
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def acceptProblem(long problemId) {

        log.info("accept-problem: $problemId")

        def problem = Problem.get(problemId);

        User user = springSecurityService.currentUser as User

        if (problem.userSuggest.id != user.id && !user.authorities.contains('ROLE_ADMIN') && !user.authorities.contains('ROLE_ADMIN_INST')) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        problem.status = Problem.Status.ACCEPTED;
        problem.save()
    }

    @Secured("hasAnyRole('ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN', 'ROLE_TRANSLATOR')")
    def translate(long problemId) {

        def problem = Problem.load(problemId)

        if (problem.problemType != Problem.ProblemType.ALGORITHM) {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        def json = request.JSON

        bindDescriptions(json, true, problem, (springSecurityService.currentUser as User));

        problemService.save(problem)

        respond problem

    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def updateTestCaseFiles() {
        testCaseService.rewriteTestCaseFiles()
        render status: HttpStatus.OK
    }

    @Secured("isAuthenticated()")
    def getErrorHelp() {
        def http = new HTTPBuilder('http://localhost:8087')
        def postBody = [errorMsg:  request.JSON.errorMessage ]

        http.post( path: '/', body: postBody,
                requestContentType: ContentType.URLENC ) { resp, reader ->
            def status = resp.statusLine.statusCode

            if (status == 200) {
                def result = [ message: reader.text() ]
                respond result
            } else {
                render status: HttpStatus.BAD_REQUEST
            }

        }

    }

    Problem deserialize(json, update, problemId) {
        User user = springSecurityService.currentUser as User
        def problem = update ? Problem.get(problemId as Long) : new Problem()

        problem.source =        json["source"] as String        ?: problem.source
        problem.level =         json["level"] as Integer        ?: problem.level
        problem.timeLimit =     json["timeLimit"] as Integer    ?: problem.timeLimit
        problem.status =        json["status"]                  ? Problem.Status.valueOf(json["status"] as String) : problem.status
        problem.problemType =   json["problemType"]             ? Problem.ProblemType.valueOf(json["problemType"] as String) : problem.problemType
        problem.quizOnly =      json["quizOnly"] != null        ? json["quizOnly"] as boolean : problem.quizOnly
        problem.nd = null

        if (!problem.problemType) {
            problem.problemType = Problem.ProblemType.ALGORITHM
        }

        bindDescriptions(json, update, problem, user);

        if (problem.getPersistentValue("status") && (problem.getPersistentValue("status") != problem.status)) {
            problem.userApproved = user
        }

        def topics = json["topics"] ? json["topics"] as List<Topic> : null
        if (topics && !topics.empty) {
            problem.setTopics(new HashSet<Topic>())

            json["topics"].each {
                problem.addToTopics(Topic.load(it["id"] as Long))
            }
        }

        if (EnumSet.of(
                Problem.ProblemType.MULTIPLE_CHOICE,
                Problem.ProblemType.SINGLE_CHOICE,
                Problem.ProblemType.TRUE_OR_FALSE).contains(problem.problemType)) {

            def locale = LocaleContextHolder.getLocale().toString()
            def choicesJson = json["choices"] ? json["choices"] as List : null


            if (problem.choices) {
                problem.choices.clear()
            } else {
                problem.choices = []
            }
            int i = 1;
            choicesJson.each {

                def choice;

                if (it.id) {
                    choice = ProblemChoice.get(it.id)
                    choice.choiceOrder = i++
                    def choicei18n = choice.i18ns.find { it.locale.equals(locale) }
                    if (choicei18n) {
                        choicei18n.description = it['description']
                    } else {
                        choice.addToI18ns(new ProblemChoiceI18n([
                                description: it['description'],
                                locale: locale]))
                    }
                } else {
                    choice = new ProblemChoice([ correct: it['correct'], choiceOrder: i++ ])
                    choice.addToI18ns(new ProblemChoiceI18n([
                            description: it['description'],
                            locale: locale]))
                }

                problem.addToChoices(choice)
            }
        }

        if (problem.problemType == Problem.ProblemType.FILL_THE_CODE) {
            problem.baseCode = json["baseCode"]
            def blank = json["blankLines"] as List
            Integer[] lines = new int[blank.size()]

            for (int i = 0; i < blank.size(); i++) {
                lines[i] = blank[i] as int
            }

            problem.baseLanguage = Language.get(json["baseLanguage"].id)
            problem.blankLines = lines
        }

        problem.validate()
        return problem
    }

    def bindDescriptions(json, update, problem, currentUser) {
        def locale = json["locale"] as String ?: LocaleContextHolder.getLocale().toString()

        def i18n

        if (update) {
            i18n = problem.i18ns.find { it.locale.equals(locale) }
        }

        if (!i18n) {
            i18n = new ProblemI18n(locale: locale)
            i18n.userSuggest = currentUser.id
        }

        i18n.name = json["name"] as String ?: problem.name()
        i18n.description = json["description"] as String ?: problem.description()
        if (problem.problemType == Problem.ProblemType.ALGORITHM || problem.problemType == Problem.ProblemType.FILL_THE_CODE) {
            i18n.inputFormat = json["inputFormat"] as String ?: problem.inputFormat()
            i18n.outputFormat = json["outputFormat"] as String ?: problem.outputFormat()
        }

        problem.addToI18ns(i18n)
    }

    @Secured("isAuthenticated()")
    def uploadImage() {
        if (params.file) {

            def kb = 1024
            def MIN_SIZE = 1 * kb
            def MAX_SIZE = 5 * (kb * kb)
            def ALLOWED_MIME_TYPE = ["image/jpg", "image/jpeg", "image/png"]

            MultipartFile mpf = (MultipartFile) request.getFile('file')
            def fileSize =  mpf.size;
            def contentType = mpf.contentType;

            if (ALLOWED_MIME_TYPE.contains(contentType)) {
                if ((fileSize >= MIN_SIZE) && (fileSize <= MAX_SIZE)) {
                    def file = problemService.uploadImage(mpf)
                    respond(
                            ([
                                    _links: [
                                            self: grailsLinkGenerator.link(
                                                    controller: "problems",
                                                    action: "image",
                                                    absolute: true
                                            ) + "/" + file.name
                                    ],
                                    name  : file.name
                            ])
                    )
                } else {
                    forward(controller: "Error", action: "invalidProblemImageSize")
                }
            } else {
                forward(controller: "Error", action: "invalidProblemImageMimeType")
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("permitAll")
    def getImageByKey(String key) {

        File file = problemService.getImage(key)

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

    @Secured("permitAll")
    def getLanguages(long problemId) {
        def i18ns = ProblemI18n.findAll("FROM ProblemI18n WHERE problem.id = ?", [ problemId ])
        if (!i18ns) {
            render status: HttpStatus.NOT_FOUND
            return
        }

        respond i18ns*.locale
    }

    def invalidProblem(Problem problem) {

        def errors = []

        problem.errors.each {
            it.getAllErrors().each {
                if (it.arguments[0] == "name") {

                    if (it.code == "unique") {
                        errors.add(ErrorReason.PROBLEM_NAME_MUST_BE_UNIQUE.setParams(it.arguments[2]))
                    }

                    if (it.code == "blank") {
                        errors.add(ErrorReason.PROBLEM_NAME_CANNOT_BE_BLANK)
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.PROBLEM_NAME_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.PROBLEM_NAME_TOO_SMALL.setParams(it.arguments[2]))
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.PROBLEM_NAME_CANNOT_BE_NULL)
                    }

                } else if (it.arguments[0] == "level") {
                    if (it.code == "range") {
                        errors.add(ErrorReason.PROBLEM_LEVEL_OUT_OF_RANGE.setParams(it.arguments[2]))
                    }
                } else {
                    errors.add(ErrorReason.GENERIC.setParams(it.code + " - " + MessageFormat.format(it.defaultMessage, it.arguments)))
                }
            }
        }

        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, errors)

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addTopic(Long problemId, Long topicId) {
        def problem = Problem.get(problemId)
        def topic = Topic.get(topicId)
        problem.addToTopics(topic)

        if (!problem.hasErrors()) {
            problem.save()
        } else {
            invalidProblem(problem)
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def deleteTopic(Long problemId, Long topicId) {
        def problem = Problem.get(problemId)

        if (problem.status != Problem.Status.ACCEPTED || problem.topics.size() > 1) {
            def topic = Topic.get(topicId)
            problem.removeFromTopics(topic)

            if (!problem.hasErrors()) {
                problem.save()
            } else {
                invalidProblem(problem)
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("permitAll")
    def countByStatus() {
        User user = springSecurityService.currentUser as User
        respond problemService.countProblemStatus(user)
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def findBadTips() {
        respond userTipVoteService.findBadTips(springSecurityService.currentUser as User);
    }

    private def checkProblemQuality(Problem problem) {

        if (problem.problemType == Problem.ProblemType.ALGORITHM && !(problem.description() && problem.description().length() > 49)) return false

        if ((problem.problemType == Problem.ProblemType.MULTIPLE_CHOICE
                || problem.problemType == Problem.ProblemType.SINGLE_CHOICE) && (!problem.choices || problem.choices.size() < 2)) {
            return false
        }

        if (problem.problemType == Problem.ProblemType.TRUE_OR_FALSE && (!problem.choices || problem.choices.size() < 1)) {
            return false
        }

        if (problem.problemType == Problem.ProblemType.FILL_THE_CODE &&
                (Strings.isNullOrEmpty(problem.baseCode) || problem.blankLines.length < 1)) {
            return false
        }

        if (problem.problemType == Problem.ProblemType.ALGORITHM && (Strings.isNullOrEmpty(problem.inputFormat())
                || Strings.isNullOrEmpty(problem.outputFormat()))) {
            return false
        }

        return (!Strings.isNullOrEmpty(problem.source)
                && problem.topics && !problem.topics.isEmpty())
    }

    @Secured("isAuthenticated()")
    def tipVote(Long testCaseId) {
        def JSON = request.JSON
        def user = springSecurityService.currentUser as User
        def oldVote = UserTipVote.findByUserIdAndTestCaseId(user.id, testCaseId)
        UserTipVote userTipVote = new UserTipVote()
        if(oldVote) {
            userTipVote = oldVote
            userTipVote.useful = JSON["useful"] as Boolean
        } else {
            userTipVote.useful = JSON["useful"] as Boolean
            userTipVote.userId = user.id
            userTipVote.testCaseId = testCaseId
        }

        if(!userTipVote.hasErrors()) {
            userTipVoteService.save(userTipVote)
            render status: HttpStatus.ACCEPTED
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("isAuthenticated()")
    def getUserVote(Long problemId) {
        def user = springSecurityService.currentUser as User
        def vote = UserProblemVote.findByUserIdAndProblemId(user.id, problemId)

        if (vote != null) {
            respond vote
        } else {
            JSONObject jsonObject = new JSONObject()
            jsonObject.score = -1
            respond jsonObject
        }
    }

    @Secured("isAuthenticated()")
    def problemVote(Long problemId) {
        def JSON = request.JSON
        def user = springSecurityService.currentUser as User
        def oldVote = UserProblemVote.findByUserIdAndProblemId(user.id, problemId)
        UserProblemVote userProblemVote = new UserProblemVote()

        short score = JSON["score"] as Short

        if (score > 5) score = 5
        if (score < 1) score = 1

        if(oldVote) {
            userProblemVote = oldVote
            userProblemVote.score = score
        } else {
            userProblemVote.score = score
            userProblemVote.userId = user.id
            userProblemVote.problemId = problemId
        }

        if(!userProblemVote.hasErrors()) {
            userProblemVote.save(flush: true)
            render status: HttpStatus.ACCEPTED
        } else {
            render status: HttpStatus.BAD_REQUEST
        }
    }

    @Secured("isAuthenticated()")
    def votes(Long testCaseId) {
        JSONObject jsonObject = new JSONObject()
        def upvotes = userTipVoteService.upvotes(testCaseId)
        def downvotes = userTipVoteService.downvotes(testCaseId)
        jsonObject.put("upvotes", upvotes)
        jsonObject.put("downvotes", downvotes)

        respond jsonObject
    }

    def getProblemsCount() {
        def result = problemService.countProblemsByLanguageAndType()
        result.each {
            def a = Problem.ProblemType.values().find { pt ->
                pt.ordinal() == it.problem_type
            }
            it.problem_type = a.toString()
        }
        respond result
    }

    def boolean canUserEditProblem(User user, Problem problem) {
        def roles = user.getAuthorities().collect { it.authority }

        return (roles.contains("ROLE_ADMIN")
                || (problem.userSuggest.id == user.id)
                || (!problem.userSuggest.userInstitutions.isEmpty() && UserInstitution.findByUserAndRoleAndInstitutionInList(
                user,
                UserInstitution.Role.ADMIN_INST,
                (problem.userSuggest.userInstitutions*.institution).toList())));
    }
}
