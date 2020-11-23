package com.thehuxley

import com.google.common.cache.RemovalCause
import com.google.common.cache.RemovalNotification
import org.hibernate.SessionFactory
import org.joda.time.LocalDate

import java.util.concurrent.TimeUnit

import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener

@Consumer
class TopCoderService {

    SessionFactory sessionFactory

    def removalListener = [ onRemoval: { RemovalNotification notification ->

        if (notification.getCause() == RemovalCause.EXPIRED) {
            def userId = notification.getKey()
            refreshUserTopCoder(User.load(userId))
        }
    }] as RemovalListener

    def usersToUpdate = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .removalListener(removalListener)
            .build()

    def queueToUpdate(userId) {
        usersToUpdate.put(userId, userId)
    }

    def refreshTopCoderV2() {

        TopCoder.executeUpdate('delete from TopCoder')
        usersToUpdate.invalidateAll()
        sessionFactory.currentSession.createSQLQuery(
                """INSERT INTO top_coder (user_id, points, position, version)
							SELECT uid, sum(nd * nd) as points, row_number() over (ORDER BY sum(nd * nd) desc), 1
							FROM (select s.user_id as uid, max(p.nd) as nd
									from submission s
									join problem p on p.id = s.problem_id
									WHERE s.evaluation = 0 AND p.problem_type = 0 AND p.quiz_only = false
									group by s.user_id,problem_id order by nd desc) AS allusers
							JOIN user_role ur on ur.user_id = uid
							JOIN role r on r.id = ur.role_id
							GROUP BY uid
							HAVING array_agg(distinct(r.authority)) = ARRAY[cast('ROLE_STUDENT' as varchar)]
							ORDER BY points desc""").executeUpdate();

    }

    def refreshUserTopCoder(User user) {
        log.info "Atualizando top coder. user.id = " + user.id
        
        sessionFactory.currentSession.createSQLQuery(
                """UPDATE top_coder set points = qr.points FROM
						(SELECT uid, sum(nd * nd) as points FROM
							(SELECT s.user_id as uid, max(p.nd) as nd
								FROM submission s
								JOIN problem p ON p.id = s.problem_id AND p.problem_type = 0 AND p.quiz_only = false
								WHERE s.evaluation = 0 and user_id = :user
								GROUP BY s.user_id,problem_id ORDER BY nd DESC)
								AS a GROUP BY uid)
							AS qr WHERE user_id = qr.uid;""")
                .setParameter("user", user.id)
                .executeUpdate();

        // Atualizar posições
        // TODO: é interessante atualizar as posições sempre que o usuário submeter? Talvez fazer isso apenas a cada 1h?

        String sql = """UPDATE top_coder SET position = qr.rc FROM (
							SELECT user_id as userid, row_number() OVER (ORDER BY points desc) as rc from top_coder order by points)
							AS qr
						WHERE user_id = qr.userid""";

        sessionFactory.currentSession.createSQLQuery(sql).executeUpdate();
    }

    @Selector('topcoder.updateNDs')
    def updateNds() {

        log.info("atualizando nds")

        def sql = """
            UPDATE problem p set nd = c.nd FROM
                (SELECT problem_id,correct,ceil(row_number() over (order by correct desc) / (count(*) OVER () / 5.0)) as nd FROM
                    (SELECT s.problem_id,
                        SUM(CASE WHEN s.evaluation = 0 then 1 else 0 end),
                        SUM(CASE WHEN s.evaluation = 0 then 1 else 0 end) / extract(day from (now() - s.date_created)) as correct,
                        date_created
                            FROM (
                                SELECT p.id as problem_id,s.evaluation,p.date_created
                                FROM problem p
                                FULL JOIN submission s on p.id = s.problem_id
                                WHERE p.date_created < :maxdate
                                    AND p.problem_type in (:types)
                                    AND p.quiz_only is false
                                    AND p.status = :accepted
                                GROUP BY p.id,s.evaluation,p.date_created,s.user_id) as s
                                GROUP BY s.problem_id,s.date_created) as d) as c
            WHERE p.id = c.problem_id;"""

        sessionFactory.currentSession.createSQLQuery(sql)
            .setParameter("maxdate", new LocalDate().minusMonths(3).toDate())
            .setParameterList("types", [Problem.ProblemType.ALGORITHM.ordinal(), Problem.ProblemType.FILL_THE_CODE.ordinal()])
            .setParameter("accepted", Problem.Status.ACCEPTED.ordinal())
            .executeUpdate()


        refreshTopCoderV2()

        log.info("Fim da atualizalção dos NDs.")

    }

}
