package com.thehuxley

import com.budjb.rabbitmq.consumer.AutoAck
import com.budjb.rabbitmq.consumer.MessageContext

class PlagiarismConsumer {

    static rabbitConfig = [
//            binding      : "huxley-dev", //Dynamic binding in resources.groovy
            connection   : "plagiarism",
            autoAck      : AutoAck.POST,
            retry        : true,
            prefetchCount: 10
    ]

    def plagiarismService

    /**
     * Handle an incoming RabbitMQ message.
     *
     * @param body The converted body of the incoming message.
     * @param context Properties of the incoming message.
     * @return
     */
    def handleMessage(body, MessageContext messageContext) {
        def sourceSubmission = Submission.get(body["source_submission"].split("/").last() as Long)
        def targetSubmission = Submission.get(body["target_submission"].split("/").last() as Long)
        def similarity = new BigDecimal(body["similarity"] as String)

        log.info "Recebendo notificação de plágio. sourceSubmission.id = ${sourceSubmission.id}, targetSubmission = ${targetSubmission.id}, similarity = ${similarity}"

        if (sourceSubmission.user.id != targetSubmission.user.id) {
            if (sourceSubmission.submissionDate < targetSubmission.submissionDate) {
                plagiarismService.registerPlagiarismSuspect(sourceSubmission, targetSubmission, similarity)
            } else {
                plagiarismService.registerPlagiarismSuspect(targetSubmission, sourceSubmission, similarity)
            }
        }
    }
}
