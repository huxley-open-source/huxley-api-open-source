package com.thehuxley

import grails.plugin.springsecurity.annotation.Secured

import org.springframework.http.HttpStatus

class LanguageController {

    static responseFormats = ['json']

    def languageService

    @Secured("permitAll")
    def index() {
        def languages = languageService.list(languageService.normalize(params))

        response.setHeader("total", languages.totalCount as String)

        respond languages
    }

    @Secured("permitAll")
    def show(Long id) {
        def language = Language.get(id)

        if (language) {
            respond language
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def save(Language language) {
        if (language.validate()) {
            respond languageService.save(language)
        } else {
            respond language.errors
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def update(Language language) {
        if (language.validate()) {
            respond languageService.save(language)
        } else {
            respond languageService.errors
        }
    }

    @Secured("hasAnyRole('ROLE_ADMIN')")
    def delete(Language language) {
        languageService.delete(language)
        render status: HttpStatus.NO_CONTENT
    }

}
