package com.thehuxley

class ParamsInterceptor {

    SpringSecurityService springSecurityService

    ParamsInterceptor() {
        matchAll()
    }

    boolean before() {
        params.q = params.q ?: ""
        params.max = Math.min(params.max as Integer ?: 10, 100)

        if (params.order) {
            if (!["asc", "desc"].contains(params.order)) {
                params.order = "asc"
            }
        }

        true
    }

    boolean after() {
        true
    }

    void afterView() {
    }

}
