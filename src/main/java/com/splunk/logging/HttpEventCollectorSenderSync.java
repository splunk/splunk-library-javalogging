package com.splunk.logging;

import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.splunk.logging.HttpEventCollectorMiddleware.convertToCallback;

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

public class HttpEventCollectorSenderSync extends AHttpEventCollectorSender implements HttpEventCollectorMiddleware.IHttpSender {
    public HttpEventCollectorSenderSync(String url, String token, String channel, String type, long delay,
                                        long maxEventsBatchCount, long maxEventsBatchSize, String sendModeStr, Map<String, String> metadata) {
        super(url, token, channel, type, delay, maxEventsBatchCount, maxEventsBatchSize, sendModeStr, metadata);
    }


    @Override
    public void postEvents(List<HttpEventCollectorEventInfo> events, HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        HttpEventCollectorMiddleware.HttpSenderResult result = postEvents(events);
        convertToCallback(callback, result);
    }

    @Override
    protected void postEventsMiddleware(List<HttpEventCollectorEventInfo> eventsBatch) {
        HttpEventCollectorMiddleware.HttpSenderResult result = this.middleware.postEvents(eventsBatch, this);
        if (result.failed != null) {
            HttpEventCollectorErrorHandler.error(
                    eventsBatch,
                    new HttpEventCollectorErrorHandler.ServerErrorException(result.failed.getMessage()));
        } else if (result.success.statusCode != 200) {
            HttpEventCollectorErrorHandler.error(
                    eventsBatch,
                    new HttpEventCollectorErrorHandler.ServerErrorException(result.success.reply));
        }
    }


    @Override
    public HttpEventCollectorMiddleware.HttpSenderResult postEvents(List<HttpEventCollectorEventInfo> events) {
        Request.Builder requestBldr = requestBuilder(events);

        try {
            Response response = httpClient.newCall(requestBldr.build()).execute();
            return new HttpEventCollectorMiddleware.HttpSenderResult(responseSuccess(response));
        } catch (IOException ex) {
            return new HttpEventCollectorMiddleware.HttpSenderResult(ex);
        }
    }
}