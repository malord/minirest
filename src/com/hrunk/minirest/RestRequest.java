package com.hrunk.minirest;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * My attempt at a pleasant REST interface.
 */
public class RestRequest {
    private static final String TAG = "RestRequest";

    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String POST = "POST";

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String XML_CONTENT_TYPE = "text/xml";

    private String method;
    private URL url;
    private String authorization;
    private byte[] body = null;
    private HttpURLConnection http;
    private String query;
    private String accept;
    private String acceptCharset;
    private String contentType;
    private String userAgent = null;
    private RestResponse response;
    private boolean throwOnError = true;
    private boolean manualGunzip = false;

    // If there are any more HTTP headers, put them in a Map

    public RestRequest(String method, String url) throws IOException {
        this.method = method;

        this.url = new URL(url);
        query = this.url.getQuery();
        if (query == null) {
            query = "";
        } else {
            query = "?" + query;
        }
    }

    public RestRequest basicAuth(String user, String password) {
        try {
            authorization = Base64.encodeToString((user + ":" + password).getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "" + e);
        }
        return this;
    }

    public RestRequest param(String name, String value) {
        String encoded = Uri.encode(name) + "=" + Uri.encode(value);
        if (query.isEmpty()) {
            query += "?" + encoded;
        } else {
            query += "&" + encoded;
        }
        return this;
    }

    public RestRequest paramIfNotNull(String name, String value) {
        if (value != null) {
            return param(name, value);
        }

        return this;
    }

    public RestRequest body(JSONObject object) {
        try {
            body = object.toString().getBytes("UTF-8");
            contentType = JSON_CONTENT_TYPE;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "" + e);
        }
        return this;
    }

    private void connect(boolean willReceiveBody) throws IOException {
        URL url = new URL(buildURL());
        if (Rest.isTraceEnabled()) {
            Rest.trace("URL: " + url);
        }
        http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod(method);
        http.setDoOutput(body != null);
        http.setDoInput(willReceiveBody);
        if (authorization != null) {
            http.setRequestProperty("Authorization", "Basic " + authorization);
        }
        if (contentType != null) {
            http.setRequestProperty("Content-Type", contentType);
        }
        if (accept != null) {
            http.setRequestProperty("Accept", accept);
        }
        if (acceptCharset != null) {
            http.setRequestProperty("Accept-Charset", acceptCharset);
        }
        if (body != null) {
            http.setRequestProperty("Content-Length", "" + body.length);
        } else {
            http.setRequestProperty("Content-Length", "0");
        }
        String userAgent = this.userAgent != null ? this.userAgent : Rest.getUserAgent();
        if (userAgent != null) {
            http.setRequestProperty("User-Agent", userAgent);
        }
        if (url.getProtocol().equalsIgnoreCase("https")) {
            http.setRequestProperty("Accept-Encoding", "gzip");
            manualGunzip = true;
        } else {
            manualGunzip = false;
        }

        if (Rest.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder(256);
            for (Map.Entry<String, List<String>> entry : http.getRequestProperties().entrySet()) {
                sb.setLength(0);
                List<String> values = entry.getValue();
                for (int i = 0; i != values.size(); ++i) {
                    if (i != 0) {
                        sb.append(';');
                    }
                    sb.append(values.get(i));
                }
                Rest.trace("Request header: " + entry.getKey() + ": " + sb.toString());
            }
        }

        http.connect();

        if (body != null) {
            if (Rest.isTraceEnabled()) {
                String contentType = http.getRequestProperty("content-type");
                if (contentType != null && contentType.equalsIgnoreCase("application/json")) {
                    Rest.trace("Body: " + new String(body, "UTF-8"));
                }
            }
            OutputStream os = http.getOutputStream();
            os.write(body);
            os.flush();
        }

        response.status = http.getResponseCode();

        if (manualGunzip) {
            String encoding = http.getHeaderField("content-encoding");
            if (encoding == null || ! encoding.equalsIgnoreCase("gzip")) {
                manualGunzip = false;
            }
        }

        if (Rest.isTraceEnabled()) {
            Rest.log("Response: " + response.status + " for: " + url.toString());
            for (Map.Entry<String, List<String>> k : http.getHeaderFields().entrySet()) {
                for (String v : k.getValue()) {
                    Rest.trace("Response header: " + k.getKey() + ": " + v);
                }
            }
        } else if (Rest.getDebugLevel() >= Rest.DEBUG_ON) {
            if (response.status / 100 != 2) {
                Rest.log("Response: " + response.status + " for: " + url.toString());
            }
        }
    }

    private String buildURL() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(url.getProtocol());
        sb.append("://");
        sb.append(url.getHost());
        if (url.getPort() >= 0) {
            sb.append(':');
            sb.append(url.getPort());
        }
        if (url.getPath() != null) {
            sb.append(url.getPath());
        } else {
            sb.append('/');
        }
        sb.append(query);
        return sb.toString();
    }

    public ByteArrayRestResponse asByteArray() throws IOException, RestException {
        ByteArrayRestResponse bytesResponse = new ByteArrayRestResponse();
        response = bytesResponse;

        if (accept == null) {
            accept = "*/*";
        }

        connect(true);

        InputStream is = getInputStreamOrErrorStream();
        bytesResponse.readResponse(is);

        if (Rest.isTraceEnabled()) {
            Rest.trace("Byte array response: " + bytesResponse.getByteArray().length + " bytes");
        }

        if (throwOnError) {
            bytesResponse.throwOnError();
        }

        return bytesResponse;
    }

    public JSONRestResponse asJSON() throws RestException, IOException, JSONException {
        JSONRestResponse jsonResponse = new JSONRestResponse();
        response = jsonResponse;

        accept = "application/json";
        acceptCharset = "UTF-8";

        connect(true);

        InputStream is = getInputStreamOrErrorStream();
        jsonResponse.readResponse(is);

        if (Rest.isTraceEnabled()) {
            Rest.trace("JSON response: " + jsonResponse.getJSONObject());
        }

        if (throwOnError) {
            jsonResponse.throwOnError();
        }

        return jsonResponse;
    }

    public StringRestResponse asUTF8() throws RestException, IOException {
        return asString("UTF-8");
    }

    public StringRestResponse asString(String encoding) throws RestException, IOException {
        StringRestResponse stringResponse = new StringRestResponse();
        response = stringResponse;

        if (accept == null) {
            accept = "*/*";
        }
        if (acceptCharset == null) {
            acceptCharset = encoding;
        }

        connect(true);

        InputStream is = getInputStreamOrErrorStream();
        stringResponse.readResponse(is, encoding);

        if (Rest.isTraceEnabled()) {
            Rest.trace("UTF-8 response: " + stringResponse.getString());
        }

        if (throwOnError) {
            stringResponse.throwOnError();
        }

        return stringResponse;
    }

    private InputStream getInputStreamOrErrorStream() {
        try {
            InputStream inputStream = http.getInputStream();
            if (manualGunzip) {
                return new GZIPInputStream(inputStream);
            }
            return inputStream;
        } catch (IOException e) {
            return http.getErrorStream();
        }
    }

    public RestRequest dontThrowOnError() {
        throwOnError = false;
        return this;
    }
}
