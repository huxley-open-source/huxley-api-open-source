package com.thehuxley.error

import com.thehuxley.expcetion.DoNotLogException
import net.sf.jmimemagic.MagicMatchNotFoundException
import org.apache.tomcat.util.http.fileupload.FileUploadBase
import org.grails.web.errors.GrailsExceptionResolver
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HuxleyExceptionResolver extends GrailsExceptionResolver {

    @Override
    ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception ex) {
        if (!ex instanceof AccessDeniedException &&
                !ex instanceof FileUploadBase.SizeLimitExceededException &&
                !ex instanceof MagicMatchNotFoundException &&
                !ex instanceof DoNotLogException) {
            super.resolveException(request, response, handler, ex)
        }        
    }

}
