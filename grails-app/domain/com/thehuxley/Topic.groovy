package com.thehuxley

import groovy.transform.EqualsAndHashCode
import org.springframework.context.i18n.LocaleContextHolder

@EqualsAndHashCode(includes = 'id')
class Topic implements Serializable {

    static hasMany = [problems: Problem, i18ns: TopicI18n]

    static searchable = {
        except = ["problems"]
    }

    static mapping = {
        id generator: "sequence", params: [sequence: "topic_id_seq"]
        i18ns lazy: false, fetchStrategy: 'select'
    }

    def name() {
        i18ns.find { it.locale.equals(LocaleContextHolder.getLocale().toString()) }?.name
    }

}

@EqualsAndHashCode(includes = 'topic, locale')
class TopicI18n implements Serializable {

    String locale
    String name

    static belongsTo = [topic: Topic]

    static mapping = {
        table 'topic_i18n'
        id composite: ['topic', 'locale']
        topic insertable: false, updateable: false
        version false
    }

}