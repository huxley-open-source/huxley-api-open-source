package com.thehuxley

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.hibernate.SessionFactory
import org.hibernate.transform.Transformers
import org.joda.time.LocalDate
import org.springframework.context.i18n.LocaleContextHolder

import java.util.concurrent.TimeUnit

class MessageService {

    final Cache<Long, Integer> messageCount = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build()

    def findAllByGroup(Group group, Map params) {
        Message.findAllByGroupAndDeleted(group, false, params)
    }

    SessionFactory  sessionFactory

    def findAllGrouped(User user, Map params) {

        def sql = """SELECT mg.*,
                            m.date_created as dateCreated,
                            m.body, m.sender_id as "senderId",
                            last_message.user_msg_count,
                            last_message.total_msg_count,
                            last_message.first_message_date,
                            mve.last_view_millis
                    FROM message_group mg
                      JOIN (SELECT message_group_id,
                                max(m.date_created) as date_created,
                                min(m.date_created) as first_message_date,
                                sum(case when sender_id != :user then 0 else 1 end) as user_msg_count,
                                count(*) as total_msg_count
                                FROM message m
                                JOIN message_group mg on m.message_group_id = mg.id
                                GROUP BY message_group_id) as last_message
                      ON mg.id = last_message.message_group_id
                      JOIN message m ON m.message_group_id = mg.id AND m.date_created = last_message.date_created
                      FULL JOIN message_view_event mve ON mg.id =  mve.message_group_id and mve.user_id = :user
                    WHERE (mg.user_id = :user
                    OR mg.group_id IN (SELECT group_id FROM user_group WHERE user_id = :user AND role != :student)
                    OR (mg.problem_id IN (SELECT id FROM problem WHERE user_suggest_id = :user))
                    OR mg.recipient_id = :user)
                    'query_filters'
                    'group_filters'
                    ORDER BY m.date_created desc"""

        if (params.filter == 'unresolved') {
            sql = sql.replace("'query_filters'", "AND message_status = " + MessageGroup.Status.UNRESOLVED.ordinal());
        } else if (params.filter == 'unanswered') {
            sql = sql.replace("'query_filters'", "AND user_msg_count = 0");
        } else if (params.filter == 'unarchived') {
            sql = sql.replace("'query_filters'", "AND mg.user_id = m.sender_id AND message_status = " + MessageGroup.Status.UNRESOLVED.ordinal());
        } else if (params.filter == 'sent') {
            sql = sql.replace("'query_filters'", "AND mg.user_id = :user");
        } else {
            sql = sql.replace("'query_filters'", "");
        }

        if (params.filter == 'group') {
            sql = sql.replace("'group_filters'", "AND group_id = " + (params.filterId as Long));
        } else if (params.filter == 'problem') {
            sql = sql.replace("'group_filters'", "AND problem_id = " + (params.filterId as Long));
        } else if (params.filter == 'user') {
            sql = sql.replace("'group_filters'", "AND mg.user_id = " + (params.filterId as Long));
        } else {
            sql = sql.replace("'group_filters'", "");
        }

        return sessionFactory.currentSession.createSQLQuery(sql)
                .setParameter("user", user.id)
                .setParameter("student", UserGroup.Role.STUDENT.ordinal())
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(params.offset ? params.offset as int : 0)
                .setMaxResults(10)
                .list();
    }

    def messageStats(User user) {
        def sql = """SELECT min(mg.message_status) as "messageStatus",
                            g.id as "groupId", g.name as "groupName",
                            p.id as "problemId",max(pi18n.name) as "problemName",
                            u.id as "userId", u.name as "userName",
                            count(*) as "messageCount",
                            sum(case when sender_id = :user then 1 else 0 end) as sent_count,
                            max(m.date_created) as last_message_date,
                            max(mvw.last_view_millis) as last_view,
                            mvw.message_group_id
                    FROM message m
                    JOIN message_group mg ON m.message_group_id = mg.id
                    FULL JOIN public.group g on g.id = mg.group_id
                    FULL JOIN problem p on p.id = mg.problem_id
                    FULL JOIN problem_i18n pi18n on (pi18n.problem_id = p.id and pi18n.locale = :locale)
                    FULL JOIN public.user u on u.id = mg.user_id
                    FULL JOIN message_view_event mvw ON mg.id = mvw.message_group_id and mvw.user_id = :user
                    WHERE 	mg.user_id 	= :user
                        OR 	mg.group_id 	IN (SELECT group_id FROM user_group WHERE user_id = :user AND role != :student)
                        OR (mg.problem_id IN (SELECT id FROM problem WHERE user_suggest_id = :user))
                        OR 	mg.recipient_id = :user
                        OR 	m.sender_id 	= :user
                    GROUP BY g.id,p.id,u.id, mvw.message_group_id
                    ORDER BY max(m.date_created) desc"""

        def msgs = sessionFactory.currentSession.createSQLQuery(sql)
                    .setParameter("locale", LocaleContextHolder.locale.toString())
                    .setParameter("user", user.id)
                    .setParameter("student", UserGroup.Role.STUDENT.ordinal())
                    .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                    .list();


        def result = [
            unresolved: 0,
            responded: 0,
            sent: 0,
            direct: 0,
            unread: 0,
            total: 0,
            groups: [],
            problems: [],
            users: []
        ];

        def users = [:]
        def groups = [:]
        def problems = [:]

        msgs.each {

            result.total++

            if (it.messageStatus == MessageGroup.Status.UNRESOLVED.ordinal()) {
                result.unresolved++;
                updateCount(users, it.userId, it.userName)
                updateCount(problems, it.problemId, it.problemName)
                updateCount(groups, it.groupId, it.groupName)
            }
            if (user.id == it.userId as Long) result.sent++;
            if (it.sent_count > 0) result.responded++;
            if (!it.groupId && !it.problemId) result.direct++;

            def unread = !it.last_view || it.last_view < it.last_message_date.getTime();

            if (unread) {
                result.unread++
            }

        }

        result.users = users.values();
        result.problems = problems.values();
        result.groups = groups.values();

        return result;
    }

    def archiveOldMessages(User user, int days) {
        def date = new LocalDate().minusDays(days).toDate()

        def sql = """UPDATE message_group set message_status = :archived FROM
                    (SELECT mg.id
                    FROM message_group mg
                      JOIN (SELECT message_group_id,
                                max(m.date_created) as date_created
                                FROM message m
                                GROUP BY message_group_id) as last_message
                      ON mg.id = last_message.message_group_id
                    JOIN message m ON m.message_group_id = mg.id AND m.date_created = last_message.date_created
                    WHERE (mg.user_id = :user
                    OR mg.group_id IN (SELECT group_id FROM user_group WHERE user_id = :user AND role != :student)
                    OR (mg.problem_id IN (SELECT id FROM problem WHERE user_suggest_id = :user))
                    OR mg.recipient_id = :user)
                    AND mg.user_id != m.sender_id AND message_status = :unresolved
                    AND m.last_updated < :date) as toArchiveQuery
                    WHERE toArchiveQuery.id = message_group.id"""

        def result = sessionFactory.currentSession.createSQLQuery(sql)
                .setParameter("archived", MessageGroup.Status.ARCHIVED.ordinal())
                .setParameter("user", user.id)
                .setParameter("student", UserGroup.Role.STUDENT.ordinal())
                .setParameter("unresolved", MessageGroup.Status.UNRESOLVED.ordinal())
                .setParameter("date", date)
                .executeUpdate()

        messageCount.invalidate(user.id)

        return result
    }

    private void updateCount(statMap, id, name) {
        if (!id) return;

        def stat = statMap[id];
        if (!stat) {
            stat = [ id: id, name: name, count: 0 ]
            statMap[id] = stat;
        }

        stat.count++;
    }

    def saveMessageGroupViewEvent(long userId, long messageGroupId) {
        def viewEvent = MessageViewEvent.findByUserIdAndMessageGroupId(userId, messageGroupId)

        long now = System.currentTimeMillis()

        if (viewEvent) {
            viewEvent.lastViewMillis = now
        } else {
            viewEvent = new MessageViewEvent([ userId: userId, messageGroupId: messageGroupId, lastViewMillis: now ])
        }

        viewEvent = viewEvent.merge()

        messageCount.invalidate(userId)

        return viewEvent
    }

    def cleanCountCache(usersIds) {
        usersIds.each {
            messageCount.invalidate(it)
        }
    }

    def findAllByRecipient(User user, Map params) {

        Message.createCriteria().list(params) {
            and {

                if (params.senderId) {
                    or {
                        eq("senderId", params.senderId as Long)
                        eq("recipientId", params.senderId as Long)
                    }
                }

                !params.problemId ?: eq("problemId", params.problemId as Long)
                !params.groupId ?: eq("groupId", params.groupId as Long)
                !params.recipientId ?: eq("recipientId", params.recipientId as Long)

                order(params.sort ?: 'dateCreated', params.order ?: 'desc')
            }
        }
    }

    def countUnread(User user) {

        if (!user) {
            return 0
        }

        def count = messageCount.getIfPresent(user.id)

        if (count == null) {
            count = sessionFactory.currentSession.createSQLQuery("""
                            SELECT COUNT(DISTINCT mg.id) FROM message m
                            JOIN message_group mg ON m.message_group_id = mg.id
                            FULL JOIN message_view_event mve
                            ON mve.message_group_id = m.message_group_id AND mve.user_id = :user
                            WHERE
                             mg.message_status != :archived
                            AND (
                              mg.user_id = :user
                              OR mg.recipient_id = :user
                              OR mg.problem_id in (SELECT id from problem where user_suggest_id = :user)
                              OR mg.group_id IN (SELECT group_id FROM user_group WHERE user_id = :user  and role != 0))
                            AND (
                              EXTRACT(EPOCH FROM m.date_created AT TIME ZONE 'America/Maceio') * 1000 > mve.last_view_millis
                              OR mve.last_view_millis IS NULL)""")
                    .setParameter("user", user.id)
                    .setParameter("archived", MessageGroup.Status.ARCHIVED.ordinal())
                    .uniqueResult()

            messageCount.put(user.id, count)
        }

        return count
    }

    def save(Message message) {
        message.save(flush: true)
    }

    def delete(Message message) {
        message.deleted = true

        message.save()
    }

}
