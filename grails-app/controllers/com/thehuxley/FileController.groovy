package com.thehuxley

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.http.HttpStatus

class FileController {

    def springSecurityService
    def fileExportService

    @Secured("isAuthenticated()")
    def createQuestionnaireExportKey(Long questionnaireId) {

        Questionnaire questionnaire = Questionnaire.get(questionnaireId)
        User currentUser = springSecurityService.currentUser as User

        if (questionnaire) {

            def role = UserGroup.findByUserAndGroup(currentUser, questionnaire.group)?.role
            def institution = questionnaire.group.institution
            def isAdminInst = UserInstitution.findByUserAndInstitution(currentUser, institution)?.role ==
                    UserInstitution.Role.ADMIN_INST

            if ((role == UserGroup.Role.TEACHER) ||
                    (role == UserGroup.Role.TEACHER_ASSISTANT) ||
                    isAdminInst ||
                    (currentUser.authorities.authority.contains("ROLE_ADMIN"))) {

                def pendencyKey = new PendencyKey(entity: questionnaireId, type: PendencyKey.Type.QUIZZ_EXPORT).save(flush: true)

                render(contentType: "application/json", text: (pendencyKey as JSON) as String)
            } else {
                render status: HttpStatus.FORBIDDEN
            }
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    @Secured("permitAll")
    def download(String key) {

        def pendencyKey = PendencyKey.findByHashKey(key)

        if (pendencyKey) {
            if (pendencyKey.type == PendencyKey.Type.QUIZZ_EXPORT) {
                Questionnaire questionnaire = Questionnaire.get(pendencyKey.entity)

                def title = "$questionnaire.group.url-$questionnaire.id-quiz"

                byte[] reportFile = fileExportService.exportToExcel(questionnaire)
                response.setContentType("application/octet-stream")
                response.setHeader("Content-disposition", "filename=${title}.xls")
                response.outputStream << reportFile
            }

            pendencyKey.delete()
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

}
