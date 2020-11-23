package com.thehuxley

import com.google.common.cache.CacheBuilder
import grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.Criteria
import org.hibernate.FetchMode
import org.hibernate.SessionFactory
import org.hibernate.sql.JoinType
import org.hibernate.transform.Transformers
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.crypto.codec.Hex
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile

import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ProblemService {

    /**
     * Por favor deixar o tipo da variável.
     * Ajuda tanto no auto-complete como para fazer syntax highlighting do SQL. Obrigado
     * **/
    SessionFactory sessionFactory

    def grailsApplication
    def springSecurityService

    def get(Long problemId, Problem.Status status) {
        Problem.createCriteria().get {
            eq('id', problemId)
            createAlias('userApproved.institution', 'approvedInst', JoinType.LEFT_OUTER_JOIN)
            createAlias('userApproved.topCoder', 'approvedTopcoder', JoinType.LEFT_OUTER_JOIN)
            createAlias('userSuggest.institution', 'suggestInst', JoinType.LEFT_OUTER_JOIN)
            createAlias('userSuggest.topCoder', 'suggestTopcoder', JoinType.LEFT_OUTER_JOIN)
            createAlias('topics.i18ns', 'ti18n', JoinType.LEFT_OUTER_JOIN)

            fetchMode('choices', FetchMode.JOIN)
            fetchMode('testCases', FetchMode.JOIN)
            fetchMode('userApproved', FetchMode.JOIN)
            fetchMode('topics', FetchMode.JOIN)
            fetchMode('approvedInst', FetchMode.SELECT)
            fetchMode('approvedTopcoder', FetchMode.SELECT)
            fetchMode('userSuggest', FetchMode.JOIN)
            fetchMode('suggestInst', FetchMode.SELECT)
            fetchMode('suggestTopcoder', FetchMode.SELECT)

            if (status) {
                eq('status', status)
            }
        }
    }

    def list(Map params, Problem.Status status) {
        params.inProblems = checkInProblems(params)
        if(params.inProblems == null) return [];
        Problem.createCriteria().list(params, getCriteria(params, status))
    }

    def bindEvaluations(User user, List<Problem> problems) {

        if (problems.isEmpty()) return;

        def result = sessionFactory.currentSession.createSQLQuery("""SELECT min(evaluation) AS evaluation, problem_id
                                                            FROM submission
                                                            WHERE problem_id in (:problems) AND user_id = :user
                                                            GROUP BY problem_id""")
                .setParameterList("problems", problems*.id)
                .setParameter("user", user.id)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list();



        problems.each { Problem problem ->
            problem.metaClass.currentUser = [ status: null ]

            result.each {
                if (it.problem_id == problem.id) {
                    problem.currentUser.status = Submission.Evaluation.values()[it['evaluation']].toString()
                }
            }

        }


    }

    def findByQuestionnaire(Long problem, Questionnaire questionnaire, Problem.Status status) {
        def questionnaireProblem = QuestionnaireProblem.findByProblemAndQuestionnaire(problem, questionnaire)

        if (questionnaireProblem && questionnaireProblem.problem.status == status) {
            questionnaireProblem.problem.metaClass.score = questionnaireProblem.score

            questionnaireProblem.problem
        }
    }

    def findAllByQuestionnaire(long questionnaireId, Problem.Status status) {

        // Essa consulta era feita diretamente com um criteria no QuestionnaireProblem. Gerava mais de 100 consultas
        def result = sessionFactory.currentSession.createSQLQuery("""
                SELECT p.id as id,p.nd as nd,p18.name as name, p.status as status, p.problem_type as "problemType", qp.score as score, string_agg(t18.name, ';') as topics
                FROM questionnaire_problem qp
                    JOIN problem p on p.id = qp.problem_id and (p.status = :status OR :all_status)
                    JOIN problem_i18n p18 on p18.problem_id = p.id and p18.locale = :locale
                    LEFT OUTER JOIN topic_problems tp on tp.problem_id = p.id
                    LEFT OUTER JOIN topic t on t.id = tp.topic_id
                    LEFT OUTER JOIN topic_i18n t18 on t18.topic_id = t.id and t18.locale =  :locale
                WHERE qp.questionnaire_id = :quiz
                GROUP BY p.id,p.nd,qp.score,p18.name
                ORDER BY p.nd,p18.name ASC""")
        .setParameter("quiz", questionnaireId)
        .setParameter("locale", LocaleContextHolder.getLocale().toString())
        .setParameter("status", status ? status.ordinal() : 0)
        .setParameter("all_status", status == null)
        .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        .list()

        result.each({ r ->
            r.topics = r.topics ? r.topics.split(";").collect({
                return [ name: it ]
            }) : null

            r.status = Problem.Status.values()[r.status]
            r.problemType = Problem.ProblemType.values()[r.problemType].toString()
        })

        return result

    }

    def countProblemStatus(User user) {
        return sessionFactory.currentSession.createSQLQuery(
                "SELECT status,COUNT(*) FROM problem p WHERE user_suggest_id = :user GROUP BY status ORDER BY status ASC")
        .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        .setParameter("user", user.id)
        .list()
    }

    def countProblemsByLanguageAndType() {
        def result = sessionFactory.currentSession.createSQLQuery(
                """select count(p.id), problem_type, pi.locale
                    from public.problem p
                    join problem_i18n pi on p.id = pi.problem_id
                    where status = 1
                    group by problem_type, pi.locale""")
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list()
        return result
    }


    def findByUser(Long problemId, User user, Problem.Status status) {
        def problem = get(problemId, status)
        def evaluations

        if(problem.problemType == Problem.ProblemType.ALGORITHM || problem.problemType == Problem.ProblemType.FILL_THE_CODE) {
            evaluations = Submission.createCriteria().list {
                projections {
                    groupProperty 'evaluation'
                }
                eq('user', user)
                eq('problem', problem)
                order('evaluation')
            }
        } else {
            evaluations = Submission.createCriteria().list {
                projections {
                    groupProperty 'evaluation'
                    groupProperty 'choices'
                }
                eq('user', user)
                eq('problem', problem)
                order('evaluation')
            }
        }

        problem.metaClass.currentUser = [status: !evaluations.isEmpty() ? evaluations.first() : null]

        problem
    }

    def findAllByUser(User user, Map params, Problem.Status status) {
        params.user = user.id
        params.inProblems = checkInProblems(params)

        def problems = Problem.createCriteria().list([max: params.max, offset: params.offset], getCriteria(params, status))

        problems.each { problem ->
            def evaluations = Submission.createCriteria().list {
                projections {
                    groupProperty 'evaluation'
                }
                eq('user', user)
                eq('problem', problem)
                order('evaluation')
            }

            problem.metaClass.currentUser = [status: !evaluations.isEmpty() ? evaluations.first() : null]
        }
    }

    def checkInProblems(Map params) {
        def inProblems = []
        def user = null

        if (params.user) {
            user = User.load(params.user as Long)
        }

        if (user && params.attempted) {
            inProblems = Submission.createCriteria().list() {
                eq("user", user)
                projections {
                    distinct("problem")
                }
            }.id
        } else if (user && params.suggested) {
            inProblems = Problem.findAllByUserSuggest(user)?.id
        } else if (user && params.approved) {
            inProblems = Problem.findAllByUserApproved(user)?.id
        }


        if (params.topics) {
            inProblems = Problem.createCriteria().list() {
                topics {
                    inList("id", params.topics as List<Long>)
                }

                if (inProblems) {
                    inList("id", inProblems)
                }
            }?.id
        }

        if (params.group) {
            def users = UserGroup.findAllByGroupAndRole(Group.load(params.group as Long), UserGroup.Role.STUDENT).user
            inProblems = inProblems ?: Problem.list().id
            inProblems -= Submission.createCriteria().list() {
                and {
                    if (users && !users.empty) {
                        inList('user', users)
                    }
                    eq('evaluation', Submission.Evaluation.CORRECT)
                }

                projections {
                    distinct("problem")
                }
            }?.id
        }

        if (params.excludeTopics) {
            inProblems = inProblems ?: Problem.list().id
            inProblems -= Problem.createCriteria().list() {
                topics {
                    inList("id", params.excludeTopics as List<Long>)
                }
            }?.id
        }

        if (user && params.excludeCorrect) {
            inProblems = inProblems ?: Problem.list().id
            inProblems -= Submission.createCriteria().list() {
                eq("user", user)
                eq("evaluation", Submission.Evaluation.CORRECT)
                projections {
                    distinct("problem")
                }
            }?.id
        }

        if (params.exclude && !params.exclude.empty) {
            inProblems = inProblems ?: Problem.list().id
            inProblems -= Submission.createCriteria().list() {
                inList("id", params.exclude as List<Long>)
            }?.id
        }

        if ((params.topics
                || params.excludeTopics
                || (user && params.attempted)
                || (user && params.suggested)
                || (user && params.approved)
                || (user && params.excludeCorrects)) && inProblems.isEmpty()) return null

        return inProblems
    }



    def save(Problem problem) {

        log.info(problem.id ? "problem-update: $problem.id" : ("problem-create: " + problem.name()))

        problem.lastUserUpdate = new Date()

        problem.save(failOnError: true)
    }

    def countOpenUserQuizzesWithProblem(Long problemId, Long userId, Date date) {
        String sql = """
                SELECT count(*) FROM user_group ug
                JOIN questionnaire q ON q.group_id = ug.group_id AND q.start_date <= :date
                JOIN questionnaire_problem qp ON q.id = qp.questionnaire_id
                WHERE ug.user_id = :user AND qp.problem_id = :problem"""

        def count = sessionFactory.currentSession.createSQLQuery(sql)
                .setParameter("date", date)
                .setParameter("user", userId)
                .setParameter("problem", problemId)
                .uniqueResult().longValue();

        return count;
    }

    def getNotifications(long problemId) {
        return sessionFactory.currentSession.createSQLQuery("""
            SELECT
              string_agg(u.id\\:\\:text,';') as "userIds",
              q.id as "quizId",
              q.title as "quizTitle",
              g.id as "groupId",
              g.name as "groupName"
            FROM PUBLIC.group g
              JOIN user_group ug on ug.group_id = g.id and ug.role != 0
              JOIN public.user u on u.id = ug.user_id
              JOIN questionnaire q on q.group_id = g.id
              JOIN questionnaire_problem qp on qp.questionnaire_id = q.id and qp.problem_id = :problem
            WHERE g.end_date > now()
            GROUP BY q.id,q.title,g.id,g.name""")
        .setParameter("problem", problemId)
        .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        .list()
    }

    Closure getCriteria(Map params, Problem.Status status) {

        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()
        def currentUser = springSecurityService.currentUser as User ?: null

        return {
            createAlias('i18ns', 'i18ns')
            eq('i18ns.locale', LocaleContextHolder.getLocale().toString())
            and {
                if (params.q) {
                    or {
                        ilike("i18ns.name", "%$params.q%")
                        try {
                            eq("id", params.q as Long)
                        } catch (NumberFormatException e) { }
                    }
                }

                if (status) {
                    eq("status", status)

                    if (status != Problem.Status.ACCEPTED && !currentUser.getAuthorities().authority.contains('ROLE_ADMIN')) {
                        eq("userSuggest", currentUser)
                    }
                } else {
                    or {
                        eq("status", Problem.Status.ACCEPTED)
                        eq("userSuggest", currentUser)
                    }
                    ne("status", Problem.Status.REJECTED)
                }

                if (params.inProblems && !params.inProblems.empty) {
                    inList("id", params.inProblems)
                }

                !params.level ?: eq("level", params.level as Integer)
                !params.levelGt ?: gt("level", params.levelGt as Integer)
                !params.levelGe ?: ge("level", params.levelGe as Integer)
                !params.levelLt ?: lt("level", params.levelLt as Integer)
                !params.levelLe ?: le("level", params.levelLe as Integer)
                !params.levelNe ?: ne("level", params.levelNe as Integer)

                !params.timeLimit ?: eq("timeLimit", params.timeLimit as Integer)
                !params.timeLimitGt ?: gt("timeLimit", params.timeLimitGt as Integer)
                !params.timeLimitGe ?: ge("timeLimit", params.timeLimitGe as Integer)
                !params.timeLimitLt ?: lt("timeLimit", params.timeLimitLt as Integer)
                !params.timeLimitLe ?: le("timeLimit", params.timeLimitLe as Integer)
                !params.timeLimitNe ?: ne("timeLimit", params.timeLimitNe as Integer)

                !params.nd ?: eq("nd", params.nd as Double)
                !params.ndGt ?: gt("nd", params.ndGt as Double)
                !params.ndGe ?: ge("nd", params.ndGe as Double)
                !params.ndLt ?: lt("nd", params.ndLt as Double)
                !params.ndLe ?: le("nd", params.ndLe as Double)
                !params.ndNe ?: ne("nd", params.ndNe as Double)

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

                (params.quizOnly == null) ?: eq("quizOnly", params.quizOnly as boolean)

                if (params.problemType) {
                    if ("CHOICES".equals(params.problemType)) {
                        or {
                            eq("problemType", Problem.ProblemType.MULTIPLE_CHOICE)
                            eq("problemType", Problem.ProblemType.SINGLE_CHOICE)
                            eq("problemType", Problem.ProblemType.TRUE_OR_FALSE)
                        }
                    } else {
                        eq("problemType", Problem.ProblemType.valueOf(params.problemType))
                    }
                }

            }
            order(params.sort ?: "nd", params.order ?: "asc")
            order("i18ns.name", "asc")
        }
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        try {
            params.max = Math.min(params.int("max", 0) ?: 10, 100)
            params.offset = params.int("offset", 0)
            params.excludeCorrect = params.boolean("excludeCorrect", false)
            params.attempted = params.filter?.toLowerCase() == "attempted"
            params.suggested = params.filter?.toLowerCase() == "suggested"
            params.approved = params.filter?.toLowerCase() == "approved"
            params.group = params.long("group")
            params.q = params.q ?: ""
            params.order = params.order ?: "asc"
            params.sort = params.sort ?: "nd"
            params.topics = params.topics ? params.list("topics")*.asType(Long) : []
            params.excludeTopics = params.excludeTopics ? params.list("excludeTopics")*.asType(Long) : []
            params.exclude = params.exclude ? params.list("exclude")*.asType(Long) : []
            params.level = params.int("level")
            params.levelGt = params.int("levelGt")
            params.levelGe = params.int("levelGe")
            params.levelLt = params.int("levelLt")
            params.levelLe = params.int("levelLe")
            params.levelNe = params.int("levelNe")
            params.nd = params.double("nd")
            params.ndGt = params.double("ndGt")
            params.ndGe = params.double("ndGe")
            params.ndLt = params.double("ndLt")
            params.ndLe = params.double("ndLe")
            params.ndNe = params.double("ndNe")

            return params
        } catch (Exception e) {
            return null
        }
    }

    boolean isSortable(param) {
        [
                "id",
                "name",
                "description",
                "inputFormat",
                "outputFormat",
                "source",
                "level",
                "timeLimit",
                "nd",
                "dateCreated",
                "lastUpdated",
                "status",
                "userApproved",
                "userSuggest",
        ].contains(param)
    }

    def uploadImage(MultipartFile file) {
        String path = grailsApplication.config.huxleyFileSystem.problem.images.dir + System.getProperty("file.separator")

        File dir = new File(path)
        dir.mkdirs()

        def originalFilename = file.originalFilename
        def index = originalFilename.lastIndexOf('.')
        def extension = ""
        if ((index > 0) && (originalFilename.size() > index)) {
            extension = originalFilename.substring(index - 1)
        }

        def filename = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest(file.bytes))) + extension
        def destFile = new File(dir, filename)

        file.transferTo(destFile)

        return destFile
    }

    def getImage(String key) {
        String path = grailsApplication.config.huxleyFileSystem.problem.images.dir + System.getProperty("file.separator") + key
        def originalFile = new File(path)

        return originalFile
    }

    def getTotalProblemsByTopics() {
        def totalByTopics = sessionFactory.currentSession.createSQLQuery("select ti18n.name,count(*) from problem p " +
                "full join public.topic_problems tp on tp.problem_id = p.id " +
                "full join topic t on tp.topic_id = t.id " +
                "join topic_i18n ti18n on (ti18n.topic_id = t.id) " +
                "group by ti18n.name;").list()
        Map totalByTopicsMap = [:]
        totalByTopics.each {
            def key = (it[0] as String) ? it[0] as String : ""
            totalByTopicsMap[key] = it[1]
        }
        return totalByTopicsMap
    }

    def getTotalProblemsByNd() {
        def totalByNd = sessionFactory.currentSession.createSQLQuery("select nd, count(nd) from problem group by nd;").list()
        Map totalByNdMap = [:]
        totalByNd.each {
            def key = ((it[0] as Integer) as String) ? (it[0] as Integer) as String : ""
            totalByNdMap[key] = it[1]
        }
        return totalByNdMap
    }

    def updateRank() {
        sessionFactory.currentSession.createSQLQuery("""
                    UPDATE problem SET
                    user_rank = (SELECT avg(score) FROM user_problem_vote uv WHERE problem.id = uv.problem_id),
                    rank_votes = (SELECT count(*) FROM user_problem_vote uv WHERE problem.id = uv.problem_id)
                    WHERE EXISTS (SELECT 1 FROM user_problem_vote uv WHERE problem.id = uv.problem_id)""")
        .executeUpdate();
    }

}
