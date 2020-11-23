package com.thehuxley

import com.budjb.rabbitmq.consumer.AutoAck
import com.budjb.rabbitmq.consumer.MessageContext

class ReevaluationConsumer {

    static rabbitConfig = [
            queue        : "reevaluation_queue",
            autoAck      : AutoAck.POST,
            retry        : true,
            prefetchCount: 10
    ]

    def queueService

    /**
     * Handle an incoming RabbitMQ message.
     *
     * @param body The converted body of the incoming message.
     * @param context Properties of the incoming message.
     * @return
     */
    def handleMessage(body, MessageContext messageContext) {
        def submission = Submission.get(body["submissionId"] as Long)

        submission.time = -1
        submission.evaluation = Submission.Evaluation.WAITING
        submission.testCaseEvaluations.clear()

        submission.save(flush: true, failOnError: true)
        
        def testCases = TestCase.findAllByProblem(submission.problem)

        queueService.sendSubmissionToJudge(submission, testCases, true)
    }
}
