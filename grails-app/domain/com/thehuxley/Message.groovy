package com.thehuxley

class Message implements Serializable {

    String body

    Date dateCreated
    Date lastUpdated

    Long senderId
    Long messageGroupId

    static mapping = {
        id generator: "sequence", params: [sequence: "message_id_seq"], bindable: true
        body type: "text"
    }

    static constraints = {
        body nullable: false, blank: false
        senderId nullable: false
        messageGroupId nullable: false
    }

}
