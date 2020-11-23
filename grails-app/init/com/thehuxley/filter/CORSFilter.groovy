package com.thehuxley.filter

import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

import javax.annotation.Priority
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Priority(Integer.MIN_VALUE)
class CORSFilter extends OncePerRequestFilter {

    static def logger = LogFactory.getLog(CORSFilter)

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (logger.debugEnabled)
            logger.debug "Appling CORSFilter, Origin: ${request.getHeader("Origin")}, Method: ${request.method}."

        String origin = request.getHeader("Origin");

        boolean options = request.method == HttpMethod.OPTIONS.name()

        if (options) {

            if (origin == null) return

            response.addHeader("Access-Control-Allow-Headers", "origin, authorization, accept, content-type, x-requested-with, x-auth-token")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE")
            response.addHeader("Access-Control-Max-Age", "3600")

        }

        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Credentials", "false")
        response.addHeader("Access-Control-Expose-Headers", "total, TH-Date")
        response.addDateHeader("TH-Date", new Date().time)

        if (!options) filterChain.doFilter(request, response)

    }

}
