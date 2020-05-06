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

import java.util.List;

/**
 * Splunk http event collector middleware implementation.
 * <p>
 * A user application can utilize HttpEventCollectorMiddleware to customize the behavior
 * of sending events to Splunk. A user application plugs middleware components to
 * the HttpEventCollectorSender by calling addMiddleware method.
 * <p>
 * HttpEventCollectorResendMiddleware.java is an example of how middleware can be used.
 */

public class HttpEventCollectorMiddleware implements IHttpEventCollectorMiddleware<HttpEventCollectorMiddleware.HttpSenderMiddleware> {

    private HttpSenderMiddleware httpSenderMiddleware = null;

    public static class HttpSenderResult {
        public HttpEventCollectorMiddleware.HttpSenderSuccess success = null;
        public Exception failed = null;

        public HttpSenderResult() {
        }

        public HttpSenderResult(HttpEventCollectorMiddleware.HttpSenderSuccess success, Exception failed) {
            this.success = success;
            this.failed = failed;
        }

        public HttpSenderResult(HttpEventCollectorMiddleware.HttpSenderSuccess success) {
            this.success = success;
        }

        public HttpSenderResult(Exception failed) {
            this.failed = failed;
        }

        public boolean isSuccess() {
            return success != null;
        }
    }

    public static class HttpSenderSuccess {
        public final int statusCode;
        public final String reply;

        public HttpSenderSuccess(int statusCode, String reply) {
            this.statusCode = statusCode;
            this.reply = reply;
        }
    }

    /**
     * An interface that describes an abstract events sender working synchronously/asynchronously.
     */
    public interface IHttpSender {
        HttpEventCollectorMiddleware.HttpSenderResult postEvents(final List<HttpEventCollectorEventInfo> events);

        void postEvents(final List<HttpEventCollectorEventInfo> events, IHttpSenderCallback callback);
    }

    /**
     * Callback methods invoked by events sender.
     */
    public interface IHttpSenderCallback {
        void completed(int statusCode, final String reply);

        void failed(final Exception ex);
    }

    /**
     * An abstract middleware component.
     */
    public static abstract class HttpSenderMiddleware implements IHttpSenderMiddleware {
        private HttpSenderMiddleware next;

        public abstract void postEvents(
                final List<HttpEventCollectorEventInfo> events,
                IHttpSender sender,
                IHttpSenderCallback callback);

        public abstract HttpEventCollectorMiddleware.HttpSenderResult postEvents(
                final List<HttpEventCollectorEventInfo> events,
                HttpEventCollectorMiddleware.IHttpSender sender);

        protected void callNext(final List<HttpEventCollectorEventInfo> events,
                                IHttpSender sender,
                                IHttpSenderCallback callback) {
            HttpSenderResult result = callNext(events, sender);
            convertToCallback(callback, result);
        }

        protected HttpEventCollectorMiddleware.HttpSenderResult callNext(final List<HttpEventCollectorEventInfo> events,
                                                                         IHttpSender sender) {
            if (next != null) {
                return next.postEvents(events, sender);
            } else {
                return sender.postEvents(events);
            }
        }
    }

    /**
     * Post http event collector data
     *
     * @param events   list
     * @param sender   is http sender
     * @param callback async callback
     */
    public void postEvents(final List<HttpEventCollectorEventInfo> events,
                           IHttpSender sender,
                           IHttpSenderCallback callback) {
        HttpSenderResult result = postEvents(events, sender);
        convertToCallback(callback, result);
    }

    /**
     * Post http event collector data
     *
     * @param events list
     */
    public HttpSenderResult postEvents(final List<HttpEventCollectorEventInfo> events, IHttpSender sender) {
        if (httpSenderMiddleware == null) {
            return sender.postEvents(events);
        } else {
            return httpSenderMiddleware.postEvents(events, sender);
        }
    }

    /**
     * Plug a middleware component to the middleware chain.
     *
     * @param middleware is a new middleware
     */
    @Override
    public void add(HttpSenderMiddleware middleware) {
        if (httpSenderMiddleware != null) {
            middleware.next = httpSenderMiddleware;
        }
        httpSenderMiddleware = middleware;
    }

    public static void convertToCallback(HttpEventCollectorMiddleware.IHttpSenderCallback callback, HttpEventCollectorMiddleware.HttpSenderResult result) {
        if (result.isSuccess()) {
            callback.completed(result.success.statusCode, result.success.reply);
        } else {
            callback.failed(result.failed);
        }
    }
}
