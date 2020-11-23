package com.thehuxley

class Institution implements Serializable {

    enum Status {
        PENDING, APPROVED, REJECTED
    }

    String name
    String acronym
    String phone
    String logo = "default.png"
    Status status = Status.PENDING

    static hasMany = [groups: Group, usersInstitution: UserInstitution]

    static searchable = {
        except = ["groups"]
    }

    static mapping = {
        id generator: "sequence", params: [sequence: "institution_id_seq"]
        status enumType: "ordinal"
    }

    static constraints = {
        name blank: false, unique: true
        acronym blank: false, size: 1..20
        phone nullable: true
    }

}
