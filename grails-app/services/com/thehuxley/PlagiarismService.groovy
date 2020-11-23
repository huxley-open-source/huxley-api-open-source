package com.thehuxley

import grails.gorm.DetachedCriteria
import grails.web.servlet.mvc.GrailsParameterMap

class PlagiarismService {

    def registerPlagiarismSuspect(Submission sourceSubmission, Submission targetSubmission, BigDecimal similarity) {
        def plagiarism = Plagiarism.createCriteria().get {
            eq("submission1", sourceSubmission)
            eq("submission2", targetSubmission)
        }
        if (plagiarism) {
            plagiarism.percentage = similarity
        } else {
            plagiarism = new Plagiarism(submission1: sourceSubmission, submission2: targetSubmission, percentage: similarity)
        }
        plagiarism.save()
    }

    def changeStatus(Plagiarism plagiarism, Plagiarism.Status status) {
        plagiarism.status = status
        plagiarism.save()
    }

    def findAllByQuestionnaire(Questionnaire questionnaire, Map params) {
        Plagiarism.createCriteria().list(params) {
            and {
                exists(new DetachedCriteria(Questionnaire, 'q1').build {
                    projections {
                        property('id')
                    }
                    createAlias('submissions', 's')
                    eqProperty('s.id', 'this.submission1.id')
                    eq('id', questionnaire.id)
                })
                exists(new DetachedCriteria(Questionnaire, 'q2').build {
                    projections {
                        property('id')
                    }
                    createAlias('submissions', 's')
                    eqProperty('s.id', 'this.submission2.id')
                    eq('id', questionnaire.id)
                })
            }
            ge("percentage", 0.8D)
        }
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)

        return params
    }

    boolean isSortable(param) {
        [
                "id",
                "percentage",
                "submission1",
                "submission2",
                "status"
        ].contains(param)
    }

}
