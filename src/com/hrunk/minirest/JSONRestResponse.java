package com.hrunk.minirest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class JSONRestResponse extends RestResponse {

    private JSONObject json;

    public JSONObject getJSONObject() {
        return json;
    }

    void readResponse(InputStream is) throws IOException, RestException, JSONException {
        byte[] bytes = Rest.readWholeStream(is);
        json = new JSONObject(new String(bytes, "UTF-8"));
    }

    @Override
    public void throwOnError() {
        if (getStatus() / 100 != 2) {
            throw new HttpRestException(getStatus(), getJSONObject().toString());
        }
    }

}
