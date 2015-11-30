package com.hrunk.minirest;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 */
public class Rest {
    private static final String TAG = "Rest";

    private static String userAgent = "HrunkRest/1.0";

    public static final int DEBUG_OFF = 0;
    public static final int DEBUG_ON = 1;
    public static final int DEBUG_TRACE = 2;
    private static int debug = DEBUG_TRACE;

    public static void setDebugLevel(int value) {
        debug = value;
    }

    public static int getDebugLevel() {
        return debug;
    }

    public static void setUserAgent(String value) {
        userAgent = value;
    }

    public static String getUserAgent() {
        return userAgent;
    }

    public static RestRequest get(String url) throws IOException {
        return new RestRequest(RestRequest.GET, url);
    }

    public static RestRequest put(String url) throws IOException {
        return new RestRequest(RestRequest.PUT, url);
    }

    public static RestRequest post(String url) throws IOException {
        return new RestRequest(RestRequest.POST, url);
    }

    public static boolean isTraceEnabled() {
        return debug >= DEBUG_TRACE;
    }

    public static void log(String string) {
        if (debug >= DEBUG_ON) {
            Log.d(TAG, string);
        }
    }

    public static void trace(String string) {
        if (isTraceEnabled()) {
            Log.d(TAG, string);
        }
    }

    static byte[] readWholeStream(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int numberRead = 0;
        while ((numberRead = is.read(buffer)) != -1){
            bos.write(buffer, 0, numberRead);
        }

        return bos.toByteArray();
    }
}
