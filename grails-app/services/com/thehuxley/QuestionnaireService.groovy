package com.thehuxley

import com.google.common.cache.CacheBuilder
import com.thehuxley.excel.QuizesToExcel
import grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.SessionFactory
import org.hibernate.result.Output
import org.hibernate.transform.Transformers
import org.hibernate.type.StandardBasicTypes
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.TimeUnit

class QuestionnaireService {

    static final long VIEW_EVENT_INTERVAL = 1 * 60 * 1000;

    def groupService
    def problemService
    def lastUserViewEvent = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build()

    SessionFactory sessionFactory

    def get(Long questionnaireId, User currentUser) {
        def response = [
            questionnaire: Questionnaire.get(questionnaireId)
        ]

        if (currentUser && response.questionnaire) {
            response.role = UserGroup.findByUserAndGroup(currentUser, response.questionnaire.group)?.role
        }

        response
    }

    def save(Questionnaire questionnaire, User currentUser) {
        log.info(questionnaire.id ? "quiz-update: $questionnaire.id" : "quiz-create: $questionnaire.title")
        questionnaire.save()
    }

    def save(Questionnaire questionnaire, User currentUser, long questionnaireToCloneId) {
        log.info("clone-quiz: $questionnaireToCloneId")
        Questionnaire questionnaireToClone = Questionnaire.get(questionnaireToCloneId)
        questionnaire.score = questionnaireToClone.score

        questionnaire.save()

        QuestionnaireProblem.findAllByQuestionnaire(questionnaireToClone).each {
            new QuestionnaireProblem(score: it.score, questionnaire: questionnaire, problem: it.problem).save()
        }

        questionnaire
    }

    def saveStudentPresence(Questionnaire questionnaire, User user) {

        String cacheKey = questionnaire.id + '#' + user.id
        Date viewDate = lastUserViewEvent.getIfPresent(cacheKey)

        Date now = new Date()
        if (viewDate && (now.getTime() - viewDate.getTime() < VIEW_EVENT_INTERVAL)) return;

        if (questionnaire.startDate < now && now < questionnaire.endDate) {
            lastUserViewEvent.put(cacheKey, now)
            log.info("save-student-presence: { quiz: $questionnaire.id }")
            new QuestionnaireViewEvent([quizId: questionnaire.id, userId: user.id, viewDate: now]).save()
        }
    }

    def findStudentPresence(Questionnaire questionnaire) {
        return sessionFactory.currentSession.createQuery(
                """select quizId as quizId,userId as userId,max(viewDate) as viewDate FROM QuestionnaireViewEvent
                WHERE quizId = :quizId AND user_id in (SELECT user.id from UserGroup where group = :quizGroup and role = :student)
                GROUP BY userId,quizId""")
        .setParameter("quizId", questionnaire.id)
        .setParameter("quizGroup", questionnaire.group)
        .setParameter("student", UserGroup.Role.STUDENT)
        .setResultTransformer(Transformers.aliasToBean(QuestionnaireViewEvent.class))
        .list()
    }

    def delete(Questionnaire questionnaire) {
        log.info("remove-quiz: $questionnaire.id")
        Questionnaire.withNewTransaction {
            QuestionnaireProblem.findAllByQuestionnaire(questionnaire)*.delete()
        }

        questionnaire.delete()

        !questionnaire.hasErrors()
    }

    def findRestrictions(long questionnaireId) {
        return QuizProblemRestriction.findAllByQuizId(questionnaireId)
    }

    def findRestrictions(long questionnaireId, long problemId) {
        QuizProblemRestriction.findAllByQuizIdAndProblemId(questionnaireId, problemId)
    }

    def findRestrictionsResult(long questionnaireId) {
        SubmissionRestrictionsEvaluation.findAllByQuizId(questionnaireId)
    }

    def list(Map params) {
        if (params.filter?.contains("OWN") && params.groups?.isEmpty()) {
            []
        } else {
            def resultList = Questionnaire.createCriteria().list([max: params.max, offset: params.offset], getCriteria(params))

            if (params.withScore) {
                params.questionnaires = resultList*.id
                def scores = findUserQuestionnaireScores(params.userId, params)

                resultList.each {
                    scores.each { score ->
                        if (score.qid == it.id) {
                            it.metaClass.currentUser = ["score": score.userscore]
                        }
                    }
                }
            }

            resultList
        }
    }

    def findByGroup(Long questionnaireId, Long groupId) {
        Questionnaire.createCriteria().get {
            createAlias('group', 'g')
            eq('id', questionnaireId)
            eq('g.id', groupId)
        }
    }

    def findAllByGroup(Group group, Map params) {
        params.group = group.id

        Questionnaire.createCriteria().list([max: params.max, offset: params.offset], getCriteria(params))
    }

    def findScores(Long quizId) {
        def scores = sessionFactory.currentSession.createSQLQuery(
                """select 
                    q.id as questionnaire_id,
                    qp.id as q_problem_id,
                    qp.problem_id as problem_id,
                    ug.user_id as user_id,
                    max(case when qup.penalty is null then 0 else qup.penalty end) as penalty,
                    max(case when qup.penalty is null and s.evaluation = 0 then qp.score else 0 end) as score,
                    max(case when qup.penalty is null and s.evaluation is not null and s.total_test_cases > 0 then (s.correct_test_cases / s.total_test_cases\\:\\:real) * qp.score else 0 end) as partial_score,
                    min(case when sre is not null  and s.evaluation = 0 then sre.wrong_restriction_count else null end) as restriction_error_count,
                    max(qpr.penalty) as restriction_penalty,
                    sum(case when s.submission_date < q.end_date then 1 else 0 end) as submission_count
                from user_group ug
                inner join questionnaire q on (q.group_id = ug.group_id)
                inner join questionnaire_problem qp on (qp.questionnaire_id = q.id)
                left outer join questionnaire_user_penalty qup on (qup.questionnaire_problem_id = qp.id and qup.user_id = ug.user_id)
                left outer join submission s on (s.user_id = ug.user_id and s.problem_id = qp.problem_id and s.submission_date <= q.end_date)
                left outer join submission_restrictions_evaluation sre on s.id = sre.submission_id
                LEFT OUTER JOIN quiz_problem_restriction qpr on q.id= qpr.quiz_id and qp.problem_id = qpr.problem_id
                where 1 = 1
                and ug.role = 0
                and q.id = :quizz
                group by q.id, qp.id, qp.problem_id, ug.user_id""")
                .setParameter("quizz", quizId)
                .list()

        def result = []

        scores.each {
            result.add([
                    "questionnaireId": it[0],
                    "questionnaireProblemId": it[1],
                    "problemId"             : it[2],
                    "userId"                : it[3],
                    "penalty"               : it[4],
                    "score"                 : it[5],
                    "partialScore"          : it[6],
                    "restrictionErrorCount" : it[7],
                    "restrictionPenalty"    : it[8],
                    "submissionCount"    : it[9]
            ])
        }

        return result
    }

    def findUserProblemScores(Questionnaire questionnaire, Long userId) {
        def sql = """SELECT q.id as qid,
							sum(case when s.submission_date < q.end_date then 1 else 0 end) as submission_count,
							max(qp.problem_id) as problem_id,
							max(case when s.evaluation = 0 and s.submission_date < q.end_date then qp.score else 0 end) as user_score,
							max(case when s.submission_date < q.end_date and s.total_test_cases > 0  then ((s.correct_test_cases / s.total_test_cases\\:\\:real) * qp.score) else 0 end) as partial_score,
							max(qp.score) as problem_score,
							max(case when qup.id > 0 then qup.penalty else -1 end) as penalty,
							min(case when sre is not null  and s.evaluation = 0 then sre.wrong_restriction_count else null end) as restriction_error_count,
							max(qpr.penalty) as restriction_penalty
					FROM questionnaire q
					JOIN questionnaire_problem qp ON q.id = qp.questionnaire_id
					JOIN problem p ON p.id = qp.problem_id
					FULL JOIN questionnaire_user_penalty qup on qup.questionnaire_problem_id = qp.id and qup.user_id = :userId
					FULL JOIN submission s on s.problem_id = qp.problem_id and s.user_id = :userId
					LEFT OUTER JOIN submission_restrictions_evaluation sre on s.id = sre.submission_id
					LEFT OUTER JOIN quiz_problem_restriction qpr on q.id = qpr.quiz_id and qp.problem_id = qpr.problem_id
					WHERE q.id = :questionnaireId
					AND p.status = 1
					GROUP BY q.id,qp.problem_id
					order by user_score"""

        def scores = sessionFactory.currentSession.createSQLQuery(sql)
                .setParameter("questionnaireId", questionnaire.id)
                .setParameter("userId", userId)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list()

        return scores
    }

	/**
	 * Encontra as notas dos questionários do usuário em aberto até o maxDate
	 */
	def findUserScores(Long userId, Date maxDate, params) {
		params.maxDate = maxDate
		return findUserQuestionnaireScores(userId, params)
	}

	private def findUserQuestionnaireScores(Long userId, params) {

		String sql = """SELECT 	qid,
                                bool_and(calcPartial)                                               as calcPartial,
								max(enddate) 														as endDate,
								max(qname) 															as title,
								sum(case when penalty >= 0 then penalty else score end) 			as userScore,
								max(qscore) 														as quizzScore,
								max(partialScore) 													as partialScore,
								sum(case when penalty >= 0 then 1 else 0 end) 						as penalties
						FROM
						(SELECT q.id as qid,
						            q.partial_score                                                        as calcPartial,
									max(q.end_date) 												      as enddate,
									max(q.title) 													      as qname,
									max(case when s.id > 0 AND s.evaluation = 0 then qp.score else 0 end) as score,
									max(case when s.total_test_cases > 0 then (s.correct_test_cases / s.total_test_cases\\:\\:real) * qp.score else 0 end) as partialScore,
									max(q.score) 													      as qscore,
									max(qp.score) 													      as qpscore,
									max(case when qup.penalty is null then -1 else qup.penalty end)       as penalty
								FROM questionnaire q
								JOIN questionnaire_problem qp ON q.id = qp.questionnaire_id
								JOIN problem p on p.id = qp.problem_id
								FULL JOIN questionnaire_user_penalty qup on qup.questionnaire_problem_id = qp.id and qup.user_id = :user
								FULL JOIN submission s
									ON s.problem_id = qp.problem_id and s.user_id = :user and s.submission_date < q.end_date
								WHERE
								group_id IN (SELECT group_id FROM user_group WHERE user_id = :user)
								AND 'questionnaire_condition'
								AND p.status = 1
								GROUP BY q.id,qp.problem_id) as qs
						GROUP BY qid
						ORDER BY enddate DESC"""

        def sqlQuery

        if (params.maxDate) {
            sql = sql.replace("'questionnaire_condition'", "q.end_date > :max_date")
            sqlQuery = sessionFactory.currentSession.createSQLQuery(sql).setParameter("max_date", params.maxDate)
        } else if (params.questionnaires) {
            sql = sql.replace("'questionnaire_condition'", "q.id in (:qids)")
            sqlQuery = sessionFactory.currentSession.createSQLQuery(sql).setParameterList("qids", params.questionnaires)
        } else {
            return []
        }

        def scores = sqlQuery
                .setParameter("user", userId)
                .setMaxResults(params.max ? params.max : Integer.MAX_VALUE)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list()

        return scores
    }

    def exportQuizesToExcel(Long groupId, OutputStream out) {
        def scores = sessionFactory.currentSession.createSQLQuery("""SELECT
                                    q.id as questionnaire_id,
                                    q.title as quiz_name,
                                    q.partial_score as is_partial,
                                    q.score as quiz_score,
                                    qp.id as q_problem_id,
                                    qp.problem_id as problem_id,
                                    u.id as user_id,
                                    u.name as user_name,
                                    max(case when qup.penalty is null then -1 else qup.penalty end) as penalty,
                                    max(case when qup.penalty is null and s.evaluation = 0 then qp.score else 0 end) as score,
                                    max(case when qup.penalty is null and s.evaluation is not null and s.total_test_cases > 0 then (s.correct_test_cases / s.total_test_cases\\:\\:real) * qp.score else 0 end) as partial_score,
                                    min(case when sre is not null  and s.evaluation = 0 then sre.wrong_restriction_count else null end) as restriction_error_count,
                                    max(qpr.penalty) as restriction_penalty,
                                    sum(case when s.submission_date < q.end_date then 1 else 0 end) as submission_count
                                from user_group ug
                                join public.user u on ug.user_id = u.id
                                inner join questionnaire q on (q.group_id = ug.group_id)
                                inner join questionnaire_problem qp on (qp.questionnaire_id = q.id)
                                left outer join questionnaire_user_penalty qup on (qup.questionnaire_problem_id = qp.id and qup.user_id = ug.user_id)
                                left outer join submission s on (s.user_id = ug.user_id and s.problem_id = qp.problem_id and s.submission_date <= q.end_date)
                                left outer join submission_restrictions_evaluation sre on s.id = sre.submission_id
                                LEFT OUTER JOIN quiz_problem_restriction qpr on q.id= qpr.quiz_id and qp.problem_id = qpr.problem_id
                                where ug.group_id = :groupid
                                and ug.role = 0
                                group by q.id, qp.id, qp.problem_id, u.name,u.id
                                order by q.start_date,q.id,u.id,qp.problem_id""")
        .setParameter("groupid", groupId)
        .list();

        List<QuizReportExcelBeanV2> result = new ArrayList<>();

        scores.each {
            println(it)
            result.add(new QuizReportExcelBeanV2([
                quizId: it[0],
                quizName: it[1],
                partialQuiz: it[2],
                quizScore: it[3],
                quizProblemId: it[4],
                problemId: it[5],
                userId: it[6],
                userName: it[7],
                penalty: it[8],
                score: it[9],
                partialScore: it[10],
                restrictionPenalty: it[12],
                submissionCount: it[13],
                restrictionErrorCount: it[11]
                ]));
        }

        return new QuizesToExcel(out).build(result);

    }

    def findStudentsId(Long quizId) {
        sessionFactory.currentSession.createSQLQuery("""
                SELECT user_id as id FROM questionnaire q
                JOIN user_group ug on ug.group_id = q.group_id AND ug.role = :student WHERE q.id = :quiz""")
                .addScalar("id", StandardBasicTypes.LONG)
                .setParameter("quiz", quizId)
                .setParameter("student", UserGroup.Role.STUDENT.ordinal())
                .list()
    }

    def addProblem(Questionnaire questionnaire, Problem problem, Double score, Map params) {

        log.info("add-problem-to-quizz: { quiz: $questionnaire.id, problem: $problem.id }")
        def questionnaireProblem = QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)

        if (questionnaireProblem) {
            questionnaireProblem.score = score
        } else {
            questionnaireProblem = new QuestionnaireProblem(questionnaire: questionnaire, problem: problem, score: score)
        }

        questionnaireProblem.save()

        def totalScore = 0

        QuestionnaireProblem.findAllByQuestionnaire(questionnaire).each {
            totalScore += it.score
        }

        questionnaire.score = totalScore

        questionnaire.save()

        problemService.findAllByQuestionnaire(questionnaire.id, null)
    }

    def removeProblem(Questionnaire questionnaire, Problem problem, Map params) {

        log.info("remove-problem-from-quizz: { quiz: $questionnaire.id, problem: $problem.id }")

        def questionnaireProblem = QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)

        if (questionnaireProblem) {
            questionnaireProblem.delete()

            def totalScore = 0

            QuestionnaireProblem.findAllByQuestionnaire(questionnaire).each {
                totalScore += it.score
            }

            questionnaire.score = totalScore

            questionnaire.save()

            problemService.findAllByQuestionnaire(questionnaire.id, null)
        }
    }

    Closure getCriteria(Map params) {

        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()
        def now = new Date()

        return {
            and {

                or {
                    if (params.q) {
                        ilike("title", "%$params.q%")
                        ilike("description", "%$params.q%")
                    }
                }

                !params.group ?: eq("group", Group.load(params.group as Long))

                if (params.groups && !params.groups.empty) {
                    inList("group", params.groups)
                }

                if (params.filter && !params.filter.empty) {
                    if (params.filter.contains("OPEN")) {
                        le("startDate", now)
                        ge("endDate", now)
                    }

                    if (params.filter.contains("CLOSED")) {
                        le("endDate", now)
                    }

                    if (params.filter.contains("INTERSECT")) {
                        le("startDate", formatter.parseDateTime(params.endPeriod as String).toDate())
                        ge("endDate", formatter.parseDateTime(params.startPeriod as String).toDate())

                    }
                }

                !params.problemsGe ?: sizeGe("problems", 0)

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

                !params.score ?: eq("score", params.score as Double)
                !params.scoreGt ?: gt("score", params.scoreGt as Double)
                !params.scoreGe ?: ge("score", params.scoreGe as Double)
                !params.scoreLt ?: lt("score", params.scoreLt as Double)
                !params.scoreLe ?: le("score", params.scoreLe as Double)
                !params.scoreNe ?: ne("score", params.scoreNe as Double)
            }

            order(params.sort ?: "startDate", params.order ?: "desc")
            order(params.sort ?: "endDate", params.order ?: "asc")
        }
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)
        params.q = params.q ?: ""
        params.filter ? params.list("filter")*.asType(String) : []

        if (params.filter instanceof String) {
            params.filter = [params.filter]
        }

        params.filter = params.filter*.toUpperCase()


        return params
    }

    boolean isSortable(param) {
        [
                "id",
                "title",
                "description",
                "score",
                "startDate",
                "endDate",
                "dateCreated",
                "lastUpdated",
                "group"
        ].contains(param)
    }

}
