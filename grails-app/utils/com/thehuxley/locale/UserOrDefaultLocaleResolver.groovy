package com.thehuxley.locale

import com.thehuxley.User
import org.apache.commons.lang.LocaleUtils
import org.grails.web.util.WebUtils
import org.springframework.context.i18n.LocaleContext
import org.springframework.context.i18n.TimeZoneAwareLocaleContext
import org.springframework.web.servlet.i18n.SessionLocaleResolver

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

/**
 * Created by marcio on 20/01/17.
 */
class UserOrDefaultLocaleResolver extends SessionLocaleResolver {

    def security

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return getLocale(request);
    }

    @Override
    public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
        return new TimeZoneAwareLocaleContext() {
            @Override
            public Locale getLocale() {
                getLocale(request)
            }
            @Override
            public TimeZone getTimeZone() {
                TimeZone timeZone = (TimeZone) WebUtils.getSessionAttribute(request, TIME_ZONE_SESSION_ATTRIBUTE_NAME);
                if (timeZone == null) {
                    timeZone = determineDefaultTimeZone(request);
                }
                return timeZone;
            }
        };
    }

    private Locale getLocale(final HttpServletRequest request) {
        def cookies = request.getCookies()
        User user = (security.currentUser as User)


        def localeString = request.getParameter("locale")

        if (localeString) {
            try {
                return LocaleUtils.toLocale(localeString)
            } catch (IllegalArgumentException ex) {
                log.info("locale-not-found $localeString")
            }
        }

        Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);

        if (locale == null) {

            if (user) {
                locale = LocaleUtils.toLocale(user.locale);
            } else {
                try {
                    cookies.each { Cookie c ->
                        if(c.name == 'locale') {
                            locale = LocaleUtils.toLocale(c.value.trim())
                        }
                    }
                } catch (Exception e) {
                    log.error("error:", e)
                }
                if(!locale) {
                    locale = determineDefaultLocale(request);
                }
            }

        }

        return locale;
    }

}