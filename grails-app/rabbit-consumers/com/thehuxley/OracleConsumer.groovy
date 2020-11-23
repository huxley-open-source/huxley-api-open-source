package com.thehuxley

import com.budjb.rabbitmq.consumer.AutoAck
import com.budjb.rabbitmq.consumer.MessageContext

class OracleConsumer {

    static rabbitConfig = [
            queue        : "oracle_result_queue",
            autoAck      : AutoAck.POST,
//            retry        : true,
            prefetchCount: 1
    ]

    def oracleService

    /**
     * Handle an incoming RabbitMQ message.
     *
     * @param body The converted body of the incoming message.
     * @param context Properties of the incoming message.
     * @return
     */
    def handleMessage(body, MessageContext messageContext) {
        oracleService.cacheResult(body["hash"], body["output"])
    }

}
