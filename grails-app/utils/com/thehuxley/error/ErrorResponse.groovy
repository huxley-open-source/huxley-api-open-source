package com.thehuxley.error

import org.springframework.http.HttpStatus

class ErrorResponse {

	HttpStatus httpStatus
	String status
	String reason
	List<ErrorReason> errors

	ErrorResponse(HttpStatus httpStatus, List<ErrorReason> errors) {
		this.httpStatus = httpStatus
		this.status =  httpStatus.value()
		this.reason = httpStatus.reasonPhrase
		this.errors = errors
	}

}
