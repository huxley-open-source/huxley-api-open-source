package com.thehuxley

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'messageGroupId, userId')
class MessageViewEvent implements Serializable {

    Long messageGroupId
    Long userId
    Long lastViewMillis

    static mapping = {
        id composite: ['messageGroupId', 'userId']
    }

    static constraints = {
        messageGroupId nullable: false
        userId nullable: false
        lastViewMillis nullable: false
    }

}
