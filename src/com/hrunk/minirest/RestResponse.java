package com.hrunk.minirest;

/**
 */
public class RestResponse {

    int status;

    RestResponse() {
    }

    public int getStatus() {
        return status;
    }

    public void throwOnError() {
        if (status / 100 != 2) {
            throw new HttpRestException(status);
        }
    }
}
