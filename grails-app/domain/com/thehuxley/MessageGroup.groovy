package com.thehuxley

/**
 * Created by marcio on 29/11/16.
 */
class MessageGroup {

    enum Type {
        PROBLEM_QUESTION,
        PROBLEM_BAD_NAME,
        BAD_DESCRIPTION,
        WRONG_TOPIC,
        BAD_TEST_CASE,
        MISSING_TEST_CASE,
        DIRECT_MESSAGE
    }

    enum Status {
        UNRESOLVED, RESOLVED, DISCARDED, ARCHIVED
    }

    String subject

    Date dateCreated
    Date lastUpdated

    Long userId
    Long problemId
    Long groupId
    Long recipientId

    Type type
    Status messageStatus = Status.UNRESOLVED

    String locale

    static mapping = {
        id generator: "sequence", params: [sequence: "message_id_seq"], bindable: true
        type enumType: "ordinal"
        messageStatus enumType: "ordinal"
    }

    static constraints = {
        subject nullable: true
        groupId nullable: true
        problemId nullable: true
        userId nullable: false
        recipientId nullable: true
    }
}
