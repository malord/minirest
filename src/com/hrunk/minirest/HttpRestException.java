package com.hrunk.minirest;

/**
 */
public class HttpRestException extends RestException {

    private int status;

    public HttpRestException(int status) {
        super("HTTP " + status);
        this.status = status;
    }

    public HttpRestException(int status, String message) {
        super("HTTP " + status + ": " + message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
