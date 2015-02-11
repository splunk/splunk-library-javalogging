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

import java.util.concurrent.Future;

/**
 * @brief Splunk http input error handler.
 *
 * @details
 * A user application can utilize HttpInputErrorHandler in order to detect errors
 * caused by network connection and/or Splunk server
 */
public class HttpInputErrorHandler {
    public interface ErrorCallback {
        void exception(final String data, final Exception ex);
        void error(final String data, final String reply);
    }

    private static ErrorCallback errorCallback;

    public static void onError(ErrorCallback callback) {
        errorCallback = callback;
    }

    public static void exception(final String data, final Exception ex) {
        if (errorCallback != null) {
            errorCallback.exception(data, ex);
        }
    }

    public static void error(final String data, final String reply) {
        if (errorCallback != null) {
            errorCallback.error(data, reply);
        }
    }
}
