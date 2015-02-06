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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.json.simple.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Timer;
import java.util.TimerTask;


/**
 * This is an internal helper class that sends logging events to Splunk http
 * input. This class is not supposed to be used by Splunk customers.
 */
public final class HttpInputEventSender extends TimerTask {
    public static final String MetadataTimeTag = "time";
    public static final String MetadataIndexTag = "index";
    public static final String MetadataSourceTag = "source";
    public static final String MetadataSourceTypeTag = "sourcetype";
    private static final String AuthorizationHeaderTag = "Authorization";
    private static final String AuthorizationHeaderScheme = "Splunk %s";

    private String httpInputUrl;
    private final String token;
    private final long maxEventsBatchCount;
    private final long maxEventsBatchSize;
    private Dictionary<String, String> metadata;
    private Timer timer;
    private StringBuilder eventsBatch = new StringBuilder();
    private long eventsCount = 0; // count of events in events batch
    private CloseableHttpAsyncClient httpClient;
    private boolean disableCertificateValidation = false;

    /**
     * Initialize HttpInputEventSender
     * @param httpInputUrl http input url
     * @param token application token
     * @param delay batching delay
     * @param maxEventsBatchCount max number of events in a batch
     * @param maxEventsBatchSize max size of batch
     * @param metadata events metadata
     */
    public HttpInputEventSender(
        final String httpInputUrl, final String token,
        long delay, long maxEventsBatchCount, long maxEventsBatchSize,
        Dictionary<String, String> metadata) {
        this.httpInputUrl = httpInputUrl;
        this.token = token;
        this.maxEventsBatchCount = maxEventsBatchCount;
        this.maxEventsBatchSize = maxEventsBatchSize;
        this.metadata = metadata;

        if (delay > 0) {
            // start heartbeat timer
            timer = new Timer();
            timer.scheduleAtFixedRate(this, delay, delay);
        }
    }

    /**
     * Send a single logging event
     * @note in case of batching the event isn't sent immediately
     * @param severity event severity level (info, warning, etc.)
     * @param message event text
     */
    public synchronized void send(final String severity, final String message) {
        eventsBatch.append(createEvent(severity, message));
        eventsCount++;
        if (eventsCount > maxEventsBatchCount || eventsBatch.length() > maxEventsBatchSize) {
            flush();
        }
    }

    /**
     * Flush all pending events
     */
    public synchronized void flush() {
        if (eventsBatch.length() > 0) {
            postEventsAsync(eventsBatch.toString());
        }
        // clear the batch buffer
        eventsBatch.setLength(0);
        eventsCount = 0;
    }

    /**
     * Close events sender
     */
    public void close() {
        if (timer != null)
            timer.cancel();
        flush();
    }

    /**
     * Timer heartbeat
     */
    @Override // TimerTask
    public void run() {
        flush();
    }

    /**
     * Disable https certificate validation of the splunk server.
     * This functionality is for development purpose only.
     */
    public void disableCertificateValidation() {
        disableCertificateValidation = true;
    }

    private String createEvent(final String severity, final String message) {
        // create event json content
        JSONObject event = new JSONObject();
        // event timestamp and metadata
        String index = metadata.get(MetadataIndexTag);
        String source = metadata.get(MetadataSourceTag);
        String sourceType = metadata.get(MetadataSourceTypeTag);
        event.put(MetadataTimeTag, String.format("%d", System.currentTimeMillis() / 1000));
        if (index != null && index.length() > 0)
            event.put(MetadataIndexTag, index);
        if (source  != null && source.length() > 0)
            event.put(MetadataSourceTag, source);
        if (sourceType  != null && sourceType.length() > 0)
            event.put(MetadataSourceTypeTag, sourceType);
        // event body
        JSONObject body = new JSONObject();
        body.put("severity", severity);
        body.put("message", message);
        // join event and body
        event.put("event", body);
        return event.toString();
    }

    private void startHttpClient() {
        if (httpClient != null) {
            // http client is already started
            return;
        }
        if (! disableCertificateValidation) {
            // create an http client that validates certificates
            httpClient = HttpAsyncClients.createDefault();
        } else {
            // create strategy that accepts all certificates
            TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] certificate,
                                         String type) {
                    return true;
                }
            };
            SSLContext sslContext = null;
            try {
                sslContext = SSLContexts.custom().loadTrustMaterial(
                   null, acceptingTrustStrategy).build();
                httpClient = HttpAsyncClients.custom()
                    .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                    .setSSLContext(sslContext).build();
            } catch (Exception e) { }
        }
        httpClient.start();
    }

    // Currently we never close http client. This method is added for symmetry
    // with startHttpClient.
    private void stopHttpClient() throws SecurityException {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {}
            httpClient = null;
        }
    }

    private void postEventsAsync(final String eventsBatch) {
        startHttpClient();
        HttpPost httpPost = new HttpPost(httpInputUrl);
        httpPost.setHeader(
            AuthorizationHeaderTag,
            String.format(AuthorizationHeaderScheme, token));
        StringEntity entity = new StringEntity(eventsBatch, "utf-8");
        entity.setContentType("application/json; charset=utf-8");
        httpPost.setEntity(entity);
        httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
            // @todo - handle reply
            public void completed(final HttpResponse response) {}
            public void failed(final Exception ex) {}
            public void cancelled() {}
        });
    }
}
