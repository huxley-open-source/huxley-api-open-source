package com.thehuxley

import com.google.common.cache.CacheBuilder
import org.springframework.security.crypto.codec.Hex

import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class CodeRunnerService {

    def codeResultCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .build()

    def queueService

    def runCode(String input, String sourceCode, String filename, Language language, Problem problem) {
        if (!input) {
            def inputId = Long.MAX_VALUE

            problem.exampleTestCases().each({
                if (it.id < inputId) {
                    inputId = it.id
                    input = it.input
                }
            })

        }

        def hash = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest((new Random().nextInt() + new Date().toString()).bytes)))
        queueService.runCode(hash, input, sourceCode, filename, language, problem)
        hash
    }

    def cacheResult(hash, output) {
        if (!output) {
            output = 'EMPTY'
        }

        codeResultCache.put(hash, output)
    }

    def getResult(hash) {
        return codeResultCache.getIfPresent(hash)
    }

}
