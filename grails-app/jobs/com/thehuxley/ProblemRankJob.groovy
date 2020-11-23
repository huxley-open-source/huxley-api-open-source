package com.thehuxley

class ProblemRankJob {

    static triggers = {
        simple startDelay: 12 * 60 * 60 * 1000, repeatInterval: 12 * 60 * 60 * 1000
    }

    def problemService

    def execute() {
        log.info "JOB: Atualizando o rank dos problemas..."

        problemService.updateRank()

        log.info "Done."
    }

}
