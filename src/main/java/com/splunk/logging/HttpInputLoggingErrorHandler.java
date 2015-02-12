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

import java.util.List;

/**
 * @brief Splunk http input error handler.
 *
 * @details
 * A user application can utilize HttpInputLoggingErrorHandler in order to detect errors
 * caused by network connection and/or Splunk server.
 *
 * Usage example:
 * HttpInputLoggingErrorHandler.onError(new HttpInputErrorHandler.ErrorCallback() {
 *     public void exception(final String data, final Exception ex) {  handle exception  }
 *     public void error(final String data, final String reply) { handle error }
 * });
 */
public class HttpInputLoggingErrorHandler {
    public interface ErrorCallback {
        void exception(final List<HttpInputLoggingEventInfo> data, final Exception ex);
        void error(final List<HttpInputLoggingEventInfo> data, final String reply);
    }

    private static ErrorCallback errorCallback;

    /**
     * Register error callbacks
     * @param callback
     */
    public static void onError(ErrorCallback callback) {
        errorCallback = callback;
    }

    /**
     * Report an exception
     * @param data
     * @param ex is an exception thrown bgy posting data
     */
    public static void exception(final List<HttpInputLoggingEventInfo> data, final Exception ex) {
        if (errorCallback != null) {
            errorCallback.exception(data, ex);
        }
    }

    /**
     * Report an error
     * @param data
     * @param reply returned by Splunk server
     */
    public static void error(final List<HttpInputLoggingEventInfo> data, final String reply) {
        if (errorCallback != null) {
            errorCallback.error(data, reply);
        }
    }
}
