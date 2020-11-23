package com.thehuxley

class Pendency implements Serializable {

    enum Status {
        PENDING, APPROVED, REJECTED
    }

    enum PendencyKind {
        INSTITUTION_DOCUMENT, //Indica ao usuário a necessidade de enviar documentos ao administrador para concluir o cadastro da instituição
        /*
            User -> Usuário para quem é a pendência
            Institution -> Instituição que possui a pendência
            Group -> Nulo
         */
                INSTITUTION_APPROVAL, //Indica ao administrador do sistema a necessidade de aprovar uma instituição cadastrada
        /*
            User -> Usuário que enviou a documentação
            Instituição -> Instituição do usuário que está enviando a documentação
            Group -> Nulo
         */
                TEACHER_DOCUMENT, //Indica ao usuário a necessidade de enviar documentos ao administrador da instituição para concluir o cadastro como professor
        /*
            User -> Usuário que precisa enviar a documentação
            Instituição -> Instituição quem precisa avaliar a documentação
            Group -> Nulo
         */
                TEACHER_APPROVAL, //Indica ao Administrador da instituição a necessidade de aprovar o cadastro de um professor
        /*
            User -> Professor que enviou a documentação
            Instituição -> Instituição que validará a documentação do professor
            Group -> Nulo
         */
                USER_GROUP_INVITATION, //Indica ao usuário que ele possui um convite para participara de um grupo
        /*
            User -> Usuário que recebeu o convite
            Institution -> Nulo
            Group -> Grupo que convidou
         */
                USER_GROUP_JOIN_REQUEST //Indica ao Professor que um usuário quer participar de um de seus grupos
        /*
            User -> Usuário que pediu para participar
            Institution -> Nulo
            Group -> Grupo ao qual o usuário quer participar
         */
    }

    enum PendencyParams {
        DOCUMENT //tipo de parametro para a url dos documentos enviados
    }

    PendencyKind kind
    Status status = Status.PENDING
    User user
    Institution institution
    Group group
    Map params
    Date dateCreated
    Date lastUpdated

    static mapping = {
        id generator: "sequence", params: [sequence: "pendency_id_seq"]
        kind enumType: "ordinal"
        status enumType: "ordinal"
    }

    static constraints = {
        group nullable: true
        institution nullable: true
    }

}
