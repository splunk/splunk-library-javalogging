package com.splunk.logging;

/**
 * @copyright Copyright 2013-2015 Splunk, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
     * This exception is passed to error callback when Splunk fails to flush events
     */
    public static class FlushException extends Exception {
        private int numMsg;
        private String errorText;

        /**
         * Create an exception with number of messages that couldn't be flushed properly
         *
         * @param numMsg number of messages
         */
        public FlushException(final int numMsg) {
            this.errorText = "There was an exception flushing [" + numMsg + "] events!";
        }

        @Override
        public String getMessage() {
            return String.valueOf(numMsg);
        }


        @Override
        public String toString() {
            return getMessage();
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


        @Override
        public String toString() {
            return getReply();
        }
    }

    public interface ErrorCallback {
        void error(final List<HttpEventCollectorEventInfo> data, final Exception ex);
    }

    private static ErrorCallback errorCallback;

    /**
     * Register error callbacks
     * @param callback ErrorCallback
     */
    public static void onError(ErrorCallback callback) {
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
}
