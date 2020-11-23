package com.thehuxley

import org.grails.web.converters.exceptions.ConverterException

class UrlMappings {

    static mappings = {
        //ADMIN
        "/v1/version"(controller: "admin", action: "getVersion")

        //ERRORS
        "400"(controller: "error", action: "badRequest")
        "401"(controller: "error", action: "unauthorized")
        "403"(controller: "error", action: "forbidden")
        "404"(controller: "error", action: "notFound")
        "406"(controller: "error", action: "notAcceptable")
        "500"(controller: "error", action: "internalServerError")

        //FILE
        "/v1/download/$key"(controller: "file", action: "download")

        //GROUPS
        "/v1/groups"(controller: "group", action: [
                GET : "index",
                POST: "save"
        ])
        "/v1/groups/$id"(controller: "group", action: [
                GET: "show",
                PUT: "update"
        ])
        "/v1/groups/validate"(controller: "group", action: [POST: "validate"])

        "/v1/groups/join/$key?"(controller: "group", action: [POST: "addByKey", PUT: "addByKey"])
        "/v1/groups/$groupId/key"(controller: "group", action: [GET: "getKey", PUT: "refreshKey"])
        "/v1/groups/key/$key?"(controller: "group", action: "getByKey")
        "/v1/groups/$groupId/users/$userId?"(controller: "group", action: [
                GET   : "getUsers",
                PUT   : "addUser",
                DELETE: "removeUser"
        ])
        "/v1/groups/$groupId/users/failingStudents"(controller: "group", action: "getFailingStudents")
        "/v1/groups/$groupId/users/add"(controller: "group", action: [POST: "addUsers"])
        "/v1/groups/$groupId/quizzes/$questionnaireId?"(controller: "group", action: "getQuestionnaires")
        "/v1/groups/$groupId/submissions/$submissionId?"(controller: "group", action: "getSubmissions")
        "/v1/groups/$groupId/resolvers"(controller: "group", action: [POST: "countResolvers"])
        "/v1/groups/$groupId/quiz/export"(controller: "group", action: "exportQuizesToExcel")

        //INSTITUTION
        "/v1/institutions"(resources: "institution")
        "/v1/institutions/validate"(controller: "institution", action: [POST: "validate"])
        "/v1/institutions/logo/$key"(controller: "institution", action: "getLogoByKey")
        "/v1/institutions/$institutionId/logo"(controller: "institution", action: [
                GET : "getLogo",
                POST: "uploadLogo",
                PUT : "cropImage"
        ])
        "/v1/institutions/$institutionId/users/$userId?"(controller: "institution", action: [
                GET   : "getUsers",
                PUT   : "addUser",
                DELETE: "removeUser"
        ])
        "/v1/institutions/$institutionId/users/add"(controller: "institution", action: [POST: "addUsers"])
        "/v1/institutions/$institutionId/changeStatus"(controller: "institution", action: [PUT: "changeStatus"])
        "/v1/institutions/$institutionId/groups/$groupId?"(controller: "institution", action: "getGroups")
        "/v1/institutions/$institutionId/normalizeRoles"(controller: "institution", action: [POST: "normalizeRoles"])

        //LANGUAGES
        "/v1/languages"(resources: "language")

        //PENDENCY
        "/v1/pendencies"(resources: "pendency")

        //PROBLEMS

        "/v1/problems"(resources: "problem")
        "/v1/problems/count"(controller: "problem", action: [GET: "getProblemsCount"])
        "/v1/problems/validate"(controller: "problem", action: [POST: "validate"])
        "/v1/problems/testcases/rewrite"(controller: "problem", action: [GET: "updateTestCaseFiles"])
        "/v1/problems/$problemId/oracle/$hash?"(controller: "problem", action: [POST: "sendToOracle", GET: "getOracleConsult"])
        "/v1/problems/$problemId/oracle/$hash/size"(controller: "problem", action: [GET: "getOracleConsultSize"])
        "/v1/problems/$problemId/run/$hash?"(controller: "problem", action: [POST: "runCode", GET: "getCodeResult"])
        "/v1/problems/$problemId/examples/$testCaseId?"(controller: "problem", action: "getExampleTestCases")
        "/v1/problems/$problemId/submissions/reevaluate"(controller: "Submission", action: [POST: "reevaluateByProblem"])
        "/v1/problems/$problemId/submissions/$submissionId?"(controller: "problem", action: "getSubmissions")
        "/v1/problems/$problemId/testcases-list"(controller: "problem", action: "getTestCaseList")
        "/v1/problems/$problemId/testcases/$testCaseId/input"(controller: "problem", action: "getInputTestCasePlainText")
        "/v1/problems/$problemId/testcases/$testCaseId/output"(controller: "problem", action: "getOutputTestCasePlainText")
        "/v1/problems/$problemId/testcases/$testCaseId?"(controller: "problem", action: [
                GET   : "getTestCases",
                POST  : "saveTestCase",
                PUT   : "updateTestCase",
                DELETE: "deleteTestCase",
        ])
        "/v1/problems/image/$key?"(controller: "problem", action: [GET: "getImageByKey", POST: "uploadImage"])
        "/v1/problems/$problemId/topics/$topicId"(controller: "problem", action: [PUT: "addTopic", DELETE: "deleteTopic"])
        "/v1/problems/stats"(controller: "problem", action: "getStats")
        "/v1/problems/countByStatus"(controller:"problem", action: "countByStatus")
        "/v1/problems/badTips"(controller:"problem", action: "findBadTips")
        "/v1/problems/$problemId/accept"(controller:"problem", action: [PUT: "acceptProblem" ])
        "/v1/problems/$problemId/translate"(controller:"problem", action: [PUT: "translate" ])
        "/v1/problems/$problemId/languages"(controller:"problem", action: "getLanguages")
        "/v1/problems/$problemId/score"(controller: "problem", action: [POST: "problemVote", GET: "getUserVote"])
        "/v1/problems/exception"(controller:"problem", action: [POST: "getErrorHelp" ])

        //QUESTIONNAIRES

        "/v1/quizzes"(resources: "questionnaire")
        "/v1/quizzes/$questionnaireId/forceUpdate"(controller: "questionnaire", action: [POST: "forceUpdate"])
        "/v1/quizzes/$questionnaireId/clone"(controller: "questionnaire", action: [POST: "clone"])
        "/v1/quizzes/$questionnaireId/problems/$problemId?"(controller: "questionnaire", action: [
                GET   : "getProblems",
                POST  : "addProblem",
                PUT   : "addProblem",
                DELETE: "removeProblem"
        ])
        "/v1/quizzes/$questionnaireId/problems/$problemId/examples/$testCaseId?"(controller: "problem", action: "getExampleTestCases")
        "/v1/quizzes/$questionnaireId/problems/$problemId/restrictions"(controller: "questionnaire", action: [ POST: "addProblemRestriction" ])
        "/v1/quizzes/$questionnaireId/similarities"(controller: "questionnaire", action: "getSimilarities")
        "/v1/quizzes/$questionnaireId/similarities/$plagiarismId"(controller: "questionnaire", action: "getSimilarity")
        "/v1/quizzes/$questionnaireId/similarities/$plagiarismId/confirm"(controller: "questionnaire", action: [POST: "confirmSimilarity"])
        "/v1/quizzes/$questionnaireId/similarities/$plagiarismId/discard"(controller: "questionnaire", action: [POST: "discardSimilarity"])
        "/v1/quizzes/$questionnaireId/users"(controller: "questionnaire", action: "getUsers")
        "/v1/quizzes/$questionnaireId/users/$userId/problems/$problemId?"(controller: "questionnaire", action: "getUserProblemScores")
        "/v1/quizzes/$questionnaireId/export"(controller: "file", action: [GET: "createQuestionnaireExportKey", POST: "createQuestionnaireExportKey"])
        "/v1/quizzes/$questionnaireId/penalties"(controller: "questionnaire", action: "getQuizzPenalties")
        "/v1/quizzes/$questionnaireId/scores"(controller: "questionnaire", action: "getQuizzWithConsolidatedScores")
        "/v1/quizzes/$questionnaireId/present"(controller: "questionnaire", action: "getPresentUsers")
        "/v1/quizzes/$questionnaireId/restrictions"(controller: "questionnaire", action: "findRestrictions")
        "/v1/quizzes/$questionnaireId/restrictions/result"(controller: "questionnaire", action: "findRestrictionsResult")
        "/v1/quizzes/import"(controller: "questionnaire", action: [POST: "importQuestionnaires"])

        //SUBMISSIONS
        "/v1/submissions"(resources: "submission")
        "/v1/submissions/$submissionId/evaluation"(controller: "submission", action: "showEvaluation")
        "/v1/submissions/reevaluate"(controller: "submission", action: [POST: "reevaluateAll"])
        "/v1/submissions/user/$userId/stats"(controller: "submission", action: "userStats")
        "/v1/submissions/group/$groupId/stats"(controller: "submission", action: "groupStats")
        "/v1/submissions/group/$groupId/thermometer"(controller: "submission", action: "groupThermometer")
        "/v1/submissions/stats"(controller: "submission", action: "groupStats")
        "/v1/submissions/problem/$problemId/stats"(controller: "submission", action: "problemStats")
        "/v1/submissions/problem/$problemId/try/$trynumber"(controller: "submission", action: "problemTrySource")
        "/v1/submissions/$submissionId/reevaluate"(controller: "submission", action: [POST: "reevaluate"])
        "/v1/submissions/$submissionId/sourcecode"(controller: "submission", action: "getSubmissionFile")
        "/v1/submissions/$submissionId/diff"(controller: "submission", action: "getDiffFile")
        "/v1/submissions/$submissionId/restriction" (controller: "submission", action: "findRestrictionEvaluation")
        "/v1/submissions/summary" (controller: "submission", action: "submissionStats")
        "/v1/submissions/evaluate/$submissionId/$problemId/$quizId" (controller: "submission", action: "evaluateRestriction")
        "/v1/submissions/$submissionId/testcase/$testCaseId" (controller: "submission", action: "downloadTestCase")

        //TOPCODER
        "/v1/topcoders"(resources: "topCoder")
        "/v1/topcoders/updateNds"(controller: "topCoder", action: "updateNds")
        "/v1/topcoders/$userId/refreshTopCoder"(controller: "topCoder", action: [POST: "refreshTopCoder"])
        "/v1/topcoders/refreshTopCoder"(controller: "topCoder", action: [POST: "refreshTopCoder"])

        //Topics
        "/v1/topics"(resources: "topic")

        //USER
        "/v1/user"(controller: "currentUser", action: [GET: "getCurrentUser", PUT: "update"])
        "/v1/user/password"(controller: "currentUser", action: [PUT: "updatePassword"])
        "/v1/user/avatar"(controller: "currentUser", action: [GET: "getAvatar", POST: "uploadAvatar", PUT: "cropAvatar"])
        "/v1/user/updateLocale"(controller: "currentUser", action: [PUT: "changeLanguage"])
        "/v1/user/institutions/$institutionId?"(controller: "currentUser", action: "getInstitutions")
        "/v1/user/groups/$groupId?"(controller: "currentUser", action: "getGroups")
        "/v1/user/problems/$problemId?"(controller: "currentUser", action: "getProblems")
        "/v1/user/problems/$problemId/examples/$testCaseId?"(controller: "problem", action: "getExampleTestCases")
        "/v1/user/problems/$problemId/stats"(controller: "currentUser", action: "getProblemData")
        "/v1/user/problems/$problemId/submissions/$submissionId?"(controller: "currentUser", action: [GET: "getProblemSubmissions", POST: "createSubmission"])
        "/v1/user/submissions/$submissionId?"(controller: "currentUser") {
            action = [GET: "getSubmissions", POST: "createSubmission"]
        }
        "/v1/user/quizzes/?" (controller: "currentUser", action: "getQuestionnaires")
        "/v1/user/$userId/quizzes/scores" (controller: "questionnaire", action: "getUserScores")
        "/v1/user/quizzes/scores" (controller: "currentUser", action: "getUserScores")
        "/v1/user/quizzes/$questionnaireId/problems/$problemId/submissions/$submissionId?" (controller: "currentUser", action: "getQuestionnaireProblemSubmissions")
        "/v1/user/messages/$messageId?" (controller: "currentUser", action: [
                GET: "getMessages",
                POST: "sendMessage",
                PUT: "editMessage",
                DELETE: "deleteMessage"
        ])
        "/v1/user/messages/stats" (controller: "currentUser", action: "messageStats")
        "/v1/user/messages/status" (controller: "currentUser", action: [POST: "changeMessageStatus"])

        "/v1/user/messages/$messageId/read"(controller: "currentUser", action: [
                POST: "markMessageAsRead"
        ])
        "/v1/user/messages/$messageId/response"(controller: "currentUser", action: [
                POST: "responseMessage"
        ])
        "/v1/user/messages/count"(controller: "currentUser", action: [
                POST: "getMessageCount"
        ])
        "/v1/user/messages/archive"(controller: "currentUser", action: [
                POST: "archiveOldMessages"
        ])
        "/v1/user/contact"(controller: "currentUser", action: [
                POST: "sendContactEmail"
        ])
        "/v1/user/feed/$feedId?"(controller: "currentUser", action: [
                GET : "getUserFeed",
                POST: "createFeedTest"
        ])

        "/v1/user/notification/preferences"(controller: "currentUser", action: [
                POST : "saveNotificationPreferences",
                PUT: "saveNotificationPreferences",
                GET: "listNotificationPreferences"
        ])

        "/v1/user/failingStudents"(controller: "currentUser", action: "listFailingStudents")

        //USERS

        "/v1/users"(resources: "user")
        "/v1/users/validate"(controller: "user", action: [POST: "validate"])
        "/v1/users/avatar/$key"(controller: "user", action: "getAvatarByKey")
        "/v1/users/password/$key"(controller: "user", action: [PUT: "updatePassword"])
        "/v1/users/recoveryPassword"(controller: "user", action: [POST: "recoveryPassword"])
        "/v1/users/anonymizer"(controller: "user", action: [POST: "anonymizer"])
        "/v1/users/$userId/avatar"(controller: "user", action: "getAvatar")
        "/v1/users/$userId/institutions/$institutionId?"(controller: "user", action: "getInstitutions")
        "/v1/users/$userId/groups/$groupId?"(controller: "user", action: "getGroups")
        "/v1/users/$userId/problems/$problemId?"(controller: "user", action: "getProblems")
        "/v1/users/$userId/problems/$problemId/examples/$testCaseId?"(controller: "problem", action: "getExampleTestCases")
        "/v1/users/$userId/problems/$problemId/stats"(controller: "user", action: "getProblemData")
        "/v1/users/$userId/problems/$problemId/submissions/$submissionId?"(controller: "user", action: "getProblemSubmissions")
        "/v1/users/$userId/submissions/$submissionId?"(controller: "user", action: "getSubmissions")
        "/v1/users/$userId/quizzes/?"(controller: "user", action: "getQuestionnaires")
        "/v1/users/$userId/quizzes/$questionnaireId/problems/$problemId/addPenalty"(controller: "questionnaire", action: "addPenalty")
        "/v1/users/$userId/quizzes/$questionnaireId/problems/$problemId/removePenalty"(controller: "questionnaire", action: "removePenalty")
        "/v1/users/$userId/quizzes/$questionnaireId/fail"(controller: "questionnaire", action: "failUserQuizz")
        "/v1/users/$userId/quizzes/$questionnaireId/problems/$problemId/penalty"(controller: "questionnaire", action: [
                GET   : "getPenalty",
                POST  : "addPenalty",
                PUT   : "addPenalty",
                DELETE: "removePenalty",
        ])
        "/v1/users/$userId/quizzes/$questionnaireId/problems/$problemId/submissions/$submissionId?"(controller: "user", action: "getQuestionnaireProblemSubmissions")

        //VOTE
        "/v1/vote/$testCaseId"(controller: "problem", action: [POST: "tipVote", GET: "votes"])

        "/error/$action?"(controller: "error")
        "500"(view: '/application/serverError')
        "404"(view: '/application/notFound')

        "400" (controller: "error", action: "badRequest", exception: ConverterException)
        "400" (controller: "error", action: "badRequest", exception: IllegalArgumentException)
    }
}
