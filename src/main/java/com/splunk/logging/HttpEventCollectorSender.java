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
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;


/**
 * This is an internal helper class that sends logging events to Splunk http event collector.
 */
final class HttpEventCollectorSender extends TimerTask implements HttpEventCollectorMiddleware.IHttpEventCollectorMiddleware {
    public static final String MetadataTimeTag = "time";
    public static final String MetadataIndexTag = "index";
    public static final String MetadataSourceTag = "source";
    public static final String MetadataSourceTypeTag = "sourcetype";
    private static final String AuthorizationHeaderTag = "Authorization";
    private static final String AuthorizationHeaderScheme = "Splunk %s";
    private static final String HttpContentType = "application/json; profile=urn:splunk:event:1.0; charset=utf-8";

    private String url;
    private String token;
    private long maxEventsBatchCount;
    private long maxEventsBatchSize;
    private long retriesOnError;
    private Dictionary<String, String> metadata;
    private Timer timer;
    private List<HttpEventCollectorEventInfo> eventsBatch = new LinkedList<HttpEventCollectorEventInfo>();
    private long eventsBatchSize = 0; // estimated total size of events batch
    private CloseableHttpAsyncClient httpClient;
    private boolean disableCertificateValidation = false;

    /**
     * Initialize HttpInputEventSender
     * @param Url http input url
     * @param token application token
     * @param delay batching delay
     * @param maxEventsBatchCount max number of events in a batch
     * @param maxEventsBatchSize max size of batch
     * @param metadata events metadata
     */
    public HttpEventCollectorSender(
            final String Url, final String token,
            long delay, long maxEventsBatchCount, long maxEventsBatchSize,
            long retriesOnError,
            Dictionary<String, String> metadata) {
        this.url = Url;
        this.token = token;
        // when size configuration setting is missing it's treated as "infinity",
        // i.e., any value is accepted.
        if (maxEventsBatchCount == 0 && maxEventsBatchSize > 0) {
            maxEventsBatchCount = Long.MAX_VALUE;
        } else if (maxEventsBatchSize == 0 && maxEventsBatchCount > 0) {
            maxEventsBatchSize = Long.MAX_VALUE;
        }
        this.maxEventsBatchCount = maxEventsBatchCount;
        this.maxEventsBatchSize = maxEventsBatchSize;
        this.retriesOnError = retriesOnError;
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
        // create event info container and add it to the batch
        HttpEventCollectorEventInfo eventInfo =
                new HttpEventCollectorEventInfo(severity, message);
        eventsBatch.add(eventInfo);
        eventsBatchSize += severity.length() + message.length();
        if (eventsBatch.size() >= maxEventsBatchCount || eventsBatchSize > maxEventsBatchSize) {
            flush();
        }
    }

    /**
     * Flush all pending events
     */
    public synchronized void flush() {
        if (eventsBatch.size() > 0) {
            postEventsAsync(eventsBatch);
        }
        // Clear the batch. A new list should be created because events are
        // sending asynchronously and "previous" instance of eventsBatch object
        // is still in use.
        eventsBatch = new LinkedList<HttpEventCollectorEventInfo>();
        eventsBatchSize = 0;
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

    private String serializeEventInfo(HttpEventCollectorEventInfo eventInfo) {
        // create event json content
        JSONObject event = new JSONObject();
        // event timestamp and metadata
        String index = metadata.get(MetadataIndexTag);
        String source = metadata.get(MetadataSourceTag);
        String sourceType = metadata.get(MetadataSourceTypeTag);
        event.put(MetadataTimeTag, String.format("%d", eventInfo.getTime()));
        if (index != null && index.length() > 0)
            event.put(MetadataIndexTag, index);
        if (source  != null && source.length() > 0)
            event.put(MetadataSourceTag, source);
        if (sourceType  != null && sourceType.length() > 0)
            event.put(MetadataSourceTypeTag, sourceType);
        // event body
        JSONObject body = new JSONObject();
        body.put("severity", eventInfo.getSeverity());
        body.put("message", eventInfo.getMessage());
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

    private void postEventsAsync(final List<HttpEventCollectorEventInfo> eventsBatch) {
        startHttpClient();
        final String encoding = "utf-8";
        // convert events list into a string
        StringBuilder eventsBatchString = new StringBuilder();
        for (HttpEventCollectorEventInfo eventInfo : eventsBatch)
            eventsBatchString.append(serializeEventInfo(eventInfo));
        // create http request
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(
                AuthorizationHeaderTag,
                String.format(AuthorizationHeaderScheme, token));
        StringEntity entity = new StringEntity(eventsBatchString.toString(), encoding);
        entity.setContentType(HttpContentType);
        httpPost.setEntity(entity);
        // post request
        httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
            long retriesCount = 0;

            public void completed(final HttpResponse response) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String reply = "";
                    try {
                        reply = EntityUtils.toString(response.getEntity(), encoding);
                    } catch (IOException e) {
                        reply = e.getMessage();
                    }
                    HttpEventCollectorErrorHandler.error(
                            eventsBatch,
                            new HttpEventCollectorErrorHandler.ServerErrorException(reply));
                }
            }

            public void failed(final Exception ex) {
                if (retriesCount >= retriesOnError) {
                    HttpEventCollectorErrorHandler.error(eventsBatch, ex);
                } else {
                    // retry
                    retriesCount ++;
                    httpClient.execute(httpPost, this);
                }
            }

            public void cancelled() {}
        });
    }
}
