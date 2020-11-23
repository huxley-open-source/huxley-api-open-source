package com.thehuxley

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'notificationType, user')
class NotificationPreferences implements Serializable {

    enum Type {
        QUESTIONNAIRE_CREATED,
        NEW_MESSAGE,
        PROBLEM_CHANGED
    }

    User user
    Type notificationType

    /** Pode ser usada pra o usuário configurar se deseja receber apenas um e-mail por dia com o resumo das notificações */
    boolean digested = false

    boolean email = false
    boolean web = false


    static mapping = {
        id composite: ['notificationType', 'user']
        notificationType enumType: "ordinal"
    }

    static constraints = {
        user nullable: false
    }

}
