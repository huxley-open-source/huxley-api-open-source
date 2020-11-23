package com.thehuxley

import net.kaleidos.hibernate.usertype.ArrayType

class SubmissionRestrictionsEvaluation implements Serializable {

    Long userId
    Long quizId
    Long submissionId
    Long problemId

    int restrictionCount = 0;
    int wrongRestrictionCount = 0;

    String result

    static constraints = {
        userId          nullable: false
        quizId          nullable: false
        submissionId    nullable: false
        problemId       nullable: false
        result          nullable: false
    }

    static mapping = {
        id generator: 'assigned', name: 'submissionId'
    }
}
