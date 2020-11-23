package com.thehuxley

import grails.plugin.springsecurity.annotation.Secured
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus

class TopicController {

    static responseFormats = ['json']

    def topicService

    @Secured("permitAll")
    def index() {
        def topics = topicService.list(topicService.normalize(params))

        response.setHeader("total", topics.totalCount as String)

        respond topics
    }

    @Secured("permitAll")
    def show(Long id) {
        def topic = Topic.get(id)

        if (topic) {
            respond topic
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def save() {
        Topic topic = deserialize(request.JSON)
        if (topic.validate()) {
            respond topicService.save(topic)
        } else {
            respond topic.errors
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def update(Topic topic) {
        if (topic.validate()) {
            respond topicService.save(topic)
        } else {
            respond topic.errors
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def delete(Topic topic) {
        topicService.delete(topic)
        render status: HttpStatus.NO_CONTENT
    }

    private def deserialize(json) {

        TopicI18n i18n = new TopicI18n([
                name: json['name'],
                locale: LocaleContextHolder.getLocale().toString()
        ])

        Topic topic = new Topic()
        topic.addToI18ns(i18n)
        return topic
    }
}
