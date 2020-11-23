package com.thehuxley

import analisador.estatico.base.Constantes
import analisador.estatico.entidade.retorno.RetornoFuncao
import analisador.estatico.entidade.retorno.RetornoRestricoes
import analisador.estatico.ufs.AnalisadorEstatico
import analisador.estatico.ufs.ResultadoAnalise
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.primitives.Doubles
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import com.thehuxley.expcetion.DoNotLogException
import grails.converters.JSON
import grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.FetchMode
import org.hibernate.SessionFactory
import org.hibernate.sql.JoinType
import org.hibernate.transform.Transformers
import org.hibernate.type.StandardBasicTypes
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.springframework.context.i18n.LocaleContextHolder
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

@Consumer
class SubmissionService {

    def queueService
    SessionFactory sessionFactory
    def topCoderService

    final Cache<Long, Integer> submissionEvaluationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build()

    def get(Long submissionId) {
        Submission.get(submissionId)
    }

    def list(Map params) {
        def resultList

        if (params.analytics) {

        } else {

            if (params.group) {

                params.userIds = sessionFactory.currentSession.createSQLQuery(
                        'SELECT user_id from user_group WHERE group_id = :group and role = :student')
                .setParameter('group', params.group as Long)
                .setParameter('student', UserGroup.Role.STUDENT.ordinal())
                .list()

                if (params.userIds.isEmpty()) {
                    return [];
                } else {
                    params.userIds = params.userIds.collect { return it.longValue() }
                }

            }

            Submission.createCriteria().list([max: params.max, offset: params.offset],
                    getCriteria(params))
        }
    }

    def fastestSubmissions(long problem, String avatarUrl) {
        def resultList = sessionFactory.currentSession.createSQLQuery("""select *
                                                from (select
                                                  s.id as submission_id,
                                                  s.time,
                                                  s.evaluation,
                                                  lang.id as language_id,
                                                  lang.name as language_name,
                                                  u.id as user_id,
                                                  u.name as user_name,
                                                  u.avatar as user_avatar,
                                                  i.id as institution_id,
                                                  i.name as institution_name,
                                                  min(s.time) over (partition by user_id ) as mtime
                                                from Submission s
                                                join public.user u on u.id = s.user_id
                                                join institution i on i.id = u.institution_id
                                                join language lang on lang.id = s.language_id
                                                where problem_id = :problem and evaluation = 0) as s
                                                where s.time = mtime
                                                and not exists (select 1 from user_role where user_id = s.user_id and role_id in (1, 3))
                                                order by time asc""")
                .setParameter("problem", problem)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .setMaxResults(10)
                .list()

        def resultList2 = []

        resultList.each({
            resultList2.add([
                    id: it.submission_id,
                    time: it.time,
                    user: [
                        id: it.user_id,
                        name: it.user_name,
                        institution: [
                            id: it.institution_id,
                            name: it.institution_name
                        ],
                        avatar: "$avatarUrl/$it.user_avatar"

                    ],
                    language: [
                        id: it.language_id,
                        name: it.language_name
                    ],
                    evaluation: Submission.Evaluation.values()[it.evaluation]
            ])
        })

        resultList2
    }

    def findByGroup(Submission submission, Group group) {
        UserGroup.findAllByGroupAndUser(group, submission?.user)
    }

    def getEvaluation(long submissionId) {

        Integer evaluation = submissionEvaluationCache.getIfPresent(submissionId)

        if (evaluation == null) {

            evaluation = sessionFactory.currentSession.createSQLQuery("""select evaluation from submission where id = :id""")
                    .addScalar("evaluation", StandardBasicTypes.INTEGER)
                    .setLong("id", submissionId)
                    .uniqueResult();

            if (evaluation != null) {
                submissionEvaluationCache.put(submissionId, evaluation)
            }
        }

        return evaluation != null ? Submission.Evaluation.values()[evaluation] : null
    }

    def findAllByGroup(Group group, Map params) {

        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()

        def users = UserGroup.findAllByGroup(group).user

        params.users = users

        if (params.submissionDateGe) {
            def date = formatter.parseDateTime(params.submissionDateGe as String).toDate()

            if (date > group.endDate) {
                params.submissionDateGe = new DateTime(group.endDate).toString(formatter)
            }

            if (date < group.startDate) {
                params.submissionDateGe = new DateTime(group.startDate).toString(formatter)
            }

        } else {
            params.submissionDateGe = new DateTime(group.startDate).toString(formatter)
        }

        if (params.submissionDateLe) {
            def date = formatter.parseDateTime(params.submissionDateLe as String).toDate()

            if (date < group.startDate) {
                params.submissionDateLe = new DateTime(group.startDate).toString(formatter)
            }

            if (date > group.endDate) {
                params.submissionDateLe = new DateTime(group.endDate).toString(formatter)
            }

        } else {
            params.submissionDateLe = new DateTime(group.endDate).toString(formatter)
        }

        Submission.createCriteria().list([max: params.max, offset: params.offset],
                getCriteria(params))
    }

    def findByProblem(Submission submission, Problem problem) {
        submission.problem?.id == problem.id ? submission : null
    }

    def findAllByProblem(Problem problem, Map params) {
        params.problem = problem.id

        Submission.createCriteria().list([max: params.max, offset: params.offset],
                getCriteria(params))
    }

    def findByUser(Submission submission, User user) {
        submission.user?.id == user.id ? submission : null
    }

    def findAllByUser(User user, Map params) {
        params.user = user.id

        Submission.createCriteria().list([max: params.max, offset: params.offset],
                getCriteria(params))
    }

    def findByUserAndProblem(Submission submission, Problem problem, User user) {
        submission.user?.id == user.id && submission.problem?.id == problem.id ?
                submission : null
    }

    def findAllByUserAndProblem(Problem problem, User user, Map params) {
        params.user = user.id
        params.problem = problem.id

        Submission.createCriteria().list([max: params.max, offset: params.offset],
                getCriteria(params))
    }

    def findByUserAndQuestionnaireAndProblem(
            Submission submission,
            User user,
            Questionnaire questionnaire,
            Problem problem) {

        if (QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)) {
            return (submission.problem?.id == problem.id
                    && submission.user?.id == user.id
                    && submission.submissionDate <= questionnaire.endDate
            ) ? submission : null
        } else {
            return null
        }
    }

    def findAllByUserAndQuestionnaireAndProblem(User user, Questionnaire questionnaire, Problem problem, Map params) {

        if (QuestionnaireProblem.findByQuestionnaireAndProblem(questionnaire, problem)) {

            DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()

            params.user = user.id
            params.problem = problem.id
            params.submissionDateLe = new DateTime(questionnaire.endDate).toString(formatter)

            Submission.createCriteria().list([max: params.max, offset: params.offset],
                    getCriteria(params))
        } else {
            [resutCount: 0]
        }
    }

    Submission createChoiceSubmission(User user, Problem problem, Long[] choices) {

        if (problem.problemType == Problem.ProblemType.ALGORITHM || problem.problemType == Problem.ProblemType.FILL_THE_CODE) {
            throw new IllegalArgumentException("Problem not multiple choices")
        }

        def submission = Submission.findByUserAndProblem(user, problem)

        if (problem.quizOnly && submission != null) {
            throw new IllegalArgumentException("Problem already solved")
        }

        if (submission == null) {
            submission = new Submission(
                    user: user,
                    problem: problem,
                    tries: 1,
                    choices: choices
            )
        } else {
            submission.tries++
            submission.choices = choices
        }

        return evaluateMultipleChoice(problem, submission)
    }

    Submission createSubmission(User user, Problem problem, Language language, String originalFilename, InputStream inputStream) {

        if (problem.problemType != Problem.ProblemType.ALGORITHM && problem.problemType != Problem.ProblemType.FILL_THE_CODE) {
            throw new IllegalArgumentException("Problem not ALGORITHM")
        }

        if (language == null) {
            throw new IllegalArgumentException("Cannot create submission without language");
        }

        def tries = (Submission.countByUserAndProblem(user, problem) + 1)

        def submission = new Submission(
                language: language,
                user: user,
                problem: problem,
                tries: tries,
                originalFilename: originalFilename
        )

        def lastSubmissions = Submission.createCriteria().list {
            eq("user", user)
            order("submissionDate", "desc")
            maxResults(5)
        }

        def timeElapsed = new Date().time

        if (!lastSubmissions.isEmpty()) {
            timeElapsed = timeElapsed - lastSubmissions.first().submissionDate.time
        }

        try {
            submission.saveFile(inputStream)

            // verifica flood atack apenas em produção (estava dificultando os testes)
            if (!(queueService instanceof MockQueueService)) {
                if (Arrays.asList(submission.contentHash).containsAll(lastSubmissions*.contentHash)
                        && timeElapsed < 60000) {
                    throw new DoNotLogException("Flood attack.")
                }

                if (timeElapsed < 5000) {
                    throw new DoNotLogException("Flood attack.")
                }
            }

            submission.save(flush: true, failOnError: true)
        } catch (Exception e) {
            submission.deleteFile()
            throw e
        }

        def testCases = TestCase.findAllByProblem(submission.problem)

        submissionEvaluationCache.put(submission.id, submission.evaluation.ordinal())
        queueService.sendSubmissionToJudge(submission, testCases)

        submission
    }

    def save(Submission submission) {
        submission.save()
    }

    def reevaluate(Map params) {
        def submissions = Submission.createCriteria().list([readOnly: true], getCriteria(params))

        submissions.each() { Submission submission ->
            if (submission.language != null) {
                queueService.sendSubmissionToReevaluation(submission)
            }
        }

        submissions
    }

    def reevaluateMultipleChoiceProblem(Problem problem) {
        def submissions = Submission.findAllByProblem(problem)

        submissions.each {
            evaluateMultipleChoice(problem, it)
        }
    }

    def evaluateMultipleChoice(Problem problem, Submission submission) {

        def correct = true
        problem.choices.each { ProblemChoice choice ->

            if ((choice.correct && !submission.choices.contains(choice.id)) ||
                    (!choice.correct && submission.choices.contains(choice.id))) {

                correct = false
            }

        }

        submission.evaluation = correct ? Submission.Evaluation.CORRECT : Submission.Evaluation.WRONG_ANSWER

        submission.save(failOnError: true)
    }

    def findRestrictionEvaluation(long submissionId) {
        SubmissionRestrictionsEvaluation.findBySubmissionId(submissionId)
    }

    def evaluateRestrictions2(Submission submission) {

        QuizProblemRestriction quizRestriction = sessionFactory.currentSession.createSQLQuery("""
                SELECT qpr.quiz_id as "quizId", qpr.problem_id as "problemId", qpr.restrictions, qpr.penalty
                FROM quiz_problem_restriction qpr
                JOIN questionnaire q on q.id = qpr.quiz_id and q.end_date > :submissionDate
                JOIN user_group ug on ug.group_id = q.group_id and ug.user_id = :userId
                JOIN questionnaire_problem qp on qp.problem_id = :problemId and qp.questionnaire_id = q.id
                WHERE qpr.problem_id = :problemId""")
        .addScalar("quizId", StandardBasicTypes.LONG)
        .addScalar("problemId", StandardBasicTypes.LONG)
        .addScalar("penalty", StandardBasicTypes.FLOAT)
        .addScalar("restrictions", StandardBasicTypes.STRING)
        .setParameter("problemId", submission.problemId)
        .setParameter("submissionDate", submission.submissionDate)
        .setParameter("userId", submission.userId)
        .setMaxResults(1)
        .setResultTransformer(Transformers.aliasToBean(QuizProblemRestriction.class))
        .uniqueResult()

        evaluateRestriction(quizRestriction, submission.id, submission.userId, submission.language.name, submission.sourceCode)
    }

    @Selector("submission.evaluateRestrictions")
    def reevaluateRestrictions(QuizProblemRestriction quizRestriction) {
        def final toReevaluate = sessionFactory.currentSession.createSQLQuery("""SELECT s.id,s.user_id,l.name as language_name,s.filename
                                                        FROM submission_restrictions_evaluation sre
                                                        JOIN submission s on s.id = sre.submission_id
                                                        JOIN language l on l.id = s.language_id
                                                        WHERE sre.quiz_id = :quiz AND sre.problem_id = :problem""")
                        .addScalar("user_id", StandardBasicTypes.LONG)
                        .addScalar("id", StandardBasicTypes.LONG)
                        .addScalar("language_name", StandardBasicTypes.STRING)
                        .addScalar("filename", StandardBasicTypes.STRING)
                        .setParameter("quiz", quizRestriction.quizId)
                        .setParameter("problem", quizRestriction.problemId)
                        .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                        .list()

        log.info("$toReevaluate.size() restrictions reevaluations")

        toReevaluate.each({
            try {
                evaluateRestriction(quizRestriction, it.id, it.user_id, it.language_name, new Submission([ filename: it.filename]).getSourceCode())
            } catch(Exception ex) {
                log.error("unexpected-err", ex)
            }
        })


    }

    def evaluateRestriction(QuizProblemRestriction quizRestriction, Long submissionId, Long userId, String languageName, String sourceCode) {
        if (quizRestriction == null) {
            return;
        }

        log.info("evaluate-restriction: { quiz: $quizRestriction.quizId, problem: $quizRestriction.problemId, submission: $submissionId }")

        int language = -1;
        if (languageName.equals("Java")) {
            language = Constantes.JAVA
        } else if (languageName.equals("Python") || languageName.equals("Python3")) {
            language = Constantes.PYTHON
        }

        ResultadoAnalise analisar = new AnalisadorEstatico(language, sourceCode, quizRestriction.restrictions).analisar()


        def result = [:];
        def resultFuncoes = [];

        RetornoRestricoes retorno = analisar.retorno
        List<RetornoFuncao> funcoes = retorno.retornoFuncoes
        funcoes.eachWithIndex {RetornoFuncao f, int idx ->

            if (!f.existe || !f.retorno || !f.foiChamada || !f.recursao || !f.parametros || !f.nome) {
                resultFuncoes.add([
                        n: idx,
                        retorno: f.retorno,
                        foiChamada: f.foiChamada,
                        recursao: f.recursao,
                        nome: f.nome,
                        parametros: f.parametros,
                        existe: f.existe
                ])
            }
        }

        if (!resultFuncoes.isEmpty()) {
            result.retornoFuncoes = resultFuncoes
        }

        if (retorno.retornoBlocosVazios && !retorno.retornoBlocosVazios.atendeu) {
            result.retornoBlocosVazios = false;
        }

        if (retorno.retornoFor && !retorno.retornoFor.atendeu) {
            result.retornoFor = retorno.retornoFor.quantidade;
        }

        if (retorno.retornoDicionario && !retorno.retornoDicionario.atendeu) {
            result.retornoDicionario = [
                    variaveis: retorno.retornoDicionario.variaveis,
                    varNaoUsada: retorno.retornoDicionario.varNaoUsada
            ]
        }

        if (retorno.retornoIf && !retorno.retornoIf.atendeu) {
            result.retornoIf = retorno.retornoIf.quantidade;
        }

        if (retorno.retornoWhile && !retorno.retornoWhile.atendeu) {
            result.retornoWhile = retorno.retornoWhile.quantidade;
        }

        if (retorno.retornoLista && !retorno.retornoLista.atendeu) {
            result.retornoLista = [
                    variaveis: retorno.retornoLista.variaveis,
                    varNaoUsada: retorno.retornoLista.varNaoUsada
            ]
        }

        if (retorno.retornoTupla && !retorno.retornoTupla.atendeu) {
            result.retornoTupla = [
                    variaveis: retorno.retornoTupla.variaveis,
                    varNaoUsada: retorno.retornoTupla.varNaoUsada
            ]
        }

        def eval = SubmissionRestrictionsEvaluation.findBySubmissionId(submissionId)

        if (eval) {
            eval.restrictionCount = analisar.qtdRestricoes
            eval.wrongRestrictionCount = analisar.qtdRestricoesInvalidas
            eval.result = (result as JSON).toString()

            log.info("update-sre: { subId: $submissionId, count: $eval.restrictionCount, errors: $eval.wrongRestrictionCount } ")
            eval.save()
        } else {
            def sre = new SubmissionRestrictionsEvaluation([
                    userId: userId,
                    quizId: quizRestriction.quizId,
                    submissionId: submissionId,
                    problemId: quizRestriction.problemId,
                    restrictionCount: analisar.qtdRestricoes,
                    wrongRestrictionCount: analisar.qtdRestricoesInvalidas,
                    result: (result as JSON).toString()
            ])

            log.info("save-new-sre: { subId: $submissionId, count: $sre.restrictionCount, errors: $sre.wrongRestrictionCount } ")
            sre.save()
        }
    }

    def generateUserStats(params) {
        // TODO poderia eliminar dois JOINs se vier params.excludeTopics em caso de não precisar das estatísticas
        // de tópicos (como no caso de estatística de grupo

        String baseQuery = """select s.evaluation as evaluation,date_trunc('day', s.submission_date),s.user_id,s.problem_id,
                                     max(pi18n.name) as pName,max(p.nd) as nd,
                                     count(distinct(s.id)),array_to_string(array_agg(distinct(ti18n.name)), ';'),s.language_id
                                from submission s
                                join problem p on p.id = s.problem_id and p.status = 1
                                join problem_i18n pi18n on pi18n.problem_id = p.id
                                full join topic_problems tp on tp.problem_id = p.id
                                full join topic t on t.id = tp.topic_id
                                full join topic_i18n ti18n on ti18n.topic_id = t.id and ti18n.locale = :locale"""

        String userFilter = " WHERE s.user_id = :user"
        String userFilterByGroup = " WHERE s.user_id in (select user_id from user_group where group_id = :group and role = 0)"
        String presenceFilter = " AND s.user_id in (select user_id from questionnaire_view_event where quiz_id = :quizId)"
        String dateFilter = " and s.submission_date between :start AND :end"
        String questionnaireFilter = " AND s.problem_id in (select problem_id from questionnaire_problem where questionnaire_id = :quizId)"

        String groupBy = " group by s.user_id,date_trunc('day', s.submission_date),s.evaluation,s.problem_id,s.language_id order by count desc"

        def submissions = []

        def userMap = [:]

        if (params.groupId != null) {
            // Estatísticas de Grupo
            def group = Group.load(Long.parseLong(params.groupId))

            submissions = sessionFactory.currentSession.createSQLQuery(baseQuery + userFilterByGroup + dateFilter + groupBy)
                    .setParameter("group", group.id)
                    .setParameter("start", group.startDate)
                    .setParameter("end", group.endDate)
                    .setParameter("locale", LocaleContextHolder.locale.toString())
                    .list()

        } else if (params.userId != null) {

            // Estatísticas de Usuário
            userMap[params.userId] = new SubmissionStats(params.userId, params)
            submissions = sessionFactory.currentSession.createSQLQuery(baseQuery + userFilter + groupBy)
                    .setParameter("user", params.userId)
                    .setParameter("locale", LocaleContextHolder.locale.toString())
                    .list()
        } else if (params.quizId != null) {
            // Estatísticas de Questionário
            def quizz = Questionnaire.get(Long.parseLong(params.quizId))
            def userQuizFilter = userFilterByGroup

            if (params.filter == 'presence') {
                userQuizFilter += presenceFilter
            }

            submissions = sessionFactory.currentSession.createSQLQuery(baseQuery + userQuizFilter + dateFilter + questionnaireFilter + groupBy)
                    .setParameter("group", quizz.groupId)
                    .setParameter("quizId", quizz.id)
                    .setParameter("start", quizz.startDate)
                    .setParameter("end", quizz.endDate)
                    .setParameter("locale", LocaleContextHolder.locale.toString())
                    .list()
        }

        submissions.each { sub ->

            def eval = sub[0]
            Timestamp date = sub[1]
            long user = (long) sub[2]
            long problem = (long) sub[3]
            int nd = (int) sub[5]
            Long languageId = (Long) sub[8]

            SubmissionStats userStats = userMap[user]

            if (userStats == null) {
                userStats = new SubmissionStats(user, params)
                userMap[user] = userStats
            }

            Submission.Evaluation evaluation = Submission.Evaluation.EMPTY_ANSWER

            int i = 0
            for (Submission.Evaluation e : Submission.Evaluation.values()) {
                if (i == eval) {
                    evaluation = e
                }
                i++
            }

            userStats.add(user, new Date(date.time), problem, evaluation, nd, sub[6], sub[7].split(';'), languageId)

        }

        return userMap
    }

    def generateThermometerStats(params) {
        // TODO poderia eliminar dois JOINs se vier params.excludeTopics em caso de não precisar das estatísticas
        // de tópicos (como no caso de estatística de grupo

        String baseQuery = """select date_trunc('day', s.submission_date),
\ts.user_id,
\tcount(distinct(s.id))
\tfrom submission s
\tWHERE s.user_id in (select user_id from user_group where group_id = :group and role = 0)
\tand s.submission_date between :start AND :end
\tand s.submission_date > current_date - interval '7 days'\t
\tgroup by s.user_id,date_trunc('day', s.submission_date),s.evaluation,s.problem_id,s.language_id order by date_trunc('day',s.submission_date) desc"""

        def submissions

        def userMap = [:]

        def group = Group.load(Long.parseLong(params.groupId))

        submissions = sessionFactory.currentSession.createSQLQuery(baseQuery)
                .setParameter("group", group.id)
                .setParameter("start", group.startDate)
                .setParameter("end", group.endDate)
                .list()

        submissions.each { sub ->
            Timestamp date = sub[0]
            long user = (long) sub[1]

            SubmissionStats userStats = userMap[user]

            if (userStats == null) {
                userStats = new SubmissionStats(user, params)
                userMap[user] = userStats
            }

            userStats.add(user, new Date(date.time), sub[2])

        }

        def jsonObject = JSON.parse((userMap as JSON).toString())
        def totalMembers = sessionFactory.currentSession.createSQLQuery("select count(*) from public.user_group where group_id= :group and role=0;")
                .setParameter("group", group.id)
                .uniqueResult()
        jsonObject.putAt('totalMembers', totalMembers)

        return jsonObject
    }

    def generateProblemStats(Long problemId, params) {
        def byUserSQL = """
                SELECT s.evaluation AS evaluation,date_trunc('day', s.submission_date),s.user_id,count(distinct(s.id)),s.language_id
                FROM submission s
                WHERE s.problem_id = :problem
                GROUP BY s.user_id,date_trunc('day', s.submission_date),s.evaluation,s.language_id
                ORDER BY count DESC"""

        def byLanguageAndEvaluationSql = """SELECT s.evaluation,s.language_id,COUNT(*)
                                        FROM submission s
                                        WHERE s.problem_id = :problem
                                        GROUP BY s.evaluation,s.language_id"""

        def byUserSubmissionCountSql = """SELECT s.user_id,count(*) as submission_count,sum(case when s.evaluation = 0 then 1 else 0 end) as correct_submission_count
                                        FROM submission s
                                        WHERE s.problem_id = :problem
                                        GROUP BY s.user_id"""

        def byLanguageAndEvaluation = sessionFactory.currentSession.createSQLQuery(byLanguageAndEvaluationSql)
                .setParameter("problem", problemId)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list()

        def byUserSubmissionCount = sessionFactory.currentSession.createSQLQuery(byUserSubmissionCountSql)
                .setParameter("problem", problemId)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list()

        return [
                byLanguageAndEvaluation: byLanguageAndEvaluation,
                byUserSubmissionCount  : byUserSubmissionCount
        ]
    }

    void update(Submission submission) {
        submission.save()
    }

    @Selector("submission.evaluateProblemSubmissions")
    def evaluateProblemSubmissions(Long problemId) {
        try {

            log.info('reevaluating submissions of problem: ' + problemId)

            def updateEvaluationsSQL = """
                UPDATE submission as s SET
                    total_test_cases = sub.total,
                    correct_test_cases = sub.correct,
                    time = sub.worst_time,
                    evaluation = sub.eval
                FROM (
                  SELECT id,
                        (test_count - correct_examples)                   AS total,
                        ((test_count - correct_examples) - wrong)         AS correct,
                        (CASE WHEN wrong = 0 THEN 0 ELSE worst_eval END)  AS eval,
                        worst_time                                        AS worst_time
                    FROM (
                      SELECT
                        s.id AS id,
                        COUNT(tc) AS test_count,
                        min(CASE WHEN tc.evaluation = 0 THEN 20 ELSE tc.evaluation END) AS worst_eval,
                        max(tc.time) AS worst_time,
                        sum(CASE WHEN tc.evaluation = 0 AND t.example is true THEN 1 ELSE 0 END) AS correct_examples,
                        sum(CASE WHEN tc.evaluation != 0 THEN 1 ELSE 0 END) AS wrong
                      FROM submission s
                      JOIN test_case_evaluation tc on tc.submission_id = s.id
                      JOIN test_case t on tc.test_case_id = t.id
                      WHERE s.problem_id = :problem
                      GROUP BY s.id) AS s) AS sub
                WHERE s.id = sub.id"""

            sessionFactory.currentSession
                    .createSQLQuery(updateEvaluationsSQL)
                    .setParameter("problem", problemId)
                    .executeUpdate();

        } catch (Exception ex) {
            log.error('unexpeted-err', ex)
        }
    }

    void evaluateSubmission(Submission submission, boolean isReevaluation) {

        def executionTimes = submission.testCaseEvaluations*.time

        def notCorrectEvaluations = submission.testCaseEvaluations.findAll { it.evaluation !=  Submission.Evaluation.CORRECT }

        if (submission.testCaseEvaluations.isEmpty()) {
            submission.evaluation = Submission.Evaluation.HUXLEY_ERROR
        } else if (notCorrectEvaluations == null || notCorrectEvaluations.isEmpty()) {
            submission.evaluation = Submission.Evaluation.CORRECT
        } else {
            submission.evaluation = notCorrectEvaluations.first().evaluation
        }

        if (submission.id) {
            submissionEvaluationCache.put(submission.id, submission.evaluation.ordinal())
        }

        if (executionTimes == null || executionTimes.isEmpty()) {
            submission.time = -1
        } else {
            submission.time = executionTimes.sort().last() // slowest test case execution time
        }

        if (submission.testCaseEvaluations.size() > 0) {
            def totalCorrectExamples = submission.testCaseEvaluations.findAll {
                it.evaluation ==  Submission.Evaluation.CORRECT && it.testCase.example
            }

            submission.totalTestCases = submission.testCaseEvaluations.size() - totalCorrectExamples.size()
            submission.correctTestCases = submission.totalTestCases - notCorrectEvaluations.size()
        }

        log.info "submission-evaluated: { id: ${submission.id}, eval: ${submission.evaluation.name()}, reevaluation: ${isReevaluation} }"

        submission.merge(flush: true, failOnError: true)

        topCoderService.queueToUpdate(submission.user.id)

        if (submission.evaluation == Submission.Evaluation.CORRECT) {
            try {
                evaluateRestrictions2(submission)
            } catch (Exception ex) {
                log.error("unexpected-error-eval-restrictions", ex)
            }
        }
    }

    Closure getCriteria(Map params) {

        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()

        return {

            createAlias('user.institution', 'userInst', JoinType.LEFT_OUTER_JOIN)
            createAlias('user.topCoder', 'userTopcoder', JoinType.LEFT_OUTER_JOIN)

            fetchMode('user', FetchMode.JOIN)
            fetchMode('parts', FetchMode.JOIN)
            fetchMode('userInst', FetchMode.SELECT)
            fetchMode('userTopcoder', FetchMode.SELECT)

            and {
                !params.submission ?: eq("id", params.submission as Long)
                !params.problem ?: eq("problem", Problem.load(params.problem as Long))
                !params.problems ?: inList("problem", params.problems)
                !params.language ?: eq("language", Language.load(params.language as Long))
                !params.user ?: eq("user", User.load(params.user as Long))
                !params.users ?: inList("user", params.users)
                !params.userIds ?: inList("user.id", params.userIds)
                ''

                !params.tries ?: eq("tries", params.tries as Integer)
                !params.triesGt ?: gt("tries", params.triesGt as Integer)
                !params.triesGe ?: ge("tries", params.triesGe as Integer)
                !params.triesLt ?: lt("tries", params.triesLt as Integer)
                !params.triesLe ?: le("tries", params.triesLe as Integer)
                !params.triesNe ?: ne("tries", params.triesNe as Integer)

                !params.time ?: eq("time", params.time as Double)
                !params.timeGt ?: gt("time", params.timeGt as Double)
                !params.timeGe ?: ge("time", params.timeGe as Double)
                !params.timeLt ?: lt("time", params.timeLt as Double)
                !params.timeLe ?: le("time", params.timeLe as Double)
                !params.timeNe ?: ne("time", params.timeNe as Double)

                !params.submissionDate ?: eq("submissionDate",
                        formatter.parseDateTime(params.submissionDate as String).toDate())
                !params.submissionDateGt ?: gt("submissionDate",
                        formatter.parseDateTime(params.submissionDateGt as String).toDate())
                !params.submissionDateGe ?: ge("submissionDate",
                        formatter.parseDateTime(params.submissionDateGe as String).toDate())
                !params.submissionDateLt ?: lt("submissionDate",
                        formatter.parseDateTime(params.submissionDateLt as String).toDate())
                !params.submissionDateLe ?: le("submissionDate",
                        formatter.parseDateTime(params.submissionDateLe as String).toDate())
                !params.submissionDateNe ?: ne("submissionDate",
                        formatter.parseDateTime(params.submissionDateNe as String).toDate())


                !params.inEvaluations ?: inList("evaluation", params.inEvaluations)

                if (params.isNotEvaluations && !params.isNotEvaluations.empty) {
                    not {
                        inList("evaluation", params.isNotEvaluations)
                    }
                }
            }



            order(params.sort ?: "submissionDate", params.order ?: "desc")
        }
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)
        params.order = params.order ?: "desc"
        params.sort = params.sort ?: "submissionDate"
        params.inEvaluations = params.list("evaluations").collect { Submission.Evaluation.valueOf(it as String) }
        params.isNotEvaluations = params.list("excludeEvaluations").collect {
            Submission.Evaluation.valueOf(it as String)
        }

        params.problem = params.problem ? Longs.tryParse(params.problem) : null
        params.language = params.language ? Longs.tryParse(params.language) : null
        params.user = params.user && params.user instanceof String ? Longs.tryParse(params.user) : params.user as Long
        params.tries = params.tries != null ? Ints.tryParse(params.tries) : null
        params.time = params.time != null ? Doubles.tryParse(params.time) : null

        return params
    }

    boolean isSortable(param) {
        [
                "id",
                "time",
                "tries",
                "comment",
                "submissionDate",
                "evaluation",
                "user",
                "problem",
                "language"
        ].contains(param)
    }

    private def updateTopicCount(Map topicMap, key) {
        Integer count = topicMap[key]
        if (count == null) {
            count = 1
        } else {
            count++
        }
        topicMap[key] = count
    }

    private def Map mapCountByEvaluation() {
        Map map = [:]
        Submission.Evaluation.values().each {
            map[it as String] = 0
        }
        map["TOTAL"] = 0
        return map
    }

    /** Apenas usado no MockQueue para testes */
    def updateEvaluation(Submission sub) {
        submissionEvaluationCache.put(sub.id, sub.evaluation.ordinal())
    }

    def userCanAccessSubmission(Long user, Long user2) {
        return sessionFactory.currentSession.createSQLQuery("""select ug1.user_id from user_group ug1
                                                                join public.user_group ug2 on ug1.group_id = ug2.group_id
                                                                where (ug1.role = 1 or ug1.role = 2) and ug1.user_id = :user1
                                                                and ug2.user_id = :user2 ;""")
                .setParameter("user1", user)
                .setParameter("user2", user2)
                .list().size() > 0
    }

}