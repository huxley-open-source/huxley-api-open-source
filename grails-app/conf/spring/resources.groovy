package spring

import com.thehuxley.CustomUserDetailsService
import com.thehuxley.PlagiarismConsumer
import com.thehuxley.error.HuxleyExceptionResolver
import com.thehuxley.marshaller.CustomMarshallerRegistrar
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import grails.util.Environment

// Place your Spring DSL code here
beans = {

    PlagiarismConsumer.rabbitConfig["queue"] = grailsApplication.config.plagiarism.username

    localeResolver(SessionLocaleResolver) {
        defaultLocale = new java.util.Locale('pt', 'BR')
        java.util.Locale.setDefault(defaultLocale)
    }

    publicMarshallerRegistrar(CustomMarshallerRegistrar) {
        avatarUrl = "${grailsApplication.config.huxley.avatarURL}"
        institutionLogoUrl = "${grailsApplication.config.huxley.baseURL}/api/v1/institutions/logo"
    }

    exceptionHandler(HuxleyExceptionResolver) {
        exceptionMappings = ['java.lang.Exception': '/error']
    }

    userDetailsService(CustomUserDetailsService)

    Environment.executeForCurrentEnvironment {
        development {
            // In development we use messageLocalService as implementation
            // of MessageService.
            springConfig.addAlias 'queueService', 'mockQueueService'
        }
    }

    localeResolver(com.thehuxley.locale.UserOrDefaultLocaleResolver) {
        security = springSecurityService
    }

}
