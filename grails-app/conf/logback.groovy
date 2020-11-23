import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.boolex.OnErrorEvaluator
import ch.qos.logback.classic.net.SMTPAppender
import com.thehuxley.logging.DuplicateMessageNormalFilter
import grails.util.Environment

import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %X{username} %X{url} %X{query} [%thread] %-5level %logger{36} - %msg%n"
    }
}

//logger("org.springframework.security", DEBUG, ['STDOUT'], false)
//logger("grails.plugin.springsecurity", DEBUG, ['STDOUT'], false)
//logger("org.pac4j", DEBUG, ['STDOUT'], false)

if (Environment.isDevelopmentMode()) {
    root(INFO, ['STDOUT'])
} else {
    appender('FILE_LOG', RollingFileAppender) {
        append = "true"
        file = "/home/huxley/data/log/huxley-api.log"

        rollingPolicy(TimeBasedRollingPolicy) {
            fileNamePattern = "/home/huxley/data/log/huxley-api.%d{dd-MM-yyyy}.log.gz"
        }

        encoder(PatternLayoutEncoder) {
            pattern = "%d{HH:mm:ss.SSS} %X{username} %X{url} %X{query} [%thread] %-5level %logger{36} - %msg%n"
        }

    }

    if (Environment.getCurrent() == Environment.PRODUCTION) {
        appender('EMAIL', SMTPAppender) {
            smtpHost = 'smtp-relay.sendinblue.com'
            smtpPort = 587
            username = ''
            password = ''
            to = 'marcio@zuq.com.br'
            to = 'support@thehuxley.com'
            from = 'erro@thehuxley.com'
            subject = 'Erro The Huxley %logger{20} - %m'
            evaluator(OnErrorEvaluator)
            filter(DuplicateMessageNormalFilter)
            layout(PatternLayout) {
                pattern = "%d{HH:mm:ss.SSS} %X{username} %X{url} %X{query} [%thread] %-5level %logger{36} - %msg%n"
            }

        }
        root(INFO, ['FILE_LOG', 'EMAIL'])
        logger("org.springframework.web.multipart.MultipartException", INFO, ['FILE_LOG'])
    } else {
        root(INFO, ['FILE_LOG'])
    }
}
