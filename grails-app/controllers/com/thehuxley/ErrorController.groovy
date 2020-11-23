package com.thehuxley

import com.thehuxley.error.ErrorReason
import com.thehuxley.error.ErrorResponse
import org.springframework.http.HttpStatus

class ErrorController {

    static responseFormats = ['json']
    static allowedMethods = []

    def index() {}

    def badRequest() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.GENERIC])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def wrongOrderParam() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.WRONG_ORDER_PARAM])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def wrongSortParam() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.WRONG_SORT_PARAM])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def entityNotFound() {
        def errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND, [ErrorReason.ENTITY_NOT_FOUND])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def passwordWrong() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.USER_PASSWORD_WRONG])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def passwordInvalid() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.USER_PASSWORD_INVALID])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def passwordNotMatch() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.USER_PASSWORD_NOT_MATCH])

        respond errorResponse, [status: errorResponse.httpStatus]
    }


    def invalidAvatarSize() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.AVATAR_INVALID_SIZE])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def invalidAvatarMimeType() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.AVATAR_INVALID_MIME_TYPE])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def invalidLogoSize() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.LOGO_INVALID_SIZE])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def invalidLogoMimeType() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.LOGO_INVALID_MIME_TYPE])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def invalidProblemImageSize() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.PROBLEM_IMAGE_INVALID_SIZE])

        respond errorResponse, [status: errorResponse.httpStatus]
    }

    def invalidProblemImageMimeType() {
        def errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, [ErrorReason.PROBLEM_IMAGE_INVALID_MIME_TYPE])

        respond errorResponse, [status: errorResponse.httpStatus]
    }
}
