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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.splunk.logging.hec.MetadataTags;
import com.splunk.logging.serialization.EventInfoTypeAdapter;
import com.splunk.logging.serialization.HecJsonSerializer;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateException;
import java.util.*;


/**
 * This is an internal helper class that sends logging events to Splunk http event collector.
 */
public class HttpEventCollectorSenderAsync extends AHttpEventCollectorSender<HttpEventCollectorMiddlewareAsync, HttpEventCollectorMiddlewareAsync.HttpSenderMiddleware> implements HttpEventCollectorMiddlewareAsync.IHttpSender {
    /**
     * Sender operation mode. Parallel means that all HTTP requests are
     * asynchronous and may be indexed out of order. Sequential mode guarantees
     * sequential order of the indexed events.
     */
    public enum SendMode
    {
        Sequential,
        Parallel
    };

    private SendMode sendMode;

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
    public HttpEventCollectorSenderAsync(
            final String url, final String token, final String channel, final String type,
            long delay, long maxEventsBatchCount, long maxEventsBatchSize,
            String sendModeStr,
            Map<String, String> metadata) {
        super(new HttpEventCollectorMiddlewareAsync(), url, token, channel, type, delay, maxEventsBatchCount, maxEventsBatchSize, metadata);
        if (sendModeStr != null) {
            if (sendModeStr.equals(SendModeSequential))
                this.sendMode = SendMode.Sequential;
            else if (sendModeStr.equals(SendModeSParallel))
                this.sendMode = SendMode.Parallel;
            else
                throw new IllegalArgumentException("Unknown send mode: " + sendModeStr);
        }
    }

    @Override
    protected OkHttpClient.Builder buildOkHttpClient() {
        OkHttpClient.Builder builder = super.buildOkHttpClient();
        // limit max  number of async requests in sequential mode
        if (sendMode == SendMode.Sequential) {
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(1);
            builder.dispatcher(dispatcher);
        }
        return builder;
    }

    @Override
    protected void postEventsMiddleware(List<HttpEventCollectorEventInfo> events) {
        this.middleware.postEvents(events,  this, new HttpEventCollectorMiddlewareAsync.IHttpSenderCallback() {

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
                           final HttpEventCollectorMiddlewareAsync.IHttpSenderCallback callback) {
        Request.Builder requestBldr = requestBuilder(events);

        httpClient.newCall(requestBldr.build()).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, final Response response) {
                String reply = "";
                int httpStatusCode = response.code();
                // read reply only in case of a server error
                try (ResponseBody body = response.body()) {
                    if (httpStatusCode != 200 && body != null) {
                        try {
                            reply = body.string();
                        } catch (IOException e) {
                            reply = e.getMessage();
                        }
                    }
                }
                callback.completed(httpStatusCode, reply);
            }

            @Override
            public void onFailure(Call call, IOException ex) {
                callback.failed(ex);
            }
        });
    }
}
