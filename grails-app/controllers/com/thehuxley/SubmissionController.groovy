package com.thehuxley

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.hibernate.ObjectNotFoundException
import org.springframework.http.HttpStatus

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SubmissionController {

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET", getSubmissionFile: "GET"]

    def submissionService
    def springSecurityService
    def problemService
    def testCaseService
    def groupService

    def avatarUrl = "${grailsApplication.config.huxley.avatarURL}"

    @Secured("permitAll")
    def index() {

        if (params.sort && !submissionService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        def currentUser = springSecurityService.currentUser as User
        if(!currentUser) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        if (params.group) {
            def group = Group.load(params.group);

            if (!groupService.hasGroupAccess(currentUser, group, true)) {
                def userInstitution = UserInstitution.findByUserAndInstitution(currentUser, group.institution)
                if (userInstitution != null && userInstitution.role != UserInstitution.Role.TEACHER) {
                    params.user = currentUser.id
                }
            }
        }

        if (params.quiz) {
            def quiz = Questionnaire.get(params.quiz)
            if (quiz) {
                if (!groupService.hasGroupAccess(currentUser, quiz.group, true)) {
                    if (UserInstitution.findByUserAndInstitution(currentUser, quiz.group.institution).role != UserInstitution.Role.TEACHER) {
                        params.user = currentUser.id
                    }
                }

                def problems = QuestionnaireProblem.findAllByQuestionnaire(quiz)*.problem

                params.problems = problems
            }
        }

        if (!params.group && !params.quiz) {
            def authorities = currentUser.authorities.authority
            if (!authorities.contains('ROLE_TEACHER') &&
                    !authorities.contains('ROLE_ADMIN') &&
                    !authorities.contains('ROLE_ADMIN_INST')) {

                if (!params.user || !authorities.contains('ROLE_TEACHER_ASSISTANT')
                        || !submissionService.userCanAccessSubmission(currentUser.id, params.user as Long)) {
                    params.user = currentUser.id
                }
            }
        }

        def submissions = submissionService.list(submissionService.normalize(params))

        if (submissions != null) {
            submissions.each { Submission submission ->
                submission.metaClass.showTestCaseEvaluations = false
            }

            response.setHeader("total", submissions.totalCount as String)
        }


        respond submissions
    }

    @Secured("permitAll")
    def submissionStats() {
        def submissions = []

        if (params.stats == "fastest") {
            submissions = submissionService.fastestSubmissions(params.problem as Long, avatarUrl + '/thumbs')
        } else if (params.stats == "last") {
            params.max = 10
            params.order = 'desc'
            params.sort = 'submissionDate'
            submissions = submissionService.list(submissionService.normalize(params))
        }

        respond submissions
    }

    @Secured("permitAll")
    def show(Long id) {
        def currentUser = springSecurityService.currentUser as User
        def submission = submissionService.get(id)

        if (currentUser) {
            def roles = currentUser?.getAuthorities().collect { it.authority }

            if (!submission) {
                render status: HttpStatus.NOT_FOUND
                return
            }

            if (roles.contains('ROLE_ADMIN') || submission.user.id == currentUser.id || submissionService.userCanAccessSubmission(currentUser.id, submission.user.id)) {
                bindShowDiffProperty(currentUser, submission)
                respond submission
                return
            }
        }

        render status: HttpStatus.FORBIDDEN
    }

    @Secured("isAuthenticated()")
    def showEvaluation(Long submissionId) {
        def result =  [ evaluation: submissionService.getEvaluation(submissionId) ]
        respond result
    }

    @Secured("permitAll")
    def userStats(Long userId) {

        def response = submissionService.generateUserStats([userId: userId])
        def jsonObject = JSON.parse((response[userId] as JSON).toString())
        jsonObject.put("totalProblemsByTopic", problemService.getTotalProblemsByTopics())
        jsonObject.put("totalProblemsByNd", problemService.getTotalProblemsByNd())
        render jsonObject as JSON
    }

    @Secured("permitAll")
    def groupStats(params) {

        def response = submissionService.generateUserStats(params)

        render response as JSON
    }

    @Secured("permitAll")
    def groupThermometer(params) {
        def stats = submissionService.generateThermometerStats(params)

        render stats as JSON
    }

    @Secured("permitAll")
    def problemStats(Long problemId, params) {
        params.excludeTopics = true;
        def response = submissionService.generateProblemStats(problemId, params)

        render response as JSON
    }

    @Secured("permitAll")
    def getSubmissionFile(Long submissionId) {

        def submission = Submission.load(submissionId)
        def user = springSecurityService.currentUser as User
    
        if (user && canAccessSubmission(user, submission)) {
            response << submission.getSourceCode()
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    def findRestrictionEvaluation(long submissionId) {
        respond submissionService.findRestrictionEvaluation(submissionId)
    }

    @Secured("isAuthenticated()")
    def downloadTestCase(long submissionId, long testCaseId) {

        TestCase testCase = TestCase.get(testCaseId)
        Submission submission = Submission.get(submissionId)

        def currentUser = springSecurityService.currentUser as User
        def roles = currentUser.getAuthorities().collect { it.authority }

        if (roles.contains("ROLE_ADMIN") || (roles.contains("ROLE_TEACHER") && submission.problemId == testCase.problemId)) {
            if (request.getHeader("Accept").equals("application/zip")) {
                response.setHeader "Content-disposition", "attachment; filename=${testCaseId}.zip"
                response.setHeader 'Content-Type', 'application/zip'

                testCaseService.zip(testCase, response.outputStream)
            } else {
                render status: HttpStatus.NO_CONTENT
            }
        } else {
            render status: HttpStatus.FORBIDDEN
        }


    }

    @Secured("permitAll")
    def problemTrySource(Long problemId, int trynumber) {
        def user = springSecurityService.currentUser as User

        if (!user) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        def submission

        if (params.userId) {
            submission = Submission.findByProblemAndUserAndTries(Problem.get(problemId), User.load(params.userId), trynumber)
        } else {
            submission = Submission.findByProblemAndUserAndTries(Problem.get(problemId), user, trynumber)
        }

        if (canAccessSubmission(user, submission)) {
            bindShowDiffProperty(user, submission)
            respond submission
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("permitAll")
    def getDiffFile(Long submissionId) {

        def submission = Submission.get(submissionId)
        def user = springSecurityService.currentUser as User

        try {
            if (user) {
                def roles = user.getAuthorities().collect { it.authority }
                if (roles.contains('ROLE_ADMIN') ||
                        submissionService.userCanAccessSubmission(user.id, submission.user.id)
                ) {
                    if (submission.diff() != null) {
                        render contentType: "application/json", text: submission.diff()
                    } else {
                        render status: HttpStatus.NOT_FOUND
                    }
                } else {
                    render status: HttpStatus.FORBIDDEN
                }
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } catch (ObjectNotFoundException e) {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def reevaluate(Long submissionId) {
        def submission = Submission.get(submissionId)
        def user = springSecurityService.currentUser as User

        if (canAccessSubmission(user, submission)) {
            params.submission = submissionId
            respond submissionService.reevaluate(submissionService.normalize(params))
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def reevaluateByProblem(Long problemId) {

        if (request.JSON) {
            extractJSON()
        }

        params.problem = problemId

        respond submissionService.reevaluate(submissionService.normalize(params))
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def reevaluateAll() {

        if (request.JSON) {
            extractJSON()
        }

        respond submissionService.reevaluate(submissionService.normalize(params))
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def update(Long id) {
        def submission = Submission.get(id)

        def json = request.JSON

        if (submission) {

            submission.evaluation = json["evaluation"] ? Submission.Evaluation.valueOf(json["evaluation"] as String) : Submission.Evaluation.HUXLEY_ERROR
            submission.time = json["time"] as Double ?: -1D
            submission.errorMsg = json["errorMsg"] as String ?: ""

            respond submissionService.update(submission)
        } else {
            render status: HttpStatus.NOT_FOUND
        }

    }

    def extractJSON() {
        params.submission = request.JSON["submission"]
        params.problem = request.JSON["problem"]
        params.language = request.JSON["language"]
        params.user = request.JSON["user"]

        params.tries = request.JSON["tries"]
        params.triesGt = request.JSON["triesGt"]
        params.triesGe = request.JSON["triesGe"]
        params.triesLt = request.JSON["triesLt"]
        params.triesLe = request.JSON["triesLe"]
        params.triesNe = request.JSON["triesNe"]

        params.time = request.JSON["time"]
        params.timeGt = request.JSON["timeGt"]
        params.timeGe = request.JSON["timeGe"]
        params.timeLt = request.JSON["timeLt"]
        params.timeLe = request.JSON["timeLe"]
        params.timeNe = request.JSON["timeNe"]

        params.submissionDate = request.JSON["submissionDate"]
        params.submissionDateGt = request.JSON["submissionDateGt"]
        params.submissionDateGe = request.JSON["submissionDateGe"]
        params.submissionDateLt = request.JSON["submissionDateLt"]
        params.submissionDateLe = request.JSON["submissionDateLe"]
        params.submissionDateNe = request.JSON["submissionDateNe"]


        params.evaluations = request.JSON["evaluations"]

        params.excludeEvaluations = request.JSON["excludeEvaluations"]
    }

    @Secured("isAuthenticated()")
    def getTestCase(Long submissionId) {
        def submission = Submission.load(submissionId)

        def problem = submission.problem
        def currentUser = springSecurityService.currentUser as User
        def testCase = TestCase.get(submission.testCase.id)
        def roles = currentUser.getAuthorities().collect { it.authority }

        if (roles.contains('ROLE_ADMIN') ||
                (testCase && testCase.example) ||
                submissionService.userCanAccessSubmission(currentUser.id, submission.user.id)
        ) {
            if (request.getHeader("Accept").equals("application/zip")) {
                response.setHeader "Content-disposition", "attachment; filename=${testCaseId}.zip"
                response.setHeader 'Content-Type', 'application/zip'

                testCase = TestCase.load(submission.testCase.id)

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

                if (testCase.tip) {
                    ZipEntry tip = new ZipEntry("${testCaseId}.tip");

                    zos.putNextEntry(tip)
                    zos.write(testCase.tip.getBytes("UTF-8"))
                    zos.closeEntry()
                }

                zos.close()
            } else {
                respond testCaseService.findByProblem(testCase, problem)
            }
        } else {
            if (testCase != null) {
                testCase.input = null
                testCase.output = null

                def res = testCaseService.findByProblem(testCase, problem)

                render (res as JSON)
            }
        }
    }

    private boolean canAccessSubmission(User user, Submission submission) {

        def roles = user.getAuthorities().collect { it.authority }

        boolean canAccess = false
        if (roles.contains('ROLE_ADMIN') || submission.user.id == user.id) {
            canAccess = true
        } else {
            def institution = submission.user.institution

            def userInstitution = UserInstitution.findByUserAndInstitution(user, institution)

            boolean hasIntitutionAccess = userInstitution &&
                    (userInstitution.role == UserInstitution.Role.ADMIN_INST || userInstitution.role == UserInstitution.Role.TEACHER)

            if (hasIntitutionAccess || submissionService.userCanAccessSubmission(user.id, submission.user.id)) {
                canAccess = true
            }
        }

        return canAccess
    }

    private void bindShowDiffProperty(User user, Submission submission) {

        def roles = user?.getAuthorities().collect { it.authority }

        if (Collections.disjoint(roles,
                Arrays.asList("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_ADMIN_INST", "ROLE_TEACHER_ASSISTANT"))) {
            submission.testCaseEvaluations.each { testCaseEvaluation ->
                if (!testCaseEvaluation.testCase.example) {
                    testCaseEvaluation.metaClass.showDiff = false
                }
            }
        }
    }
}