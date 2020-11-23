package com.thehuxley

class Group implements Serializable {

    String name
    String url
    String description
    String accessKey
    Date startDate = new Date()
    Date endDate = new Date().plus(6 * 30)
    Date dateCreated
    Date lastUpdated

    static belongsTo = [institution: Institution]

    static hasMany = [usersGroup: UserGroup, questionnaires: Questionnaire]

    static searchable = true

    static mapping = {
        table "group"
        id generator: "sequence", params: [sequence: "group_id_seq"]
    }

    static constraints = {
        name blank: false, unique: true
        url unique: true, nullable: false, matches: "[a-zA-Z0-9-]+"
        description nullable: true
        accessKey nullable: true
        startDate nullable: false
        endDate nullable: false
    }

}