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

import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;


/**
 * This is an internal helper class that sends logging events to Splunk http event collector.
 */
public class HttpEventCollectorSender extends AHttpEventCollectorSender implements HttpEventCollectorMiddleware.IHttpSender {
    /**
     * Initialize HttpEventCollectorSender
     * @param url http event collector input server
     * @param token application token
     * @param delay batching delay
     * @param maxEventsBatchCount max number of events in a batch
     * @param maxEventsBatchSize max size of batch
     * @param metadata events metadata
     * @param channel unique GUID for the client to send raw events to the server
     * @param type event data type
     */
    public HttpEventCollectorSender(
            final String url, final String token, final String channel, final String type,
            long delay, long maxEventsBatchCount, long maxEventsBatchSize,
            String sendModeStr,
            Map<String, String> metadata) {
        super(url, token, channel, type, delay, maxEventsBatchCount, maxEventsBatchSize, sendModeStr, metadata);
    }

    @Override
    protected void postEventsMiddleware(List<HttpEventCollectorEventInfo> events) {
        this.middleware.postEvents(events,  this, new HttpEventCollectorMiddleware.IHttpSenderCallback() {

            @Override
            public void completed(int statusCode, String reply) {
                if (statusCode != 200) {
                    HttpEventCollectorErrorHandler.error(
                            events,
                            new HttpEventCollectorErrorHandler.ServerErrorException(reply));
                }
            }

            @Override
            public void failed(Exception ex) {
                HttpEventCollectorErrorHandler.error(
                        events,
                        new HttpEventCollectorErrorHandler.ServerErrorException(ex.getMessage()));
            }
        });
    }


    @Override
    public void postEvents(final List<HttpEventCollectorEventInfo> events,
                           final HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        Request.Builder requestBldr = requestBuilder(events);

        httpClient.newCall(requestBldr.build()).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, final Response response) {
                HttpEventCollectorMiddleware.HttpSenderSuccess result = responseSuccess(response);
                callback.completed(result.statusCode, result.reply);
            }

            @Override
            public void onFailure(Call call, IOException ex) {
                callback.failed(ex);
            }
        });
    }

    @Override
    public HttpEventCollectorMiddleware.HttpSenderResult postEvents(List<HttpEventCollectorEventInfo> events) {
        final CountDownLatch latch = new CountDownLatch(1);
        final HttpEventCollectorMiddleware.HttpSenderResult result = new HttpEventCollectorMiddleware.HttpSenderResult();
        postEvents(events, new HttpEventCollectorMiddleware.IHttpSenderCallback(){
            @Override
            public void completed(int statusCode, String reply) {
                result.success = new HttpEventCollectorMiddleware.HttpSenderSuccess(statusCode, reply);
            }

            @Override
            public void failed(Exception ex) {
                result.failed = ex;
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            result.failed = ex;
        }
        return result;
    }
}
