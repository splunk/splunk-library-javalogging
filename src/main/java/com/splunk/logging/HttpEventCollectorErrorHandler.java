package com.splunk.logging;

/**
 * @copyright
 *
 * Copyright 2013-2015 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

/**
 * Splunk http event collector error handler.
 *
 * A user application can utilize HttpEventCollectorErrorHandler in order to detect errors
 * caused by network connection and/or Splunk server.
 *
 * Usage example:
 * HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
 *     public void error(final String data, final Exception ex) {  // handle exception  }
 * });
 */
public class HttpEventCollectorErrorHandler {

    /**
     * Register error handler via full class name.
     *
     * When the class name is null or empty, null is registered to the <code>HttpEventCollectorErrorHandler</code>.
     *
     * @param errorCallbackClass the name of the class, for instance: <code>com.splunk.logging.util.StandardErrorCallback</code>
     */
    public static void registerClassName(String errorCallbackClass) {
        if (errorCallbackClass == null || errorCallbackClass.trim().isEmpty()) {
            HttpEventCollectorErrorHandler.onError(null);
            return;
        }
        try {
            ErrorCallback callback = (ErrorCallback) Class.forName(errorCallbackClass).newInstance();
            HttpEventCollectorErrorHandler.onError(callback);
        } catch (final Exception e) {
            System.err.println("Warning: cannot create ErrorCallback instance: " + e);
        }
    }

    /**
     * This exception is passed to error callback when Splunk server replies an error
     */
    @SuppressWarnings("serial")
    public static class ServerErrorException extends Exception {
        private String reply;
        private long errorCode = -1;
        private String errorText;

        /**
         * Create an exception with server error reply
         * @param serverReply server reply
         */
        public ServerErrorException(final String serverReply) {
            reply = serverReply;
            try {
                // read server reply
                JsonObject json = JsonParser.parseString(serverReply).getAsJsonObject();
                errorCode = json.get("code").getAsLong();
                errorText = json.get("text").getAsString();
            } catch (Exception e) {
                errorText = e.getMessage();
            }
        }

        /**
         * @return Splunk server reply in json format
         */
        public String getReply() {
            return reply;
        }

        /**
         * @return error code replied by Splunk server
         */
        public long getErrorCode() {
            return errorCode;
        }

        /**
         * * @return error text replied by Splunk server
         */
        public String getErrorText() {
            return errorText;
        }

        @Override
        public String getMessage() {
            return getErrorText();
        }


        @Override public String toString() {
            return getReply();
        }
    }

    public interface ErrorCallback {
        void error(final List<HttpEventCollectorEventInfo> data, final Exception ex);
    }

    private static ErrorCallback errorCallback;

    /**
     * Register error callbacks.
     *
     * @param callback ErrorCallback Only one ErrorCallback can be registered. A new one will replace the old one.
     */
    public static void onError(ErrorCallback callback) {
        if (callback == null) {
            logInfo("Reset ErrorCallback to null (no error handling).");
        }
        else {
            logInfo("Register ErrorCallback implementation: " + callback);
            // onError() is called multiple times in unit tests and is also replaced intentionally.
            // Issue a warning when it is replaced by a different kind of handler.
            if (errorCallback != null && !errorCallback.equals(callback)) {
                logWarn("ErrorCallback instance of '"
                    + errorCallback.getClass().getName()
                    + "' will be replaced by handler instance of '"
                    + callback.getClass().getName()
                    + "'");
            }
        }
        errorCallback = callback;
    }

    /**
     * Report an exception
     * @param data eventdata
     * @param ex is an exception thrown by posting or processing data
     */
    public static void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
        if (errorCallback != null) {
            errorCallback.error(data, ex);
        }
    }

    private static void logInfo(String message) {
        System.out.println("Info: " + message);
    }
    private static void logWarn(String message) {
        System.out.println("Warning: " + message);
    }
}
