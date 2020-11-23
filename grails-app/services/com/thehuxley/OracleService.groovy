package com.thehuxley

import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

import grails.orm.PagedResultList

class OracleService {

    def oracleCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(500)
            .build()

    def oracleSizeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(500)
            .build()

    def queueService

    def cacheResult(hash, output) {
        if (!output) {
            output = "SEM RESPOSTA"
        }
        oracleCache.asMap().put(hash, output)
        oracleSizeCache.asMap().put(hash, output.size())
    }

    def getResultSize(hash) {
        if (oracleSizeCache.asMap().containsKey(hash)) {
            return oracleSizeCache.asMap().get(hash)
        }
        return null
    }

    def getResult(hash) {
        if (oracleCache.asMap().containsKey(hash)) {
            return oracleCache.asMap().get(hash)
        }
        return null
    }

    void sendToOracle(String hash, String input, Problem problem) {
        ArrayList<Submission> submissions = new ArrayList<>()

        int max = 5

        PagedResultList result = Submission.createCriteria().list(max: 1) {
            setReadOnly(true)
            eq('problem', problem)
            eq('evaluation', Submission.Evaluation.CORRECT)
            eq('user', problem.userSuggest)
            order('submissionDate', 'desc')
        }

        if (result.size() > 0) {
            Submission submission = result.first() as Submission
            submissions.add(submission)
        }

        List<User> correctUsers = Submission.createCriteria().list([max: 5]) {
            setReadOnly(true)
            eq('problem', problem)
            eq('evaluation', Submission.Evaluation.CORRECT)
            ne('user', problem.userSuggest)
            projections {
                distinct('user')
            }
        }

        // Ordena os usuários pelo topcoder, decrescente
        Collections.sort(correctUsers, new Comparator<User>() {
            @Override
            int compare(User u1, User u2) {
                if (u1.topCoder != null && u2.topCoder == null) return 1
                if (u1.topCoder == null && u2.topCoder != null) return -1
                if (u1.topCoder == null && u2.topCoder == null) return 0
                return u1.topCoder.position - u2.topCoder.position
            }
        })

        /*
            Recupera a submissão correta mais atual de cada usuário,
            ordenado pelo topcoder, verifica se o arquivo de submissão
            ainda existe, e se existir adiciona a submissão na
            lista de submissões a enviar ao orákulo.
            Lembrando que existe um limite máximo de submissões a enviar.
         */
        for (int i = 0; i < correctUsers.size() && submissions.size < max; ++i) {
            def user = correctUsers.get(i)

            Submission.createCriteria().list(max: 1) {
                setReadOnly(true)
                eq('problem', problem)
                eq('evaluation', Submission.Evaluation.CORRECT)
                eq('user', user)
                order('submissionDate', 'desc')
            }.each { submission ->
                submissions.add(submission)
            }
        }

        if (submissions.size == 0) {
            log.warn('Problema ' + problem + ' não possui submissões suficientes para o orákulo')
        } else {
            queueService.sendSubmissionsToOracle(hash, input, submissions)
        }
    }

}