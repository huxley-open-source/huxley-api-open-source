package com.thehuxley

class TopCoder implements Serializable {

    Double points
    Long position

    static belongsTo = [user: User]

    static mapping = {
        id generator: "sequence", params: [sequence: "top_coder_id_seq"]
    }

    static constraints = {
        user unique: true
        position nullable: true
    }

}
