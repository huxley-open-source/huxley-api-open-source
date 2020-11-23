package com.thehuxley

import com.budjb.rabbitmq.consumer.AutoAck
import com.budjb.rabbitmq.consumer.MessageContext

class EvaluationConsumer {

    static rabbitConfig = [
            queue        : "evaluation_queue",
            autoAck      : AutoAck.POST,
            retry        : false,
            prefetchCount: 10
    ]

    def submissionService

    /**
     * Handle an incoming RabbitMQ message.
     *
     * @param body The converted body of the incoming message.
     * @param context Properties of the incoming message.
     * @return
     */
    def handleMessage(body, MessageContext messageContext) {
        Submission submission = Submission.get(body["submissionId"] as Long)

        if (!submission) {
            return
        }

        def testCaseEvaluations = body["testCaseEvaluations"]

        testCaseEvaluations.each { evaluation ->
            def testCaseEvaluation = new TestCaseEvaluation()

            testCaseEvaluation.testCase = TestCase.get(evaluation["testCaseId"] as Long)
            testCaseEvaluation.errorMsg = evaluation["errorMsg"]
            testCaseEvaluation.diff = evaluation["diff"]
            testCaseEvaluation.evaluation = Submission.Evaluation.valueOf(evaluation["evaluation"] as String)
            testCaseEvaluation.time = evaluation["executionTime"]

            submission.addToTestCaseEvaluations(testCaseEvaluation)
        }

        Boolean isReevaluation = body["isReevaluation"] as Boolean

        submissionService.evaluateSubmission(submission, isReevaluation)
    }
}
