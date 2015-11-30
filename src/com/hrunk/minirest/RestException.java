package com.hrunk.minirest;

/**
 */
public class RestException extends RuntimeException {

    public RestException() {

    }

    public RestException(String message) {
        super(message);
    }
}
