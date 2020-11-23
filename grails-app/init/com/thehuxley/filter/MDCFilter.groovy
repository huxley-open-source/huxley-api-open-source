package com.thehuxley.filter

import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

import javax.annotation.Priority
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by marcio on 05/04/17.
 */
@Component
@Priority(Integer.MAX_VALUE)
class MDCFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain filterChain) throws ServletException, java.io.IOException {

        def auth = SecurityContextHolder.context?.authentication
        MDC.put("username", auth?.name)
        MDC.put("url", req.getRequestURI())
        MDC.put("query", req.getQueryString())

        filterChain.doFilter(req, resp)
    }
}
