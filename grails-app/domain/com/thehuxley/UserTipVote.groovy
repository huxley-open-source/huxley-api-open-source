package com.thehuxley

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'userId, testCaseId')
class UserTipVote implements Serializable {

    Long userId
    Long testCaseId
    boolean useful

    static mapping = {
        id composite: ['testCaseId', 'userId']
    }

    static constraints = {
        userId nullable: false
        testCaseId nullable: false
        useful nullable: false
    }
}
