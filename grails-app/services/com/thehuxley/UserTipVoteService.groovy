package com.thehuxley

import grails.transaction.Transactional
import org.hibernate.SessionFactory
import org.hibernate.transform.Transformers
import org.springframework.context.i18n.LocaleContextHolder

@Transactional
class UserTipVoteService {

    SessionFactory sessionFactory

    def save(UserTipVote userTipVote) {
        userTipVote.save(flush: true)
    }

    def upvotes(Long testCaseId) {
        return sessionFactory.currentSession.createSQLQuery("select count(*) from user_tip_vote where useful=true and test_case_id= :testCase ;").setParameter("testCase", testCaseId).uniqueResult()
    }

    def downvotes(Long testCaseId) {
        return sessionFactory.currentSession.createSQLQuery("select count(*) from user_tip_vote where useful=false and test_case_id= :testCase ;").setParameter("testCase", testCaseId).uniqueResult()
    }

    def findBadTips(User user) {
        def isAdmin = user.authorities.contains('ROLE_ADMIN')

        def sql = """SELECT  p.id as problem_id,pi18n.name,utv.test_case_id,
                            SUM(case when useful then 1 else 0 end) as useful,
                            SUM(case when useful then 0 else 1 end) as not_useful
                        FROM user_tip_vote utv
                        JOIN test_case tc ON tc.id = utv.test_case_id
                        JOIN problem p ON tc.problem_id = p.id
                        JOIN problem_i18n pi18n ON pi18n.problem_id = p.id
                        WHERE pi18n.locale = :locale
                        'user_condition'
                        GROUP BY utv.test_case_id,p.id,pi18n.name
                        HAVING SUM(case when useful then 1 else -1 end) < 1"""

        if (isAdmin) {
            sql = sql.replace("'user_condition'", "")
        } else {
            sql = sql.replace("'user_condition'", "AND p.user_suggest_id = " + user.id)
        }

        return sessionFactory.currentSession.createSQLQuery(sql)
            .setParameter("locale", LocaleContextHolder.getLocale().toString())
            .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
            .setMaxResults(10)
            .list()
    }
}
