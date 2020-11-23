package com.thehuxley

class QuizProblemRestriction implements Serializable {

    Long quizId
    Long problemId
    String restrictions
    float penalty = 0

    static mapping = {
        id composite: ['quizId', 'problemId']
    }

    static constraints = {
        quizId          nullable: false
        problemId       nullable: false
        restrictions    nullable: false
    }
}
