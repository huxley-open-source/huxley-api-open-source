package com.thehuxley

class QuestionnaireUserPenalty implements Serializable {

    Date dateCreated
    Date lastUpdated
    Double penalty = 0.0

    QuestionnaireProblem questionnaireProblem
    User user

    static constraints = {
        questionnaireProblem nullabe: false, blank: false, unique: "user"
        user nullabe: false, blank: false
        penalty nullabe: false, blank: false
    }

    static mapping = {
        id generator: "sequence", params: [sequence: "questionnaire_user_penalty_id_seq"]
    }

}
