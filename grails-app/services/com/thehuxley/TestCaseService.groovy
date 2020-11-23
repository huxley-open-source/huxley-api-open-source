package com.thehuxley

import com.google.common.base.Strings
import com.google.common.cache.CacheBuilder
import grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.SessionFactory
import org.hibernate.transform.Transformers
import org.springframework.context.i18n.LocaleContextHolder

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TestCaseService {

    static long LARGE_TEST_CASE_SIZE = 2024;

    def problemStatsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .build()

    def grailsApplication

    SessionFactory sessionFactory

    def submissionService
    def springSecurityService

    def save(TestCase testCase) {
        problemStatsCache.invalidateAll()

        def input = testCase.input
        def output = testCase.output

        if (input.length() + output.length() > LARGE_TEST_CASE_SIZE) {
            testCase.input = ""
            testCase.output = ""
            testCase.large = true
        }

        testCase.save()

        if (!testCase.id) {
            return testCase;
        }

        createTestCaseFiles(testCase.id, testCase.problemId, input, output)

        return testCase
    }

    def update(TestCase testCase) {
        save(testCase)
    }

    def delete(long testCaseId) {

        def testCase = TestCase.get(testCaseId)

        if (testCase == null) throw new IllegalArgumentException("testCaseId");

        def problem = Problem.get(testCase.problemId);
        def testCases = TestCase.findAllByProblem(problem);
        def examplesCount = 0;
        testCases.each {
            if (it.example) examplesCount++
        }

        if (problem.status == Problem.Status.ACCEPTED && (testCases.size() < 2 || (testCase.example && examplesCount == 1))) {
            throw new IllegalArgumentException("example");
        }

        TestCaseEvaluation.findAll("FROM TestCaseEvaluation WHERE test_case_id = ?", testCaseId).each {
            it.delete()
        }

        UserTipVote.findAll("FROM UserTipVote WHERE testCaseId = ?", testCaseId).each {
            it.delete()
        }

        testCase.delete()

        String path = grailsApplication.config.huxleyFileSystem.problem.testCases.dir

        Files.deleteIfExists(Paths.get(path, problem.id.toString(), testCaseId + ".in"))
        Files.deleteIfExists(Paths.get(path, problem.id.toString(), testCaseId + ".out"))
    }

    def findByProblem(TestCase testCase, Problem problem, Boolean exampleOnly = false) {
        if (testCase.problem.id == problem.id) {
            if (exampleOnly) {
                if (testCase.example) {
                    testCase
                }
            } else {
                testCase
            }
        }
    }

    def findAllByProblem(Problem problem, Map params, Boolean exampleOnly = false) {
        TestCase.createCriteria().list(params) {
            eq("problem", problem)
            if (exampleOnly) {
                eq("example", true)
            }
        }
    }

    def findByProblemInputPlainText(TestCase testCase, Problem problem, Boolean exampleOnly = false) {
        if (testCase.problem.id == problem.id) {
            if (exampleOnly) {
                if (testCase.example) {
                    return testCase.input
                }
            } else {
                return testCase.input
            }
        }
    }

    def findByProblemOutputPlainText(TestCase testCase, Problem problem, Boolean exampleOnly = false) {
        if (testCase.problem.id == problem.id) {
            if (exampleOnly) {
                if (testCase.example) {
                    return testCase.output
                }
            } else {
                return testCase.output
            }
        }
    }

    def getTestCaseList(Problem problem) {
        def resultList = TestCase.createCriteria().list([:]) {
            createAlias("i18ns", "i18ns")
            eq("i18ns.locale", LocaleContextHolder.getLocale().toString())
            eq("problem", problem)
        }

        resultList.each {
            it.input = null
            it.output = null
            it.discard()
        }
    }

    def testCaseNeedingTips() {

        def needing_tips = problemStatsCache.getIfPresent("problem_needing_tips");

        if (!needing_tips) {

            def query = """SELECT s.problem_id, p.user_suggest_id as user_id, tce.test_case_id,count(*) as failures
                        FROM submission as s
                        INNER JOIN test_case_evaluation tce on (tce.submission_id = s.id)
                        INNER JOIN test_case tc on tc.id = tce.test_case_id
                        INNER JOIN test_case_i18n tci18n on tci18n.test_case_id = tc.id
                        INNER JOIN problem p ON p.id = s.problem_id
                        WHERE 1 = 1
                        AND tce.evaluation != 0
                        and tc.example = false
                        and tci18n.tip is null
                        GROUP BY s.problem_id, p.user_suggest_id, tce.test_case_id
                        ORDER BY failures desc"""

            needing_tips = sessionFactory.currentSession.createSQLQuery(query)
                    .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                    .list();

            problemStatsCache.put("problem_needing_tips", needing_tips);
        }

        def result = []

        for (int i = 0; i < 15; i++) {
            def it = needing_tips[i]
            // cria um cópia para não ter problema de concorrencia, já que sera alterado o atributo name
            result.add([
                    problem_id: it.problem_id,
                    user_suggest_id: it.user_suggest_id,
                    test_case_id: it.test_case_id,
                    failures: it.failures
            ])
        }

        def currentUser = springSecurityService.currentUser as User ?: null

        boolean isAdmin = false

        if (currentUser) {
            isAdmin = currentUser.authorities.authority.contains("ROLE_ADMIN")
        }

        if (!isAdmin) {
            result.removeIf({ it.user_id != currentUser.id })
        }

        if (!result.isEmpty()) {
            def names = sessionFactory.currentSession
                    .createSQLQuery("""SELECT p.id as problem_id,pi18n.name as name FROM problem p
                                        JOIN problem_i18n pi18n on pi18n.problem_id = p.id and pi18n.locale = :locale
                                        WHERE p.id in :problems""")
                    .setParameterList("problems", result*.problem_id)
                    .setParameter("locale", LocaleContextHolder.getLocale().toString())
                    .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                    .list();

            def nameMap = [:]
            names.each {
                nameMap.put(it.problem_id, it.name)
            }

            result.each {
                it.name = nameMap.get(it.problem_id)
            }
        }

        return result
    }

    def rewriteTestCaseFiles() {

        def session = sessionFactory.openStatelessSession()
        session.beginTransaction()

        def offset = 0

        def testCases = session.createQuery("FROM TestCase ORDER BY id")
                .setFirstResult(offset)
                .setMaxResults(20)
                .list()

        // Feito de 20 em 20 para não dar OutOfMemory
        while (!testCases.isEmpty()) {

            testCases.each { tc ->
                log.info("rewriting $tc")

                if (tc.large) {
                    log.info("ignoring-large-test-case")
                    return
                }

                if (Strings.isNullOrEmpty(tc.input) ) {
                    log.warn("null-test-input")
                    return
                }
                createTestCaseFiles(tc.id, tc.problemId, tc.input, tc.output)
            }

            offset += 20
            testCases = session.createQuery("FROM TestCase ORDER BY id")
                    .setFirstResult(offset)
                    .setMaxResults(20)
                    .list()
        }

        session.getTransaction().rollback()
        session.close()
    }

    def zip(TestCase testCase, OutputStream out) {

        log.info("zip-testcase: $testCase.id")

        ZipOutputStream zos = new ZipOutputStream(out)

        String path = grailsApplication.config.huxleyFileSystem.problem.testCases.dir
        File dir = Paths.get(path, testCase.getProblemId().toString()).toFile()

        ZipEntry input = new ZipEntry("${testCase.id}.in")
        zos.putNextEntry(input)

        if (!testCase.large && testCase.input) {
            zos.write(testCase.input.getBytes("UTF-8"))
        } else {
            zos.write(new File(dir, testCase.id + ".in").getText("UTF-8").getBytes("UTF-8"))
        }

        zos.closeEntry()

        ZipEntry output = new ZipEntry("${testCase.id}.out");
        zos.putNextEntry(output)

        if (!testCase.large && testCase.output) {
            zos.write(testCase.output.getBytes("UTF-8"))
        } else {
            zos.write(new File(dir, testCase.id + ".out").getText("UTF-8").getBytes("UTF-8"))
        }

        zos.closeEntry()

        if (testCase.tip()) {

            ZipEntry tip = new ZipEntry("${testCase.id}.tip");

            zos.putNextEntry(tip)
            zos.write(testCase.tip().getBytes("UTF-8"))
            zos.closeEntry()
        }

        zos.close()

        return zos
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)
        params.q = params.q ?: ""

        return params
    }

    boolean isSortable(param) {
        [
                "dateCreated",
                "lastUpdated",
                "example",

        ].contains(param)
    }

    private void createTestCaseFiles(long testCaseId, Long problemId, String input, String output) {
        String path = grailsApplication.config.huxleyFileSystem.problem.testCases.dir
        File dir = Paths.get(path, problemId.toString()).toFile()
        dir.mkdirs()

        replaceFile(new File(dir, testCaseId + ".in"), input)
        replaceFile(new File(dir, testCaseId + ".out"), output)
    }

    private void replaceFile(File file, String content) {
        if (file.exists()) {
            file.delete()
        }

        file.createNewFile()
        file << content
    }

}