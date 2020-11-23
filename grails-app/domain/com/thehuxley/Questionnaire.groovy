package com.thehuxley

class Questionnaire implements Serializable {

    String title
    String description
    Double score = 0.0
    Date startDate
    Date endDate
    Date dateCreated
    Date lastUpdated

    /** nota dos problemas deve ser calculada de acordo com quantidade de testes acertados? */
    boolean partialScore = false

    Group group

    static belongsTo = Group

    static hasMany = [problems: Problem, submissions: Submission]

    static mapping = {
        id generator: "sequence", params: [sequence: "questionnaire_id_seq"]
        description type: "text"
        problems joinTable: [name: "questionnaire_problem", column: "problem_id", key: "questionnaire_id"]
        submissions joinTable: [name: "questionnaire_submission", column: "submission_id", key: "questionnaire_id"]
    }

    static constraints = {
        title blank: false
        description nullable: true, blank: true
        startDate nullable: false
        endDate nullable: false
        partialScore nullable: false
    }

}