package com.thehuxley

import com.thehuxley.error.ErrorReason
import com.thehuxley.error.ErrorResponse
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.rest.RestfulController
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.json.JsonBuilder
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.JSONException
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus

import java.text.MessageFormat

class QuestionnaireController {

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET"]

    def plagiarismService
    def questionnaireService
    def problemService
    def userService
    def springSecurityService
    NotificationService notificationService

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def clone(long questionnaireId) {
        Questionnaire questionnaire = deserialize(false, null)
        User currentUser = springSecurityService.currentUser as User

        if (canChangeQuizz(currentUser, questionnaire)) {
            onValid questionnaire, {
                respond(questionnaireService.save(questionnaire, currentUser, questionnaireId))
            }
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("isAuthenticated()")
    def index() {
        if (params.sort && !questionnaireService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        def currentUser = springSecurityService.currentUser as User

        def parameters = questionnaireService.normalize(params)

        if (parameters.filter && parameters.filter.contains("OWN")) {
            parameters.groups = []
            parameters.groups.addAll(UserGroup.findAllByUserAndRole(currentUser, UserGroup.Role.TEACHER).group)
            parameters.groups.addAll(UserGroup.findAllByUserAndRole(currentUser, UserGroup.Role.TEACHER_ASSISTANT).group)
        }

        respond questionnaireService.list(parameters)
    }

    @Secured("permitAll")
    def show(Long id) {

        User currentUser = springSecurityService.currentUser as User
        def response = questionnaireService.get(id, currentUser)
        def questionnaire = response.questionnaire

        if (questionnaire) {
            if (canChangeQuizz(currentUser, questionnaire) || (canViewQuizz(currentUser, questionnaire) && !QuestionnaireProblem.findAllByQuestionnaire(questionnaire).empty)) {
                questionnaireService.saveStudentPresence(questionnaire, currentUser)
                respond response
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("isAuthenticated()")
    def delete(Long id) {

        def currentUser = springSecurityService.currentUser as User
        def questionnaire = Questionnaire.get(id)

        if (canChangeQuizz(currentUser, questionnaire)) {

            if (questionnaireService.delete(questionnaire)) {
                render status: HttpStatus.NO_CONTENT
            } else {
                render status: HttpStatus.INTERNAL_SERVER_ERROR
            }
        } else {
            render status: HttpStatus.FORBIDDEN
        }

    }

    @Secured("permitAll")
    def getProblems(Long questionnaireId, Long problemId) {
        def questionnaire = Questionnaire.load(questionnaireId)
        def currentUser = springSecurityService.currentUser as User

        if (canChangeQuizz(currentUser, questionnaire) || canViewQuizz(currentUser, questionnaire)) {
            if (problemId) {
                respond problemService.findByQuestionnaire(Problem.load(problemId), questionnaire, Problem.Status.ACCEPTED)
            } else {

                if (params.sort && !problemService.isSortable(params.sort)) {
                    forward(controller: "Error", action: "wrongSortParam")
                    return
                }

                respond problemService.findAllByQuestionnaire(questionnaireId, Problem.Status.ACCEPTED)
            }

        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("permitAll")
    def getUsers(Long questionnaireId) {

        def questionnaire = Questionnaire.load(questionnaireId)
        def currentUser = springSecurityService.currentUser as User

        println 'lkasdlasjdklasjdas'
        if (canViewQuizz(currentUser, questionnaire)) {

            if (params.sort && !userService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }
            println 'lkasdlasjdklasjdas@@@@@@@@@@@@@@2'
            respond userService.findAllByQuestionnaire(questionnaire, userService.normalize(params))

        } else {
            println 'AS>DDD'
            render status: HttpStatus.FORBIDDEN
        }

    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getPenalty(Long userId, Long questionnaireId, Long problemId) {
        respond QuestionnaireUserPenalty.createCriteria().get {
            createAlias('user', 'u')
            createAlias('questionnaireProblem', 'qp')
            createAlias('questionnaireProblem.questionnaire', 'q')
            createAlias('questionnaireProblem.problem', 'p')
            eq('u.id', userId)
            eq('q.id', questionnaireId)
            eq('p.id', problemId)
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getSimilarity(Long questionnaireId, Long plagiarismId) {

        def currentUser = springSecurityService.currentUser as User
        def questionnaire = Questionnaire.load(questionnaireId)

        if (canChangeQuizz(currentUser, questionnaire)) {
            respond Plagiarism.get(plagiarismId)
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def confirmSimilarity(Long questionnaireId, Long plagiarismId) {

        def addSimilarityPenalty = { user, questionnaire, problem ->
            def questionnaireProblem = QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)
            def questionnaireUserPenalty = QuestionnaireUserPenalty.findByUserAndQuestionnaireProblem(user, questionnaireProblem)

            if (questionnaireUserPenalty) {
                questionnaireUserPenalty.penalty = 0
            } else {
                questionnaireUserPenalty = new QuestionnaireUserPenalty(
                        questionnaireProblem: questionnaireProblem,
                        user: user,
                        penalty: 0
                )
            }
            questionnaireUserPenalty.save()
        }

        def removeSimilarityPenalty = { user, questionnaire, problem ->
            def questionnaireProblem = QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)
            def questionnaireUserPenalty = QuestionnaireUserPenalty.findByUserAndQuestionnaireProblem(user, questionnaireProblem)

            if (questionnaireUserPenalty) {
                questionnaireUserPenalty.delete()
            }
        }

        def currentUser = springSecurityService.currentUser as User
        def questionnaire = Questionnaire.get(questionnaireId)

        if (canChangeQuizz(currentUser, questionnaire)) {
            def plagiarism = Plagiarism.createCriteria().get {
                createAlias("submission1", "s1")
                createAlias("submission1.user", "u1")
                createAlias("submission1.problem", "p1")
                createAlias("submission2", "s2")
                createAlias("submission2.user", "u2")
                createAlias("submission2.problem", "p2")
                eq("id", plagiarismId)
            }

            def originalSubmission = plagiarism.submission1.submissionDate < plagiarism.submission2.submissionDate ?
                    plagiarism.submission1 : plagiarism.submission2

            def plagiarizedSubmission = plagiarism.submission1.submissionDate > plagiarism.submission2.submissionDate ?
                    plagiarism.submission1 : plagiarism.submission2

            def json = request.JSON

            def problem = originalSubmission.problem

            if (json) {
                def option = Integer.valueOf(json.option)
                if (option == 1) {
                    addSimilarityPenalty(originalSubmission.user, questionnaire, problem)
                    removeSimilarityPenalty(plagiarizedSubmission.user, questionnaire, problem)
                } else if (option == 2) {
                    addSimilarityPenalty(plagiarizedSubmission.user, questionnaire, problem)
                    removeSimilarityPenalty(originalSubmission.user, questionnaire, problem)
                } else if (option == 3) {
                    addSimilarityPenalty(originalSubmission.user, questionnaire, problem)
                    addSimilarityPenalty(plagiarizedSubmission.user, questionnaire, problem)
                } else {
                    removeSimilarityPenalty(originalSubmission.user, questionnaire, problem)
                    removeSimilarityPenalty(plagiarizedSubmission.user, questionnaire, problem)
                }
            }

            respond plagiarismService.changeStatus(Plagiarism.get(plagiarismId), Plagiarism.Status.CONFIRMED)
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def discardSimilarity(Long questionnaireId, Long plagiarismId) {

        def removeSimilarityPenalty = { user, questionnaire, problem ->
            def questionnaireProblem = QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)
            def questionnaireUserPenalty = QuestionnaireUserPenalty.findByUserAndQuestionnaireProblem(user, questionnaireProblem)

            if (questionnaireUserPenalty) {
                questionnaireUserPenalty.delete()
            }
        }

        def currentUser = springSecurityService.currentUser as User
        def questionnaire = Questionnaire.load(questionnaireId)

        if (canChangeQuizz(currentUser, questionnaire)) {
            def plagiarism = Plagiarism.createCriteria().get {
                createAlias("submission1", "s1")
                createAlias("submission1.user", "u1")
                createAlias("submission1.problem", "p1")
                createAlias("submission2", "s2")
                createAlias("submission2.user", "u2")
                createAlias("submission2.problem", "p2")
                eq("id", plagiarismId)
            }

            def problem = plagiarism.submission1.problem

            removeSimilarityPenalty(plagiarism.submission1.user, questionnaire, problem)
            removeSimilarityPenalty(plagiarism.submission2.user, questionnaire, problem)

            respond plagiarismService.changeStatus(Plagiarism.load(plagiarismId), Plagiarism.Status.DISCARDED)
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getSimilarities(Long questionnaireId) {

        def currentUser = springSecurityService.currentUser as User
        def questionnaire = Questionnaire.get(questionnaireId)

        if (questionnaire) {
            if (params.sort && !plagiarismService.isSortable(params.sort)) {
                forward(controller: "Error", action: "wrongSortParam")
                return
            }

            def similarities = plagiarismService.findAllByQuestionnaire(questionnaire, plagiarismService.normalize(params))

            response.setHeader("total", similarities.totalCount as String)

            respond similarities
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def save() {
        Questionnaire questionnaire = deserialize(false, null)
        User currentUser = springSecurityService.currentUser as User

        if (canChangeQuizz(currentUser, questionnaire)) {
            onValid questionnaire, {

                def result =  questionnaireService.save(questionnaire, currentUser)

                notificationService.notify(currentUser,
                        questionnaire.group, true, NotificationPreferences.Type.QUESTIONNAIRE_CREATED,
                        [ questionnaire.title, currentUser.id, currentUser.name, result.id as long, questionnaire.title, questionnaire.group.url, questionnaire.group.name ])

                respond result
            }
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def update(Long id) {
        User currentUser = springSecurityService.currentUser as User
        Questionnaire questionnaire = Questionnaire.get(id)

        if (questionnaire) {
            if (currentUser && canUpdate(questionnaire)) {
                questionnaire = deserialize(true, questionnaire.id)
                onValid questionnaire, {
                    respond questionnaireService.save(questionnaire, currentUser)
                }
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getPresentUsers(long questionnaireId) {
        User currentUser = springSecurityService.currentUser as User
        Questionnaire questionnaire = Questionnaire.get(questionnaireId)

        if (canChangeQuizz(currentUser, questionnaire)) {
            respond questionnaireService.findStudentPresence(questionnaire)
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def validate() {
        onValid deserialize(false, null) as Questionnaire, {
            render status: HttpStatus.ACCEPTED
        }
    }

    def canUpdate(Questionnaire questionnaire) {
        return canChangeQuizz(springSecurityService.currentUser as User, questionnaire)
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addProblem(Long questionnaireId, Long problemId) {

        Questionnaire questionnaire = Questionnaire.get(questionnaireId)

        if (canUpdate(questionnaire)) {
            Problem problem = Problem.get(problemId)
            def score = 0

            if (params.score) {
                score = params.score
            } else {
                if (request.JSON["score"]) {
                    score = request.JSON["score"]
                }
            }

            try {
                score = score as Double
            } catch (NumberFormatException e) {
                score = score.replace(",", ".")
                score = score as Double
            }

            respond questionnaireService.addProblem(questionnaire, problem, score as Double, problemService.normalize(params as GrailsParameterMap))
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def removeProblem(Long questionnaireId, Long problemId) {
        def questionnaire = Questionnaire.get(questionnaireId)

        if (canUpdate(questionnaire)) {
            respond questionnaireService.removeProblem(
                    questionnaire,
                    Problem.get(problemId),
                    params
            )
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addProblemRestriction(Long questionnaireId, Long problemId) {
        Questionnaire questionnaire = Questionnaire.get(questionnaireId)

        if (canUpdate(questionnaire)) {
            def json = request.JSON;
            def problemRestriction = QuizProblemRestriction.findByQuizIdAndProblemId(questionnaireId, problemId)

            if (problemRestriction) {
                problemRestriction.restrictions = new JsonBuilder(json['restrictions']).toString()
                problemRestriction.penalty = json['penalty'] as Float

                notify 'submission.evaluateRestrictions', problemRestriction
            } else {
                problemRestriction = new QuizProblemRestriction([
                        quizId: questionnaireId,
                        problemId: problemId,
                        restrictions: new JsonBuilder(json['restrictions']).toString(),
                        penalty: json['penalty'] as Float
                ])
            }

            respond problemRestriction.merge()
        } else {
            render status: HttpStatus.FORBIDDEN
        }
    }

    @Secured("isAuthenticated()")
    def findRestrictions(long questionnaireId) {

        if (params['problem']) {
            respond questionnaireService.findRestrictions(questionnaireId, params['problem'] as Long)
        } else  {
            respond questionnaireService.findRestrictions(questionnaireId)
        }
    }

    @Secured("isAuthenticated()")
    def findRestrictionsResult(long questionnaireId) {
        respond questionnaireService.findRestrictionsResult(questionnaireId)
    }

    @Secured("permitAll")
    def getData(Long questionnaireId) {
        respond questionnaireService.getData(Questionnaire.load(questionnaireId), params.key)
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getQuizzWithConsolidatedScores(Long questionnaireId) {
        // FIXME Não queria ter que fazer isso, mas o modelo de questionário está ruim e essa foi a solução mais rápida
        def users = questionnaireService.findStudentsId(questionnaireId)
        def scores = questionnaireService.findScores(questionnaireId)

        def penalties = QuestionnaireUserPenalty.findAll("""FROM QuestionnaireUserPenalty qp
											LEFT JOIN FETCH qp.user
											LEFT JOIN FETCH qp.questionnaireProblem
											WHERE qp.questionnaireProblem.questionnaire.id = :questionnaire""", [questionnaire: questionnaireId])
        def result = []

        users.each { Long userId ->

            def uScore = [
                    "userId"         : userId,
                    "correctProblems": [],
                    "penalties"      : []
            ];

            scores.each {
                if (it.userId == userId) {
                    uScore.correctProblems.add(it)
                }
            }

            penalties.each { QuestionnaireUserPenalty p ->
                if (p.userId == userId) {
                    uScore.penalties.add([
                            "questionnaireProblemId": p.questionnaireProblemId,
                            "penalty"               : p.penalty
                    ])
                }
            }

            result.add(uScore)
        }

        render result as JSON
    }

    @Secured("permitAll")
    def getUserScores(Long userId, params) {
        // PRECISA REMOVER ISSO NO CLIENTE. USUARIO NAO PRECISA VER NOTA DO QUESTIONARIO ANTES DELE ENCERRAR
        // Talvez só exibir os questionários em aberto e quantas questões foram respondidas

        Date maxDate = params.endDateGe ?
                DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(params.endDateGe).toDate() : new LocalDate().minusDays(1).toDate();

        def result =  questionnaireService.findUserScores(userId, maxDate, params) as JSON;
        render result
    }

    @Secured("permitAll")
    def getUserProblemScores(Long questionnaireId, Long userId) {

        Questionnaire quest = Questionnaire.load(questionnaireId);
        Date now = new Date();

        if (quest.endDate.after(now) && springSecurityService.currentUser.id != userId && !canChangeQuizz(springSecurityService.currentUser, quest)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        def scores = questionnaireService.findUserProblemScores(quest, userId)
        List<QuestionnaireProblem> problems = QuestionnaireProblem.findAllByQuestionnaire(quest, [sort: 'score', order: 'asc']);
        def result = ["problems": problems.problem, "scores": scores];
        render result as JSON;
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def getQuizzPenalties(Long questionnaireId) {
        def questionnaire = Questionnaire.load(questionnaireId)

        def penalties = QuestionnaireUserPenalty.findAll("""FROM QuestionnaireUserPenalty qp
											LEFT JOIN FETCH qp.user
											LEFT JOIN FETCH qp.questionnaireProblem
											WHERE qp.questionnaireProblem.questionnaire = :questionnaire""", [questionnaire: questionnaire])

        render penalties as JSON
    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def failUserQuizz(Long userId, Long questionnaireId) {

        def questionnaire = Questionnaire.load(questionnaireId)
        def user = User.load(userId)

        if (canChangeQuizz(springSecurityService.currentUser, questionnaire)) {
            def questionnaireProblems = QuestionnaireProblem.findAllByQuestionnaire(questionnaire)
            def questionnaireUserPenalty = QuestionnaireUserPenalty.findAllByUser(user)

            for (problem in questionnaireProblems) {

                def problemPenalty;

                for (penalty in questionnaireUserPenalty) {
                    if (penalty.questionnaireProblem.id == problem.id) {
                        problemPenalty = penalty;
                        break;
                    }
                }

				if (problemPenalty == null) {
					problemPenalty = new QuestionnaireUserPenalty(
							questionnaireProblem: problem,
							user: user,
							penalty: 0
					);
				} else {
					problemPenalty.penalty = 0
				}

                problemPenalty.save()
            }


        } else {
            render status: HttpStatus.FORBIDDEN
        }

		render (user as JSON)
	}

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def addPenalty(Long userId, Long questionnaireId, Long problemId) {

        def questionnaire = Questionnaire.load(questionnaireId)
        def currentUser = User.load(userId)
        def problem = Problem.load(problemId)

        if (!canChangeQuizz(springSecurityService.currentUser, questionnaire)) {
            render status: HttpStatus.FORBIDDEN
            return
        }

        def penalty

        if (params.penalty) {
            try {
                penalty = params.getDouble("penalty")
            } catch (Exception e) {
                render status: HttpStatus.BAD_REQUEST
                return
            }
        } else if (request.JSON["penalty"]) {
            try {
                penalty = request.JSON["penalty"] as Double
            } catch (Exception e) {
                render status: HttpStatus.BAD_REQUEST
                return
            }
        } else {
            render status: HttpStatus.BAD_REQUEST
            return
        }

        def questionnaireProblem = QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)

        if (questionnaireProblem) {
            def questionnaireUserPenalty = QuestionnaireUserPenalty.findByUserAndQuestionnaireProblem(
                    currentUser,
                    questionnaireProblem
            )

			if (questionnaireUserPenalty && penalty == -1) {
				questionnaireUserPenalty.delete()
				render(questionnaireUserPenalty as JSON)
				return
			}

            if (questionnaireUserPenalty) {

                questionnaireUserPenalty.penalty = penalty

            } else {
                questionnaireUserPenalty = new QuestionnaireUserPenalty(
                        questionnaireProblem: questionnaireProblem,
                        user: currentUser,
                        penalty: penalty
                )
            }

            questionnaireUserPenalty.save()

            questionnaireUserPenalty.hasErrors() ?
                    render(status: HttpStatus.BAD_REQUEST) :
                    render(questionnaireUserPenalty as JSON)

        } else {
            render status: HttpStatus.BAD_REQUEST
        }

    }

    @Secured("hasAnyRole('ROLE_TEACHER_ASSISTANT', 'ROLE_TEACHER', 'ROLE_ADMIN_INST', 'ROLE_ADMIN')")
    def importQuestionnaires() {
        def json = request.JSON
        def groups = []
        User currentUser = springSecurityService.currentUser as User
        json["groups"].forEach {
            def group = Group.get(it as Long)
            groups.add(group)
        }
        json["quizzes"].forEach {
            Questionnaire questionnaire = deserializeJSON(it)
            def quizToClone = it["id"]
            groups.forEach {
                questionnaire.group = it
                if (canChangeQuizz(currentUser, questionnaire)) {
                    onValid questionnaire, {
                        questionnaireService.save(questionnaire, currentUser, quizToClone)
                    }
                } else {
                    render status: HttpStatus.FORBIDDEN
                }
            }
        }
        render status: HttpStatus.ACCEPTED
    }

    def onValid(Questionnaire questionnaire, c) {
        if (questionnaire.hasErrors()) {
            invalidQuestionnaire(questionnaire)
        } else {
            c()
        }
    }


    Questionnaire deserialize(update, id) {
        def questionnaire = update ? Questionnaire.get(id as Long) : new Questionnaire()

        def json = request.JSON
        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()

        questionnaire.title = json["title"] as String ?: questionnaire.title
        questionnaire.description = json["description"] as String ?: questionnaire.description

        if (json["partialScore"] != null) {
            questionnaire.partialScore = json["partialScore"] as Boolean
        } else {
            questionnaire.partialScore = questionnaire.partialScore
        }

        if (json["group"] && json["group"]["id"]) {
            questionnaire.group = Group.get(json["group"]["id"] as Long) ?: questionnaire.group
        } else {
            questionnaire.group = questionnaire.group
        }


        try {
            questionnaire.startDate = json["startDate"] ?
                    formatter.parseDateTime(json["startDate"] as String).toDate() :
                    questionnaire.startDate

            questionnaire.endDate = json["endDate"] ?
                    formatter.parseDateTime(json["endDate"] as String).toDate() :
                    questionnaire.endDate

        } catch (IllegalArgumentException e) {
            log.info("unexpected-err-saving-quiz", e)
        }

        questionnaire.validate()

        return questionnaire
    }

    Questionnaire deserializeJSON(json) {
        def questionnaire = new Questionnaire()

        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()

        questionnaire.title = json["title"] as String ?: questionnaire.title
        questionnaire.description = json["description"] as String ?: questionnaire.description

        if (json["partialScore"] != null) {
            questionnaire.partialScore = json["partialScore"] as Boolean
        } else {
            questionnaire.partialScore = questionnaire.partialScore
        }

        if (json["group"] && json["group"]["id"]) {
            questionnaire.group = Group.get(json["group"]["id"] as Long) ?: questionnaire.group
        } else {
            questionnaire.group = questionnaire.group
        }


        try {
            questionnaire.startDate = json["startDate"] ?
                    formatter.parseDateTime(json["startDate"] as String).toDate() :
                    questionnaire.startDate

            questionnaire.endDate = json["endDate"] ?
                    formatter.parseDateTime(json["endDate"] as String).toDate() :
                    questionnaire.endDate

        } catch (IllegalArgumentException e) {

        }

        questionnaire.validate()

        return questionnaire
    }

    def invalidQuestionnaire(Questionnaire questionnaire) {
        def errors = []

        questionnaire.errors.each {
            it.getAllErrors().each {
                if (it.arguments[0] == "title") {

                    if (it.code == "blank") {
                        errors.add(ErrorReason.QUESTIONNAIRE_TITLE_CANNOT_BE_BLANK)
                    }

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.QUESTIONNAIRE_TITLE_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.QUESTIONNAIRE_TITLE_TOO_SMALL.setParams(it.arguments[2]))
                    }

                    if (it.code == "nullable") {
                        errors.add(ErrorReason.QUESTIONNAIRE_TITLE_CANNOT_BE_NULL)
                    }

                } else if (it.arguments[0] == "description") {

                    if (it.code == "size.toobig") {
                        errors.add(ErrorReason.QUESTIONNAIRE_DESCRIPTION_TOO_BIG.setParams(it.arguments[2]))
                    }

                    if (it.code == "size.toosmall") {
                        errors.add(ErrorReason.QUESTIONNAIRE_DESCRIPTION_TOO_SMALL.setParams(it.arguments[2]))
                    }

                } else if (it.arguments[0] == "group") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.QUESTIONNAIRE_GROUP_CANNOT_BE_NULL)
                    }
                } else if (it.arguments[0] == "endDate") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.QUESTIONNAIRE_END_DATE_CANNOT_BE_NULL)
                    }
                } else if (it.arguments[0] == "startDate") {
                    if (it.code == "nullable") {
                        errors.add(ErrorReason.QUESTIONNAIRE_START_DATE_CANNOT_BE_NULL)
                    }
                } else {
                    errors.add(ErrorReason.GENERIC.setParams(it.code + " - " + MessageFormat.format(it.defaultMessage, it.arguments)))
                }
            }
        }

        if (questionnaire.endDate < questionnaire.startDate) {
            errors.add(ErrorReason.QUESTIONNAIRE_END_DATE_CANNOT_BE_EARLIER_THAN_START_DATA)
        }

        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, errors)

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    private boolean canViewQuizz(User currentUser, Questionnaire quizz) {
        boolean hasPermission = currentUser &&
                (currentUser.authorities.authority.contains('ROLE_ADMIN') ||
                        UserInstitution.findByUserAndInstitutionAndRole(currentUser, quizz.group.institution, UserInstitution.Role.ADMIN_INST));

        if (!hasPermission) {
            UserGroup ug = UserGroup.findByUserAndGroup(currentUser, quizz.group)
            hasPermission = ug && (quizz.startDate < new Date() || ug.role == UserGroup.Role.TEACHER || ug.role == UserGroup.Role.TEACHER_ASSISTANT)
        }

        return hasPermission
    }

    private boolean canChangeQuizz(User currentUser, Questionnaire questionnaire) {

        if (!currentUser) return false
        if (currentUser.authorities.authority.contains('ROLE_ADMIN')) return true

        def group = questionnaire.group

        if (UserInstitution.findByUserAndInstitutionAndRole(
                currentUser, group.institution, UserInstitution.Role.ADMIN_INST)) {
            return true
        }

        def userGroup = UserGroup.findByUserAndGroup(currentUser, group)

        if (!userGroup) {
            return false
        }

        return (userGroup.role == UserGroup.Role.TEACHER || userGroup.role == UserGroup.Role.TEACHER_ASSISTANT)
    }

}
