package com.thehuxley

import grails.plugin.springsecurity.annotation.Secured
import org.springframework.http.HttpStatus
import org.springframework.context.i18n.LocaleContextHolder as LCH


class PendencyController {

    static responseFormats = ['json']
    static allowedMethods = [show: "GET", index: "GET", save: "POST", update: "PUT"]

    def pendencyService
    def mailService
    def institutionService
    def groupService
    def messageSource

    @Secured("permitAll")
    def index() {

        if (params.sort && !pendencyService.isSortable(params.sort)) {
            forward(controller: "Error", action: "wrongSortParam")
            return
        }

        respond pendencyService.list(pendencyService.normalize(params))
    }

    @Secured("permitAll")
    def show(Long id) {
        respond pendencyService.get(Pendency.load(id))
    }

    @Secured("permitAll")
    def save() {
        def pendencyJSON = request.JSON
        def pendency = new Pendency()

        if (pendencyJSON["institution"]) {
            pendency.institution = Institution.get(pendencyJSON["institution"]["id"] as Long)
        }

        if (pendencyJSON["group"]) {
            pendency.group = Group.get(pendencyJSON["group"]["id"] as Long)
            pendency.institution = pendency.group.institution
        }

        pendency.user = User.get(pendencyJSON["user"]["id"] as Long)
        pendency.kind = Pendency.PendencyKind.valueOf(pendencyJSON["kind"] as String)

        def response = pendencyService.save(pendency)

        if (response) {

            if (pendency.kind == Pendency.PendencyKind.INSTITUTION_APPROVAL) {

                def link = "${grailsApplication.config.huxley.baseURL}/admin/institutions?q=${pendency.institution.name}&sort=name&order=desc&page=1&all"

                mailService.sendMail {
                    from "support@thehuxley.com"
                    to "romero.malaquias@thehuxley.com"
                    cc "rodrigo.paes@thehuxley.com", "marcio.guimaraes@thehuxley.com"
                    subject messageSource.getMessage("email.pendency.institutionApproval.subject", [
                            pendency.institution.acronym,
                            pendency.institution.name
                    ].toArray(), LCH.getLocale())
                    html messageSource.getMessage("email.pendency.institutionApproval.body", [
                            "The Huxley Team",
                            pendency.institution.acronym,
                            pendency.institution.name,
                            pendency.user.name,
                            pendency.user.email,
                            link
                    ].toArray(), LCH.getLocale())
                }
            } else if (pendency.kind == Pendency.PendencyKind.TEACHER_APPROVAL) {

                def link = "${grailsApplication.config.huxley.baseURL}/institutions/$pendency.institution.id/pendency"

                UserInstitution.findAllByInstitutionAndRole(pendency.institution, UserInstitution.Role.ADMIN_INST).user.each { User user ->
                    mailService.sendMail {
                        from "support@thehuxley.com"
                        to user.email
                        subject messageSource.getMessage("email.pendency.teacherApproval.subject", [
                                pendency.user.name
                        ].toArray(), LCH.getLocale())
                        html messageSource.getMessage("email.pendency.teacherApproval.body", [
                                user.name,
                                pendency.user.name,
                                pendency.user.email,
                                pendency.institution.name,
                                link
                        ].toArray(), LCH.getLocale())
                    }
                }
            }

            respond response
        } else {
            render status: HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    @Secured("permitAll")
    def update(Long id) {
        Pendency pendency = Pendency.get(id)

        def status = request.JSON["status"] ?: params.status

        if (status) {
            pendency.status = Pendency.Status.valueOf(status as String)
        }

        if (pendency.status != pendency.getPersistentValue("status")) {
            if (pendency.status == Pendency.Status.APPROVED) {
                switch (pendency.kind as String) {
                    case "USER_GROUP_INVITATION":
                        groupService.addToGroup(pendency.user, pendency.group)
                        break
                    case "INSTITUTION_APPROVAL":
                        institutionService.changeStatus(pendency.institution, Institution.Status.APPROVED)

                        mailService.sendMail {
                            to pendency.user.email
                            bcc "support@thehuxley.com"
                            subject messageSource.getMessage("email.pendency.institutionApproval.approved.subject", [
                                    pendency.institution.name
                            ].toArray(), LCH.getLocale())
                            html messageSource.getMessage("email.pendency.institutionApproval.approved.body", [
                                    pendency.user.name,
                                    pendency.institution.acronym,
                                    pendency.institution.name,
                                    "${grailsApplication.config.huxley.baseURL}/groups/create"
                            ].toArray(), LCH.getLocale())
                        }

                        break
                    case "TEACHER_APPROVAL":
                        institutionService.addToInstitution(pendency.user, pendency.institution, UserInstitution.Role.TEACHER)

                        mailService.sendMail {
                            to pendency.user.email
                            bcc "support@thehuxley.com"
                            subject messageSource.getMessage("email.pendency.teacherApproval.approved.subject", [

                            ].toArray(), LCH.getLocale())
                            html messageSource.getMessage("email.pendency.teacherApproval.approved.body", [
                                    pendency.user.name,
                                    pendency.institution.name,
                                    "${grailsApplication.config.huxley.baseURL}/groups/create"
                            ].toArray(), LCH.getLocale())
                        }


                        break
                }
            } else if (pendency.status == Pendency.Status.REJECTED) {
                switch (pendency.kind as String) {
                    case "USER_GROUP_INVITATION":
                        groupService.removeFromGroup(pendency.user, pendency.group)
                        break
                    case "INSTITUTION_APPROVAL":
                        institutionService.changeStatus(pendency.institution, Institution.Status.REJECTED)

                        mailService.sendMail {
                            to pendency.user.email
                            bcc "support@thehuxley.com"
                            subject messageSource.getMessage("email.pendency.institutionApproval.rejected.subject", [
                                    pendency.institution.name
                            ].toArray(), LCH.getLocale())
                            html messageSource.getMessage("email.pendency.institutionApproval.rejected.body", [
                                    pendency.user.name,
                                    pendency.institution.acronym,
                                    pendency.institution.name
                            ].toArray(), LCH.getLocale())
                        }

                        break
                    case "TEACHER_APPROVAL":
                        institutionService.changeRole(pendency.user, pendency.institution, UserInstitution.Role.STUDENT)

                        mailService.sendMail {
                            to pendency.user.email
                            bcc "support@thehuxley.com"
                            subject messageSource.getMessage("email.pendency.teacherApproval.rejected.subject", [

                            ].toArray(), LCH.getLocale())
                            html messageSource.getMessage("email.pendency.teacherApproval.rejected.body", [
                                    pendency.user.name,
                                    pendency.institution.name,
                            ].toArray(), LCH.getLocale())
                        }

                        break
                }
            }

            respond pendencyService.save(pendency)
        } else {
            respond pendencyService.get(pendency)
        }
    }
}
