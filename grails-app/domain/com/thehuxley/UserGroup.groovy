package com.thehuxley

class UserGroup implements Serializable {

    enum Role {
        STUDENT, TEACHER_ASSISTANT, TEACHER
    }

    Boolean enabled = true

    User user
    Group group
    Role role = Role.STUDENT

    static constraints = {
        user unique: ['group']
    }

    static mapping = {
        table "user_group"
        id generator: "sequence", params: [sequence: "user_group_id_seq"]
        role enumType: "ordinal"
        group column: "group_id"
    }

}
