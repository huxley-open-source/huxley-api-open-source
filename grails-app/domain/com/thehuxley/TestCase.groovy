package com.thehuxley

import groovy.transform.EqualsAndHashCode
import org.springframework.context.i18n.LocaleContextHolder

@EqualsAndHashCode(includes = 'id')
class TestCase implements Serializable {

    String input
    String output
    Date dateCreated
    Date lastUpdated
    Boolean example = false
    Boolean large   = false

    static belongsTo = [problem: Problem]

    static hasMany = [i18ns: TestCaseI18n]

    static mapping = {
        id generator: "sequence", params: [sequence: "test_case_id_seq"]
        i18ns lazy: false, fetchStrategy: 'select'
    }

    static constraints = {
        dateCreated nullable: true
        lastUpdated nullable: true
    }

    def tip() {
        i18ns.find { it.locale.equals(LocaleContextHolder.getLocale().toString()) }?.tip
    }

}

@EqualsAndHashCode(includes = 'testCase, locale')
class TestCaseI18n implements Serializable {

    String locale
    String tip

    static belongsTo = [testCase: TestCase]

    static mapping = {
        table 'test_case_i18n'
        id composite: ['testCase', 'locale']
        testCase insertable: false, updateable: false
        version false
    }

    static constraints = {
        tip nullable: true
    }

}