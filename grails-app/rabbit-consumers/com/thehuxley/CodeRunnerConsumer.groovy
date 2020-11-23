package com.thehuxley

import com.budjb.rabbitmq.consumer.AutoAck
import com.budjb.rabbitmq.consumer.MessageContext

class CodeRunnerConsumer {

    static rabbitConfig = [
            queue        : "code_result_queue",
            autoAck      : AutoAck.POST,
            prefetchCount: 1
    ]

    def codeRunnerService

    /**
     * Handle an incoming RabbitMQ message.
     *
     * @param body The converted body of the incoming message.
     * @param context Properties of the incoming message.
     * @return
     */
    def handleMessage(body, MessageContext messageContext) {
        codeRunnerService.cacheResult(body["hash"], body["output"])
    }

}
