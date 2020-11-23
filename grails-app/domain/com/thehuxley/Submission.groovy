package com.thehuxley

import groovy.transform.EqualsAndHashCode
import net.kaleidos.hibernate.usertype.ArrayType
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.NullOutputStream

import java.security.DigestInputStream
import java.security.MessageDigest

@EqualsAndHashCode(includes = 'id')
class Submission implements Serializable {

    def grailsApplication

    enum Evaluation {
        CORRECT, //0
        WRONG_ANSWER, //1
        RUNTIME_ERROR, //2
        COMPILATION_ERROR, //3
        EMPTY_ANSWER, //4
        TIME_LIMIT_EXCEEDED, //5
        WAITING, //6
        EMPTY_TEST_CASE, //7
        WRONG_FILE_NAME, //8
        PRESENTATION_ERROR, //9
        HUXLEY_ERROR, //10
        FORK_BOMB //11
    }

    Double time = -1
    Integer tries = 0
    String originalFilename
    String filename
    String comment
    String contentHash
    Date submissionDate = new Date()
    Evaluation evaluation = Evaluation.WAITING

    int correctTestCases = 0
    int totalTestCases = -1

    User user
    Problem problem
    Language language

    static hasMany = [parts: CodePartSubmission, testCaseEvaluations: TestCaseEvaluation]

    Long[] choices

    static searchable = {
        only = ["time", "tries", "submissionDate", "evaluation"]
        user parent: true, component: true
        problem parent: true, component: true
        language parent: true, component: true
    }

    static constraints = {
        originalFilename blank: false, nullable: true
        filename blank: false, nullable: true
        tries nullable: false
        submissionDate nullable: false
        comment nullable: true
        choices nullable: true
        language nullable: true
        contentHash nullable: true
    }

    static mapping = {
        id generator: "sequence", params: [sequence: "submission_id_seq"]
        comment type: "text"
        problem cascade: "evict"
        testCaseEvaluations cascade: "all-delete-orphan"
        choices type: ArrayType, params: [type: Long]
    }

    def diff() {
        testCaseEvaluations.find { it.diff != null }?.diff
    }

    def errorMsg() {
        testCaseEvaluations.find { it.errorMsg != null }?.errorMsg
    }

    def getSourceCode() {
        def file = new File(grailsApplication.config.huxleyFileSystem.base + filename)

        if (file.exists()) {
            return file.getText('UTF-8')
        } else {
            log.warn("submission-file-not-found ${file.getAbsolutePath()}")
            return null
        }
    }

    def saveFile(InputStream inputStream) {
        def base = grailsApplication.config.huxleyFileSystem.base

        def now = new Date()

        def filePath = File.separator +
                now[Calendar.YEAR] +
                File.separator +
                (now[Calendar.MONTH] + 1) +
                File.separator +
                now[Calendar.DAY_OF_MONTH] +
                File.separator

        new File(base + filePath).mkdirs()

        def generatedFilename = UUID.randomUUID().toString()

        while (new File(base + filePath + generatedFilename).exists()) {
            generatedFilename = UUID.randomUUID().toString()
        }

        byte[] byteArray = IOUtils.toByteArray(inputStream)

        FileUtils.writeByteArrayToFile(new File(base + filePath + generatedFilename), byteArray)

        filename = filePath + generatedFilename

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1")

        DigestInputStream digestInputStream = new DigestInputStream(new ByteArrayInputStream(byteArray), sha1)

        try {
            IOUtils.copy(digestInputStream, new NullOutputStream())

            contentHash = Hex.encodeHexString(sha1.digest())
        } finally {
            digestInputStream.close()
        }
    }

    def deleteFile() {
        def base = grailsApplication.config.huxleyFileSystem.base

        def file = new File(base + filename)

        if (file.exists()) {
            file.delete()
        }
    }

}

@EqualsAndHashCode(includes = 'submission, testCase')
class TestCaseEvaluation implements Serializable {

    Submission.Evaluation evaluation = Submission.Evaluation.WAITING
    Double time = -1
    String errorMsg
    String diff

    static belongsTo = [submission: Submission, testCase: TestCase]

    static mapping = {
        table 'test_case_evaluation'
        id composite: ['submission', 'testCase']
        submission insertable: false, updateable: false
        testCase insertable: false, updateable: false
        status enumType: "ordinal"
        version false
    }

    static constraints = {
        errorMsg nullable: true
        diff nullable: true
    }

}

class CodePartSubmission implements Serializable {

    int lineNumber
    long problemId
    String code

    static belongsTo = [submission: Submission]

    static mapping = {
        id generator: "sequence", params: [sequence: "submission_id_seq"]
        code nullable: false
        problemId nullable: false
        submission column: 'submission_id'
        lineNumber nullable: false
        version false
    }
}