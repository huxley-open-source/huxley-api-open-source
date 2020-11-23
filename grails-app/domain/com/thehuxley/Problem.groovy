package com.thehuxley

import groovy.transform.EqualsAndHashCode
import net.kaleidos.hibernate.usertype.ArrayType
import org.springframework.context.i18n.LocaleContextHolder

@EqualsAndHashCode(includes = 'id')
class Problem implements Serializable {

    enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    enum ProblemType {
        ALGORITHM,
        MULTIPLE_CHOICE,
        SINGLE_CHOICE,
        TRUE_OR_FALSE,
        FILL_THE_CODE
    }

    String source
    Integer level = 1
    Integer timeLimit = 1
    Double nd
    Date dateCreated
    Date lastUpdated
    Date lastUserUpdate
    Status status = Status.PENDING

    ProblemType problemType = ProblemType.ALGORITHM

    String baseCode
    Language baseLanguage
    Integer[] blankLines

    User userApproved
    User userSuggest

    Float userRank = 0
    Integer rankVotes = 0

    boolean quizOnly = false

    static hasMany = [topics: Topic, testCases: TestCase, submissions: Submission, i18ns: ProblemI18n, choices: ProblemChoice]

    static belongsTo = [Topic]

    static mapping = {
        id generator: "sequence", params: [sequence: "problem_id_seq"]
        status enumType: "ordinal"
        problemType enumType: "ordinal"
        blankLines type: ArrayType, params: [type: Integer]
        topics joinTable: [name: "topic_problems", key: "problem_id", column: "topic_id"], fetch: 'select'
        i18ns lazy: false, fetch: 'select'
        testCases lazy: false, fetch: 'select'
        choices lazy: false, fetch: 'select', cascade: "all-delete-orphan"
    }

    static constraints = {
        userApproved nullable: true
        userSuggest nullable: true
        source nullable: true
        level range: 1..10
        nd nullable: true
        lastUserUpdate nullable: true
        blankLines nullable: true
        baseCode nullable: true
        baseLanguage nullable: true
        choices nullable: true
        userRank nullable: true
        rankVotes nullable: true
    }

    def beforeInsert() {
        nd = level
    }

    def beforeUpdate() {
        if (!nd) {
            nd = level
        }
    }

    def exampleTestCases() {
        testCases.findAll { it.example }.sort { it.id }
    }

    def notExampleTestCases() {
        testCases.findAll { !it.example }.sort { it.id }
    }

    def name() {
        i18ns.find { it.locale.equals(LocaleContextHolder.getLocale().toString()) }?.name
    }

    def description() {
        i18ns.find { it.locale.equals(LocaleContextHolder.getLocale().toString()) }?.description
    }

    def inputFormat() {
        i18ns.find { it.locale.equals(LocaleContextHolder.getLocale().toString()) }?.inputFormat
    }

    def outputFormat() {
        i18ns.find { it.locale.equals(LocaleContextHolder.getLocale().toString()) }?.outputFormat
    }

}

@EqualsAndHashCode(includes = 'problem, locale')
class ProblemI18n implements Serializable {
    String locale
    String name
    String description
    String inputFormat
    String outputFormat
    Long userSuggest

    static belongsTo = [problem: Problem]

    static mapping = {
        table 'problem_i18n'
        id composite: ['problem', 'locale']
        problem insertable: false, updateable: false
        version false
    }

    static constraints = {
        inputFormat nullable: true
        outputFormat nullable: true
        userSuggest nullable: true
    }

}

@EqualsAndHashCode(includes = 'problemId, choiceOrder, locale')
class ProblemChoice implements Serializable {

    static hasMany = [i18ns: ProblemChoiceI18n]

    boolean correct = false
    int choiceOrder

    static belongsTo = [problem: Problem]

    static mapping = {
        id generator: "sequence", params: [sequence: "problem_id_seq"]
        table 'problem_choice'
        version false
    }

    def description() {
        i18ns.find { it.locale.equals(LocaleContextHolder.getLocale().toString()) }?.description
    }
}

@EqualsAndHashCode(includes = 'problemChoiceId, locale')
class ProblemChoiceI18n implements Serializable {

    String description
    String locale

    static belongsTo = [problemChoice: ProblemChoice]

    static mapping = {
        id composite: ['problemChoice', 'locale']
        table 'problem_choice_i18n'
        problemChoice insertable: false, updateable: false
        version false
    }
}
