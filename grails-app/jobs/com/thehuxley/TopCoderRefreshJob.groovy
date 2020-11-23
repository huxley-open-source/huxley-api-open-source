package com.thehuxley

class TopCoderRefreshJob {

    static triggers = {
        cron cronExpression: "0 0 3 * * ?" // execute job every 3 a.m.
    }

    def topCoderService

    def execute() {
        log.info "JOB: Atualizando o ND e o TopCoder..."

        topCoderService.updateNds()

        log.info "Done."
    }

}
