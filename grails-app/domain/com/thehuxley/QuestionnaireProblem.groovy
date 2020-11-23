package com.thehuxley

class QuestionnaireProblem implements Serializable {

    Double score

    Questionnaire questionnaire
    Problem problem

    static mapping = {
        table "questionnaire_problem"
        id generator: "sequence", params: [sequence: "questionnaire_problem_id_seq"]
    }

}
