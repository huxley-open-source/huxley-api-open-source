package com.thehuxley

import grails.core.GrailsApplication
import grails.plugin.springsecurity.annotation.Secured
import grails.util.Environment

class AdminController {

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET", save: "POST", update: "PUT", delete: "DELETE"]

    GrailsApplication grailsApplication

    @Secured("permitAll")
    def getVersion() {
        respond([version: grailsApplication.metadata.getApplicationVersion(), environment: Environment.current.name])
    }

}
