package com.thehuxley

class QuestionnaireViewEvent implements Serializable {

    Long quizId
    Long userId
    Date viewDate

    static mapping = {
        id generator: "sequence", params: [sequence: "questionnaire_view_event_seq"]
    }

    static constraints = {
        quizId nullable: false
        userId nullable: false
        viewDate nullable: false
    }

}
