package com.thehuxley

class UserInstitution implements Serializable {

    enum Role {
        STUDENT, TEACHER_ASSISTANT, TEACHER, ADMIN_INST
    }

    Boolean enabled = true

    User user
    Institution institution
    Role role = Role.STUDENT

    static constraints = {
        user unique: ['institution']
    }

    static mapping = {
        table "user_institution"
        id generator: "sequence", params: [sequence: "user_institution_id_seq"]
        role enumType: "ordinal"
    }

}
