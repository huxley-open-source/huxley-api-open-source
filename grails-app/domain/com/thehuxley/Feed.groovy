package com.thehuxley

class Feed implements Serializable {

    enum Type {
        USER_SUBMISSION_STATUS
    }

    Date dateCreated
    Date lastUpdated
    Type type
    Map body
    User recipient

    static mapping = {
        id generator: "sequence", params: [sequence: "feed_id_seq"]
        type enumType: "ordinal"
    }

    static constraints = {
        body nullable: true
        type nullable: false
    }

}
