package com.thehuxley.expcetion

class DoNotLogException extends RuntimeException {

    DoNotLogException(String message) {
        super(message)
    }

    DoNotLogException(String message, Throwable stackTrace) {
        super(message, stackTrace)
    }

}
