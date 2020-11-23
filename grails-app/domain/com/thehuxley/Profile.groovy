package com.thehuxley

class Profile implements Serializable {

    String photo
    String smallPhoto
    Date lastLogin
    Date dateCreated
    Date lastUpdated

    User user
    Institution institution

    static constraints = {
        user unique: true
        institution nullable: false
    }

    static mapping = {
        id generator: "sequence", params: [sequence: "profile_id_seq"]
    }

}
