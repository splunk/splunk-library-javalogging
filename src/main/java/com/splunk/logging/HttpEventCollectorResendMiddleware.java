package com.splunk.logging;

import java.util.Arrays;
import java.util.HashSet;

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
import java.util.Set;

/**
 * Splunk http event collector resend middleware.
 *
 *
 * HTTP event collector middleware plug in that implements a simple resend policy.
 * When HTTP post reply isn't an application error it tries to resend the data.
 * An exponentially growing delay is used to prevent server overflow.
 */
public class HttpEventCollectorResendMiddleware
        extends HttpEventCollectorMiddleware.HttpSenderMiddleware {

    /**
     * List of HTTP event collector server application error statuses. These statuses
     * indicate non-transient problems that cannot be fixed by resending the
     * data.
     */
    private static final Set<Integer> HttpEventCollectorApplicationErrors  = new HashSet<>(Arrays.asList(
            // Forbidden
            403,
            // Method Not Allowed
            405,
            // Bad Request
            400
    ));
    private long retriesOnError = 0;

    /**
     * Create a resend middleware component.
     * @param retriesOnError is the max retry count.
     */
    public HttpEventCollectorResendMiddleware(long retriesOnError) {
        this.retriesOnError = retriesOnError;
    }

    public void postEvents(
            final List<HttpEventCollectorEventInfo> events,
            HttpEventCollectorMiddleware.IHttpSender sender,
            HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
         callNext(events, sender, new Callback(events, sender, callback));
    }

    private boolean shouldRetry(int statusCode) {
        return statusCode != 200 && !HttpEventCollectorApplicationErrors.contains(statusCode);
    }

    private class Callback implements HttpEventCollectorMiddleware.IHttpSenderCallback {
        private long retries = 0;
        private final List<HttpEventCollectorEventInfo> events;
        private HttpEventCollectorMiddleware.IHttpSenderCallback prevCallback;
        private HttpEventCollectorMiddleware.IHttpSender sender;
        private final long RetryDelayCeiling = 60 * 1000; // 1 minute
        private long retryDelay = 1000; // start with 1 second

        public Callback(
                final List<HttpEventCollectorEventInfo> events,
                HttpEventCollectorMiddleware.IHttpSender sender,
                HttpEventCollectorMiddleware.IHttpSenderCallback prevCallback) {
            this.events = events;
            this.prevCallback = prevCallback;
            this.sender = sender;
        }

        @Override
        public void completed(int statusCode, final String reply) {
            if (shouldRetry(statusCode) && retries < retriesOnError) {
                retry();
            } else {
                // if non-retryable, resend wouldn't help, delegate to previous callback
                prevCallback.completed(statusCode, reply);
            }
        }

        @Override
        public void failed(final Exception ex) {
            if (retries < retriesOnError) {
                retry();
            } else {
                prevCallback.failed(ex);
            }
        }

        private void retry() {
            retries++;
            try {
                Thread.sleep(retryDelay);
                callNext(events, sender, this);
            } catch (InterruptedException ie) {
                prevCallback.failed(ie);
            }
            // increase delay exponentially
            retryDelay = Math.min(RetryDelayCeiling, retryDelay * 2);
        }
    }
}
