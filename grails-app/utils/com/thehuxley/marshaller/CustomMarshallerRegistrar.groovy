package com.thehuxley.marshaller

import com.thehuxley.*
import com.thehuxley.error.ErrorReason
import com.thehuxley.error.ErrorResponse
import grails.converters.JSON
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import javax.annotation.PostConstruct
import java.text.MessageFormat

class CustomMarshallerRegistrar {

    def static avatarUrl

    def static institutionLogoUrl

    @PostConstruct
    def static registerMarshallers() {

        JSON.createNamedConfig('lowDetail') {
            it.registerObjectMarshaller(SubmissionStats) {
                [
                        userId                  : it.userId,
                        submissionsCount        : it.submissionsCount,
                        triedProblemsCount      : it.triedProblemsCount,
                        solvedProblemsCount     : it.solvedProblemsCount,
                        languageAndEvaluationMap: it.languageAndEvaluationMap,
                        ndCount                 : it.ndCount
                ]
            }

            it.registerObjectMarshaller(Problem) {
                def problem = [
                        id          : it.id,
                        name        : it.name(),
                        nd          : it.nd,
                        topics      : it.topics ? it.topics.collect({ return [ name: it.name() ]}) : null,
                        status      : it.status.toString(),
                        problemType : it.problemType.toString(),
                        rankVotes   : it.rankVotes,
                        userRank    : it.userRank
                ]

                if (it.metaClass.hasProperty(Problem, 'currentUser')) {
                    problem << [currentUser : it.currentUser]
                }

                return problem
            }
        }

        JSON.createNamedConfig('atLeastTeacher') {

            registerUserMarshallers(it)
            registerProblemMarshallers(it)

            it.registerObjectMarshaller(ProblemChoice) {
                def problemChoice =  [
                        id          : it.id,
                        description : it.description(),
                        correct     : it.correct,
                        choiceOrder : it.choiceOrder
                ]

                problemChoice
            }
        }

        JSON.registerObjectMarshaller(Date) {
            DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis()
            DateTime dt = new DateTime((it as Date).getTime())
            formatter.print(dt)
        }

        JSON.registerObjectMarshaller(ErrorResponse) {
            [
                    status: it.status,
                    reason: it.reason,
                    errors: it.errors
            ]
        }

        JSON.registerObjectMarshaller(Feed.Type) {
            it as String
        }

        JSON.registerObjectMarshaller(ErrorReason) {

            def message

            if (it.params) {
                message = MessageFormat.format(it.reason as String, (it.params as List).toArray())
            } else {
                message = it.reason
            }

            [
                    code   : it.value,
                    message: message
            ]
        }

        JSON.registerObjectMarshaller(Feed) { Feed feed ->

            def body = [:]

            if (feed.type == Feed.Type.USER_SUBMISSION_STATUS) {
                Submission submission = Submission.get(feed.body['submissionId'])

                body = [
                        submission: submission
                ]
            }

            [
                    id  : feed.id,
                    date: feed.dateCreated,
                    body: body,
                    type: feed.type
            ]
        }

        JSON.registerObjectMarshaller(Feed.Type) {
            it as String
        }

        JSON.registerObjectMarshaller(Group) {
            def group = [
                    id         : it.id,
                    name       : it.name,
                    url        : it.url,
                    description: it.description,
                    startDate  : it.startDate,
                    endDate    : it.endDate,
                    dateCreated: it.dateCreated,
                    lastUpdated: it.lastUpdated,
                    institution: it.institution,
            ]

            group
        }

        registerUserMarshallers(JSON)

        JSON.registerObjectMarshaller(Language) {
            [
                    id           : it.id,
                    name         : it.name,
                    compiler     : it.compiler,
                    extension    : it.extension,
                    script       : it.script,
                    execParams   : it.execParams,
                    compileParams: it.compileParams,
                    label        : it.label
            ]
        }

        registerProblemMarshallers(JSON)

        JSON.registerObjectMarshaller(ProblemChoice) {
            def problemChoice =  [
                    id          : it.id,
                    description : it.description(),
                    choiceOrder : it.choiceOrder
            ]

            problemChoice
        }

        JSON.registerObjectMarshaller(Submission.Evaluation) {
            it as String
        }

        JSON.registerObjectMarshaller(MessageGroup.Status) {
            it as String
        }

        JSON.registerObjectMarshaller(MessageGroup.Type) {
            it as String
        }

        JSON.registerObjectMarshaller(TopCoder) {
            [
                    user    : it.user,
                    position: it.position,
                    points  : it.points
            ]
        }

        JSON.registerObjectMarshaller(Submission) {
            def testCaseEvaluations

            def showTestCaseEvaluations =
                    !it.metaClass.hasProperty(TestCaseEvaluation, 'showTestCaseEvaluations') || it.showTestCaseEvaluations

            if (showTestCaseEvaluations && it.testCaseEvaluations) {
                testCaseEvaluations = it.testCaseEvaluations.sort { a, b ->
                    !a.testCase.example <=> !b.testCase.example ?: a.testCase.id <=> b.testCase.id
                }
            }

            [
                    id                 : it.id,
                    time               : it.time,
                    tries              : it.tries,
                    comment            : it.comment,
                    submissionDate     : it.submissionDate,
                    evaluation         : it.evaluation,
                    filename           : it.originalFilename,
                    testCaseEvaluations: testCaseEvaluations,
                    user               : [
                        id: it.user.id,
                        name: it.user.name,
                        avatar: "$avatarUrl/thumbs/$it.user.avatar"
                    ],
                    problem            : [
                        id: it.problem.id,
                        name: it.problem.name()
                    ],
                    language           : it.language,
                    errorMsg           : showTestCaseEvaluations ? it.errorMsg() : null,
                    codeParts          : it.parts
            ]
        }

        JSON.registerObjectMarshaller(TestCaseEvaluation) {
            def testCaseEvaluation = [
                    testCaseId: it.testCase.id,
                    tip       : it.testCase.tip(),
                    evaluation: it.evaluation,
                    errorMsg  : it.errorMsg,
                    time      : it.time
            ]

            def hasShowDiffProperty = it.metaClass.hasProperty(TestCaseEvaluation, 'showDiff')
            if (!hasShowDiffProperty || (hasShowDiffProperty && it.showDiff)) {
                testCaseEvaluation << [diff: it.diff]
                if (it.evaluation != Submission.Evaluation.CORRECT) {
                    testCaseEvaluation.input = it.testCase.input
                    testCaseEvaluation.large = it.testCase.large
                }
            }

            testCaseEvaluation
        }

        JSON.registerObjectMarshaller(SubmissionStats) {
            [
                    userId                    : it.userId,
                    submissionsCount          : it.submissionsCount,
                    triedProblemsCount        : it.triedProblemsCount,
                    solvedProblemsCount       : it.solvedProblemsCount,
                    ndCount                   : it.ndCount,
                    solvedProblemsCountByNd   : it.solvedProblemsCountByNd,
                    triedProblemsCountByNd    : it.triedProblemsCountByNd,
                    triedProblemsCountByTopic : it.triedProblemsCountByTopic,
                    solvedProblemsCountByTopic: it.solvedProblemsCountByTopic,
                    languageAndEvaluationMap  : it.languageAndEvaluationMap,
                    history                   : it.history

            ]
        }

        JSON.registerObjectMarshaller(Questionnaire) {
            def questionnaire = [
                    id         : it.id,
                    title      : it.title,
                    description: it.description,
                    score      : it.score,
                    startDate  : it.startDate,
                    endDate    : it.endDate,
                    serverTime : new Date(),
                    dateCreated: it.dateCreated,
                    lastUpdated: it.lastUpdated,
                    group      : it.group,
                    partialScore: it.partialScore
            ]
            if (it.metaClass.hasProperty(Questionnaire, 'currentUser')) {
                questionnaire << [currentUser: it.currentUser]
            }

            questionnaire
        }

        JSON.registerObjectMarshaller(QuestionnaireUserPenalty) {
            [
                    id                  : it.id,
                    dateCreated         : it.dateCreated,
                    penalty             : it.penalty,
                    user                : it.user.id,
                    questionnaireProblem: [
                            id   : it.questionnaireProblem.id,
                            score: it.questionnaireProblem.score
                    ]
            ]
        }

        JSON.registerObjectMarshaller(TestCase) {
            [
                    id         : it.id,
                    input      : it.input != null ? it.input.take(2024) : null,
                    output     : it.output != null ? it.output.take(2024) : null,
                    large      : it.large,
                    example    : it.example,
                    tip        : it.tip(),
                    dateCreated: it.dateCreated,
                    lastUpdated: it.lastUpdated,
            ]
        }

        JSON.registerObjectMarshaller(UserGroup.Role) {
            it as String
        }

        JSON.registerObjectMarshaller(Pendency.PendencyKind) {
            it as String
        }

        JSON.registerObjectMarshaller(Pendency.Status) {
            it as String
        }

        JSON.registerObjectMarshaller(NotificationPreferences.Type) {
            it as String
        }

        JSON.registerObjectMarshaller(Pendency) {
            [
                    id         : it.id,
                    kind       : it.kind,
                    status     : it.status,
                    user       : it.user,
                    institution: it.institution,
                    group      : it.group,
                    params     : it.params,
                    dateCreated: it.dateCreated,
                    lastUpdated: it.lastUpdated
            ]
        }

        JSON.registerObjectMarshaller(PendencyKey.Type) {
            it as String
        }

        JSON.registerObjectMarshaller(PendencyKey) {
            [
                    key        : it.hashKey,
                    type       : it.type,
                    id         : it.entity,
                    dateCreated: it.dateCreated,
                    lastUpdated: it.lastUpdated,
            ]
        }

        JSON.registerObjectMarshaller(Plagiarism.Status) {
            it as String
        }

        JSON.registerObjectMarshaller(Plagiarism) {
            [
                    id                   : it.id,
                    plagiarizedSubmission: it.submission1.submissionDate.before(it.submission2.submissionDate) ? it.submission1 : it.submission2,
                    suspiciousSubmission : it.submission2.submissionDate.after(it.submission1.submissionDate) ? it.submission2 : it.submission1,
                    similarity           : it.percentage,
                    status               : it.status
            ]
        }

        JSON.createNamedConfig('public') {}

        JSON.createNamedConfig('private') {
            it.registerObjectMarshaller(User) {
                [
                        id         : it.id,
                        name       : it.name,
                        username   : it.username,
                        email      : it.email,
                        avatar     : "$avatarUrl/thumbs/$it.avatar",
                        institution: it.institution,
                        roles      : it.authorities,
                        locale     : it.locale
                ]
            }
        }
    }

    private static void registerUserMarshallers(registry) {

        registry.registerObjectMarshaller(Institution.Status) {
            it as String
        }

        registry.registerObjectMarshaller(Institution) {
            def institution = [
                    id     : it.id,
                    name   : it.name,
                    acronym: it.acronym,
                    logo   : "$institutionLogoUrl/$it.logo",
                    status : it.status
            ]

            if (it.hasProperty('role')) {
                institution << [role: it.role]
            }

            institution
        }

        registry.registerObjectMarshaller(UserInstitution.Role) {
            it as String
        }

        registry.registerObjectMarshaller(Role) {
            [
                    id       : it.id,
                    authority: it.authority
            ]
        }

        registry.registerObjectMarshaller(User) {
            def user = [
                    id         : it.id,
                    name       : it.name,
                    avatar     : "$avatarUrl/thumbs/$it.avatar",
                    institution: it.institution
            ]

            if (it.metaClass.hasProperty(User, 'role')) {
                user << [role: it.role]
            }

            if (it.metaClass.hasProperty(User, 'debug')) {
                user << [debug: it.debug]
            }

            user
        }
    }

    private static void registerProblemMarshallers(registry) {
        registry.registerObjectMarshaller(Problem.Status) {
            it as String
        }

        registry.registerObjectMarshaller(Topic) {
            [
                    id  : it.id,
                    name: it.name()
            ]
        }

        registry.registerObjectMarshaller(Problem) {

            def examples = it.exampleTestCases()

            def testCases = [
                    examples: examples ? examples.size() : 0,
                    total   : it.testCases ? it.testCases.size() : 0
            ]

            def problem = [
                    id          : it.id,
                    name        : it.name(),
                    description : it.description(),
                    inputFormat : it.inputFormat(),
                    outputFormat: it.outputFormat(),
                    source      : it.source,
                    level       : it.level,
                    timeLimit   : it.timeLimit,
                    nd          : it.nd,
                    dateCreated : it.dateCreated,
                    lastUpdated : it.lastUserUpdate,
                    approvedBy  : it.userApproved ? [ id: it.userApproved.id, name: it.userApproved.name ] : null,
                    suggestedBy : [ id: it.userSuggest.id, name: it.userSuggest.name ],
                    topics      : it.topics,
                    status      : it.status,
                    problemType : it.problemType.toString(),
                    baseCode    : it.baseCode,
                    blankLines  : it.blankLines,
                    choices     : it.choices,
                    testCases   : testCases,
                    quizOnly    : it.quizOnly,
                    baseLanguage: it.baseLanguage,
                    userRank    : it.userRank,
                    rankVotes   : it.rankVotes
            ]

            if (it.metaClass.hasProperty(Problem, 'currentUser')) {
                problem << [currentUser : it.currentUser]
            }

            if (it.metaClass.hasProperty(Problem, 'score')) {
                problem << [score : it.score]
            }

            problem
        }
    }
}
