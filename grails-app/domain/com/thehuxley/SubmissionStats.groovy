package com.thehuxley

import com.thehuxley.Submission.Evaluation
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

class SubmissionStats {

    private static final DateTimeFormatter dateFtm = DateTimeFormat.forPattern("yyyyMMdd")

    static mapWith = "none"

    final long userId

    int submissionsCount    = 0
    int triedProblemsCount  = 0
    int solvedProblemsCount = 0
    int ndCount             = 0

    Map languageAndEvaluationMap = [:]

    Map solvedProblemsCountByTopic
    Map triedProblemsCountByTopic
    Map solvedProblemsCountByNd
    Map triedProblemsCountByNd

    Map submissionCountByLanguage
    Map history

    transient Set triedProblemsIds = new HashSet<>()
    transient Set solvedProblemsIds = new HashSet<>()

    def SubmissionStats(long userId, params) {
        this.userId = userId

        if (params.excludeTopics == null || params.excludeTopics == false) {
            solvedProblemsCountByTopic  = [:]
            triedProblemsCountByTopic   = [:]
            solvedProblemsCountByNd     = [:]
            triedProblemsCountByNd      = [:]
        }

        if (params.excludeLanguage == null || params.excludeLanguage == false) {
            submissionCountByLanguage = [:]
        }

        history = [:]
    }

    def add(long userId, Date date, long problem, Evaluation eval, int nd, int count, String[] topics, Long languageId) {

        if (userId != this.userId) return

        def isCorrect = eval == Submission.Evaluation.CORRECT

        def alreadyTried = triedProblemsIds.contains(problem)

        submissionsCount += count

        def alreadySolved = solvedProblemsIds.contains(problem)

        if (!alreadySolved && triedProblemsCountByTopic != null) {
            topics.each() { String topic ->
                if (!alreadyTried) updateTopicCount(triedProblemsCountByTopic, topic)
                if (isCorrect) updateTopicCount(solvedProblemsCountByTopic, topic)
            }
        }

        if (!alreadyTried) {
            triedProblemsIds.add(problem)
            triedProblemsCount++
            if (triedProblemsCountByNd != null) {
                updateTopicCount(triedProblemsCountByNd, nd)
            }
        }

        if (!alreadySolved && isCorrect) {
            if (triedProblemsCountByNd != null) {
                updateTopicCount(solvedProblemsCountByNd, nd)
            }
            solvedProblemsIds.add(problem)
            solvedProblemsCount++
        }

        if (languageId) {
            def languageEvaluations = languageAndEvaluationMap[languageId]

            if (languageEvaluations == null) {
                languageEvaluations = mapCountByEvaluation()
                languageAndEvaluationMap[languageId] = languageEvaluations
            }

            languageEvaluations[eval as String] += count
            languageEvaluations['TOTAL'] += count
        }

        String dateStr = dateFtm.print(date.getTime())

        if (history != null) {
            def hist = history[dateStr]

            if (hist == null) {
                hist = [ submissionCount: 0, ndCount: 0, repeated: 0 ]
                history[dateStr] = hist
            }

            hist['submissionCount']++

            if (alreadySolved) {
                hist['repeated'] += 1
            } else if (isCorrect) {
                hist['ndCount'] += (nd*nd)
                ndCount += (nd*nd)
            }
        }

    }

    def add(long userId, Date date, int count) {

        if (userId != this.userId) return

        submissionsCount += count

        String dateStr = dateFtm.print(date.getTime())

        if (history != null) {
            def hist = history[dateStr]

            if (hist == null) {
                hist = [ submissionCount: 0, ndCount: 0, repeated: 0 ]
                history[dateStr] = hist
            }

            hist['submissionCount']++

        }

    }

    private def Map mapCountByEvaluation() {
        Map map = [:]
        Submission.Evaluation.values().each {
            map[it as String] = 0
        }
        map["TOTAL"] = 0
        return map
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
}
