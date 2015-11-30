package com.hrunk.minirest;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class StringRestResponse extends RestResponse {

    private String string;

    public String getString() {
        return string;
    }

    void readResponse(InputStream is, String encoding) throws IOException, RestException {
        byte[] bytes = Rest.readWholeStream(is);
        string = new String(bytes, encoding);
    }

    @Override
    public void throwOnError() {
        if (getStatus() / 100 != 2) {
            throw new HttpRestException(getStatus(), string);
        }
    }

}
