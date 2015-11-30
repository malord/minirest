package com.hrunk.minirest;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class ByteArrayRestResponse extends RestResponse {

    private byte[] bytes;

    public byte[] getByteArray() {
        return bytes;
    }

    void readResponse(InputStream is) throws IOException, RestException {
        bytes = Rest.readWholeStream(is);
    }

    @Override
    public void throwOnError() {
        if (getStatus() / 100 != 2) {
            throw new HttpRestException(getStatus());
        }
    }

}
