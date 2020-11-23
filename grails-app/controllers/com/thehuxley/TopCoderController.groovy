package com.thehuxley

import grails.plugin.springsecurity.annotation.Secured

class TopCoderController {

    static responseFormats = ['json']

    def userService
    def groupService
    def springSecurityService

    @Secured("permitAll")
    def index() {
        def currentUser = springSecurityService.currentUser as User

        if (params.group) {
            respond groupService.findAllInTopCoder(Group.load(params.long('group')), userService.normalize(params))
        } else if (params.focused && currentUser) {
            respond userService.findAllInTopCoderWithFocus(currentUser, params.max as Integer)
        } else {
            respond userService.findAllInTopCoder(userService.normalize(params))
        }
    }

    @Secured("permitAll")
    def show(Long id) {
        respond userService.findInTopCoder(User.load(id))
    }

}
