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

/**
 * @brief Splunk http event collector middleware class.
 *
 * @details
 * A user application can utilize HttpEventCollectorMiddleware to customize the behavior
 * of sending events to Splunk. The user application can replace the default middleware
 * with a custom middleware instance implementing the IHttpEventCollectorMiddleware interface
 * by calling setMiddleware().
 * Usage example:
 * HttpEventCollectorMiddleware.setMiddleware();
 */

public class HttpEventCollectorMiddleware {

    public interface IHttpEventCollectorMiddleware {
        public void send(String severity, String message);

        public void flush();

        public void close();
    }

    private static IHttpEventCollectorMiddleware senderMiddleware;
    public static void setMiddleware(IHttpEventCollectorMiddleware middleware) {
        senderMiddleware = middleware;
    }

    public static boolean hasMiddleware() {
        return senderMiddleware != null;
    }

    public static void send(String severity, String message) {
        senderMiddleware.send(severity, message);
    }

    public static void flush() {
        senderMiddleware.flush();
    }

    public static void close() {
        senderMiddleware.close();
    }
}
