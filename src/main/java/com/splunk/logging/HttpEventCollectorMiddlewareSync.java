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

public class HttpEventCollectorMiddlewareSync implements IHttpEventCollectorMiddleware<HttpEventCollectorMiddlewareSync.HttpSenderMiddleware> {

    private HttpSenderMiddleware httpSenderMiddleware = null;

    public static class HttpSenderResult {
        public final HttpSenderSuccess success;
        public final Exception failed;

        public HttpSenderResult(HttpSenderSuccess success, Exception failed) {
            this.success = success;
            this.failed = failed;
        }

        public HttpSenderResult(HttpSenderSuccess success) {
            this.success = success;
            this.failed = null;
        }

        public HttpSenderResult(Exception failed) {
            this.success = null;
            this.failed = failed;
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
     * An interface that describes an abstract events sender working synchronously.
     */
    public interface IHttpSender {
        HttpSenderResult postEvents(final List<HttpEventCollectorEventInfo> events);
    }

    /**
     * An abstract middleware component.
     */
    public static abstract class HttpSenderMiddleware implements IHttpSenderMiddleware {
        private HttpSenderMiddleware next;

        public abstract HttpSenderResult postEvents(
                final List<HttpEventCollectorEventInfo> events,
                IHttpSender sender);

        protected HttpSenderResult callNext(final List<HttpEventCollectorEventInfo> events,
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
}
