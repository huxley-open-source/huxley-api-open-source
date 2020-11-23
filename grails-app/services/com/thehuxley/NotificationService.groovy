package com.thehuxley

import grails.plugins.mail.MailService
import org.apache.commons.lang.LocaleUtils
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NotificationService {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    def grailsApplication
    MessageSource messageSource
    MailService mailService

    def notify(User author, Group group, boolean allMembers, NotificationPreferences.Type type, params) {


        def preferences = NotificationPreferences.executeQuery(
                "FROM NotificationPreferences WHERE user in " +
                        "(SELECT user FROM UserGroup WHERE group = :group AND (:allMembers = true OR role != :role))",
                ["group": group, "allMembers": allMembers, "role": UserGroup.Role.STUDENT])

        sendNotification(preferences, type, params, author)

    }

    def notify(User author, List<Long> userIds, NotificationPreferences.Type type, params) {
        def preferences = NotificationPreferences.where {
            user.id in userIds && notificationType == type
        }.list()

        sendNotification(preferences, type, params, author)
    }

    private void sendNotification(List<NotificationPreferences> preferences, NotificationPreferences.Type type, params, User author) {

        Set emails = [];

        preferences.removeIf({
            it.user.id == author.id
        })

        preferences.each { NotificationPreferences np ->
            if (np.notificationType == type && np.email && np.user.email) {
                emails.add(np.user.email)
            }
        }

        if (emails.isEmpty()) return

        log.info "send-notification: { emails: ${emails.size()}, type: ${type} }"

        def locale = LocaleUtils.toLocale(author.locale)

        def subjectString = messageSource.getMessage("notification.email.subject." + type.toString(), params.toArray(), locale)
        def bodyString = messageSource.getMessage("notification.email.body." + type.toString(), params.toArray(), locale)

        bodyString += messageSource.getMessage("notification.email.signature", null, locale)

        EXECUTOR.submit({
            mailService.sendMail {
                from author.name + '<' + author.email + '>'
                (emails.size() < 2) ?: to(emails.toArray())
                emails.size() > 1 ?: bcc(emails.toArray())
                subject subjectString
                html bodyString
            }
        })

    }

}
