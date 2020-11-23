package com.thehuxley

import org.springframework.security.crypto.codec.Hex

import java.security.MessageDigest

class QueueService {

    def rabbitMessagePublisher

    def sendSubmissionToReevaluation(Submission submission) {
        def dataMap = [submissionId: submission.id]

        rabbitMessagePublisher.send {
            routingKey = "reevaluation_queue"
            body = dataMap
        }
    }

    def runCode(String hash, String input, String sourceCode, String filename, Language language, Problem problem) {
        def dataMap = [hash      : hash, filename: filename, language: language.script,
                       sourceCode: sourceCode, input: input]

        rabbitMessagePublisher.send {
            routingKey = "code_queue"
            body = dataMap
        }
    }

    def sendSubmissionToJudge(Submission submission, allTestCases, boolean isReevaluation = false) {
        def testCases = []

        allTestCases.each {
            def testCase = [
                    id     : it.id,
                    input  : it.input,
                    output : it.output,
                    example: it.example

            ]
            testCases.add(testCase)
        }

        def language = [
                name  : submission.language.name,
                script: submission.language.script
        ]

        def dataMap = [submissionId  : submission.id, filename: submission.originalFilename, language: language,
                       sourceCode    : submission.getSourceCode(), testCases: testCases*.id, time: submission.time,
                       problem       : [id: submission.problem.id, timeLimit: submission.problem.timeLimit],
                       isReevaluation: isReevaluation]

        Submission.withTransaction {
            submission.testCaseEvaluations.each {
                it.delete()
            }

            submission.save(flush: true)
        }

        rabbitMessagePublisher.send {
            priority = isReevaluation ? 0 : 10
            routingKey = "submission_queue"
            body = dataMap
        }

        log.info "Submissão enviada para a avaliação. submission.id ${submission.id}, reevaluation = ${isReevaluation}"
    }

    def sendSubmissionsToOracle(String hash, String input, List<Submission> chosenSubmissions) {
        def submissions = []

        chosenSubmissions.each {
            def language = [
                    name  : it.language.name,
                    script: it.language.script
            ]

            def problem = [
                    id       : it.problem.id,
                    timeLimit: it.problem.timeLimit
            ]

            def submission = [
                    submissionId: it.id,
                    filename    : it.originalFilename,
                    language    : language,
                    sourceCode  : it.getSourceCode(),
                    time        : it.time,
                    problem     : problem
            ]

            submissions.add(submission)
        }

        def dataMap = [hash: hash, input: input, submissions: submissions]

        log.info "Enviando entrada para o oráculo. ${input}"

        rabbitMessagePublisher.send {
            routingKey = "oracle_queue"
            body = dataMap
        }
    }

}

