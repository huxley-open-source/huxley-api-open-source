package com.thehuxley

import org.springframework.security.crypto.codec.Hex

import java.security.MessageDigest

class PendencyKey implements Serializable {

    enum Type {
        GROUP_INVITATION, CHANGE_PASSWORD, QUIZZ_EXPORT
    }

    String hashKey
    Type type
    Long entity
    Date dateCreated
    Date lastUpdated

    def beforeInsert() {
        hashKey = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest((new Random().nextInt() + new Date().toString()).bytes)))
    }

    static constraints = {
        hashKey nullable: true, unique: true
        type nullable: false
        entity nullable: true
    }

    static mapping = {
        id generator: "sequence", params: [sequence: "pendency_key_id_seq"]
        version false
        type enumType: "ordinal"
    }

}
