package com.thehuxley

import grails.web.servlet.mvc.GrailsParameterMap

class LanguageService {

    def list(Map params) {
        params = normalize(params)

        if (!isSortable(params.sort)) {
            params.sort = null
        }

        Language.createCriteria().list(params) {
            or {
                if (params.q) {
                    ilike("name", "%$params.q%")
                    ilike("label", "%$params.q%")
                }
            }
        }
    }

    def save(Language language) {
        language.save()
    }

    def delete(Language language) {
        language.delete()
    }

    private GrailsParameterMap normalize(GrailsParameterMap params) {
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)
        params.q = params.q ?: ""

        return params
    }

    private boolean isSortable(param) {
        [
                "id",
                "name",
                "execParams",
                "compileParams",
                "compiler",
                "script",
                "extension",
                "label"
        ].contains(param)
    }

}
