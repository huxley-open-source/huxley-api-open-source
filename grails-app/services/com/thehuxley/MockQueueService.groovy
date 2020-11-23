package com.thehuxley

import com.google.common.io.Files
import grails.util.Holders
import org.springframework.boot.ApplicationHome
import org.springframework.context.ApplicationContext

import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class MockQueueService {

    def grailsApplication


    static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1)

    def sendSubmissionToReevaluation(Submission submission) {
        println 'MockQueueService sendSubmissionToReevaluation'
    }

    def runCode(String hash, String input, String sourceCode, String filename, Language language, Problem problem) {

        println 'lkasjdlaksjdlaskdjaslkdja ' + input
        ApplicationContext ctx = (ApplicationContext) Holders.grailsApplication.mainContext
        def codeRunnerService = ctx.getBean("codeRunnerService");


        Random random = new Random();
        if (random.nextInt(4) > 1) {
            println 'ASJDLJKASJJSSS>>> '
            codeRunnerService.cacheResult(hash, "!#ERROR:Traceback (most recent call last):\nFile \"/HuxleyCode.py\", line 1, in <module>\nif (a == 2):\nNameError: name 'a' is not defined")
        } else {
            codeRunnerService.cacheResult(hash, input)
        }



    }

    def sendSubmissionToJudge(Submission submission, allTestCases, boolean isReevaluation = false) {

        println 'MockQueueService sendSubmissionToJudge'

        ApplicationContext ctx = (ApplicationContext) Holders.grailsApplication.mainContext
        def submissionService = ctx.getBean("submissionService");
        def topCoderService = ctx.getBean("topCoderService");



        EXECUTOR.schedule(new Runnable() {
            @Override
            void run() {
                try {

                    def base = grailsApplication.config.huxleyFileSystem.base

                    def now = submission.submissionDate

                    def line = Files.readFirstLine(new File(base + submission.filename), Charset.defaultCharset())
                    String[] parts = line.split(",")

                    if (parts.length < 4) {
                        Random random = new Random();
                        submission.evaluation = Submission.Evaluation.CORRECT//Submission.Evaluation.values()[random.nextInt(12)]
                        submission.time = random.nextInt(4)
                        submission.totalTestCases = random.nextInt(5)
                        submission.correctTestCases = random.nextInt(submission.totalTestCases + 1)
                        def testCaseEvaluation = new TestCaseEvaluation()
                        testCaseEvaluation.testCase = allTestCases[0]
                        testCaseEvaluation.errorMsg = 'asdasdasdasdasdasdas'
                        //testCaseEvaluation.diff = 'dasd asdasdas dasd asd asd'
                        testCaseEvaluation.evaluation = submission.evaluation
                        testCaseEvaluation.time = submission.time

                        submission.addToTestCaseEvaluations(testCaseEvaluation)
                        println '>SDAS>D>ASD>ASD>AS'
                    } else {
                        println '>>>>@@@ hedsdsy'
                        submission.evaluation = Submission.Evaluation.valueOf(parts[2])
                        submission.time = Integer.parseInt(parts[3])
                        submission.totalTestCases = Integer.parseInt(parts[0])
                        submission.correctTestCases = Integer.parseInt(parts[1])
                    }


                } catch (Exception ex) {
                    ex.printStackTrace()
                }

                submissionService.update(submission)
                submissionService.updateEvaluation(submission)
                topCoderService.queueToUpdate(submission.user.id)

                if (submission.evaluation == Submission.Evaluation.CORRECT) {
                    try {
                    submissionService.evaluateRestrictions2(submission)
                    } catch (Exception ex) {
                        ex.printStackTrace()
                    }
                }
            }
        }, 3, TimeUnit.SECONDS)

        submission.evaluation = Submission.Evaluation.WAITING
        submissionService.update(submission)


    }

    def sendSubmissionsToOracle(String hash, String input, List<Submission> chosenSubmissions) {
        println 'MockQueueService sendSubmissionsToOracle'
    }

}

