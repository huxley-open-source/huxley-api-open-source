package com.thehuxley

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'userId, problemId')
class UserProblemVote implements Serializable {

    Long userId
    Long problemId
    short score

    static mapping = {
        id composite: ['problemId', 'userId']
    }

    static constraints = {
        userId nullable: false
        problemId nullable: false
        score nullable: false
    }
}
