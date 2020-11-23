package com.thehuxley

import grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.FetchMode
import org.hibernate.sql.JoinType
import org.springframework.context.i18n.LocaleContextHolder

class TopicService {

    def list(Map params) {
        params = normalize(params)
        
        if (!isSortable(params.sort)) {
            params.sort = null
        }
        def topics = checkInTopics(params)

        Topic.createCriteria().list(params) {
            createAlias('i18ns', 'i18ns', JoinType.LEFT_OUTER_JOIN)
            fetchMode('i18ns', FetchMode.JOIN)
            eq('i18ns.locale', LocaleContextHolder.getLocale().toString())
            if (params.q) {
                ilike("i18ns.name", "%$params.q%")
            }
            if (topics && !topics.empty) {
                inList("id", topics)
            }
        }
    }

    def save(Topic topic) {
        topic.save()
    }

    def delete(Topic topic) {
        topic.delete()
    }

    private GrailsParameterMap normalize(GrailsParameterMap params) {
        params.q = params.q ?: ""
        params.haveInCommon = params.haveInCommon ? params.list("haveInCommon")*.asType(Long) : []

        return params
    }

    private checkInTopics(Map params) {

        LinkedHashSet<Long> inTopics = []

        if (params.haveInCommon && !params.haveInCommon.empty) {
            Problem.createCriteria().list() {
                topics {
                    inList("id", params.haveInCommon as List<Long>)
                }
            }.topics*.each {
                inTopics << (it.id as Long)
            }
        }

        (inTopics - (params.haveInCommon as List))
    }

    private def isSortable(param) {
        [
                "id",
                "name"
        ].contains(param)
    }

}
