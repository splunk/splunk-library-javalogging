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
import org.json.simple.JSONValue;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Locale;



/**
 * This is an internal helper class that sends logging events to Splunk http event collector.
 */
final class HttpEventCollectorSender extends TimerTask implements HttpEventCollectorMiddleware.IHttpSender {
    public static final String MetadataTimeTag = "time";
    public static final String MetadataHostTag = "host";
    public static final String MetadataIndexTag = "index";
    public static final String MetadataSourceTag = "source";
    public static final String MetadataSourceTypeTag = "sourcetype";
    public static final String MetadataMessageFormatTag = "messageFormat";
    private static final String AuthorizationHeaderTag = "Authorization";
    private static final String AuthorizationHeaderScheme = "Splunk %s";
    private static final String HttpEventCollectorUriPath = "/services/collector/event/1.0";
    private static final String HttpContentType = "application/json; profile=urn:splunk:event:1.0; charset=utf-8";
    private static final String SendModeSequential = "sequential";
    private static final String SendModeSParallel = "parallel";

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
    
    /**
     * 
     * Format of the event message. It is used to apply necessary parsing and formatting to the message, if required for the
     * message format.
     * <p>
     * If format is not set, then it is assumed to be "text"
     * </p>
     *
     */
    public enum MessageFormat {
        
        TEXT("text"),
        JSON("json");

        private final String format;

        MessageFormat(final String format) {
            this.format = format;
        }

        /**
         * Gets MessageFormat instance from string format.
         *
         * @param format the message format
         * @return the message format
         */
        public static MessageFormat fromFormat(final String format) {
            if (format != null && format.trim().length() > 0) {
                for (final MessageFormat formatEnum : values()) {
                    if (formatEnum.format.equals(format)) {
                        return formatEnum;
                    }
                }
            }
            return TEXT;
        }
    };

    /**
     * Recommended default values for events batching.
     */
    public static final int DefaultBatchInterval = 10 * 1000; // 10 seconds
    public static final int DefaultBatchSize = 10 * 1024; // 10KB
    public static final int DefaultBatchCount = 10; // 10 events

    private String url;
    private String token;
    private long maxEventsBatchCount;
    private long maxEventsBatchSize;
    private Dictionary<String, String> metadata;
    private Timer timer;
    private List<HttpEventCollectorEventInfo> eventsBatch = new LinkedList<HttpEventCollectorEventInfo>();
    private long eventsBatchSize = 0; // estimated total size of events batch
    private CloseableHttpAsyncClient httpClient;
    private boolean disableCertificateValidation = false;
    private SendMode sendMode = SendMode.Sequential;
    private HttpEventCollectorMiddleware middleware = new HttpEventCollectorMiddleware();
    private final MessageFormat messageFormat;

    /**
     * Initialize HttpEventCollectorSender
     * @param Url http event collector input server
     * @param token application token
     * @param delay batching delay
     * @param maxEventsBatchCount max number of events in a batch
     * @param maxEventsBatchSize max size of batch
     * @param metadata events metadata
     */
    public HttpEventCollectorSender(
            final String Url, final String token,
            long delay, long maxEventsBatchCount, long maxEventsBatchSize,
            String sendModeStr,
            Dictionary<String, String> metadata) {
        this.url = Url + HttpEventCollectorUriPath;
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
        this.metadata = metadata;
        
        final String format = metadata.get(MetadataMessageFormatTag);
        // Get MessageFormat enum from format string. Do this once per instance in constructor to avoid expensive operation in
        // each event sender call
        this.messageFormat = MessageFormat.fromFormat(format);
        
        if (sendModeStr != null) {
            if (sendModeStr.equals(SendModeSequential))
                this.sendMode = SendMode.Sequential;
            else if (sendModeStr.equals(SendModeSParallel))
                this.sendMode = SendMode.Parallel;
            else
                throw new IllegalArgumentException("Unknown send mode: " + sendModeStr);
        }

        if (delay > 0) {
            // start heartbeat timer
            timer = new Timer();
            timer.scheduleAtFixedRate(this, delay, delay);
        }
    }

    public void addMiddleware(HttpEventCollectorMiddleware.HttpSenderMiddleware middleware) {
        this.middleware.add(middleware);
    }

    /**
     * Send a single logging event
     * @note in case of batching the event isn't sent immediately
     * @param severity event severity level (info, warning, etc.)
     * @param message event text
     */
    public synchronized void send(
            final String severity,
            final String message,
            final String logger_name,
            final String thread_name,
            Map<String, String> properties,
            final String exception_message,
            Serializable marker
    ) {
        // create event info container and add it to the batch
        HttpEventCollectorEventInfo eventInfo =
                new HttpEventCollectorEventInfo(severity, message, logger_name, thread_name, properties, exception_message, marker);
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

    @SuppressWarnings("unchecked")
    private static void putIfPresent(JSONObject collection, String tag, Object value) {
        if (value != null) {
            if (value instanceof String && ((String) value).length() == 0) {
                // Do not add blank string
                return;
            }
            collection.put(tag, value);
        }
    }

    @SuppressWarnings("unchecked")
    private String serializeEventInfo(HttpEventCollectorEventInfo eventInfo) {
        // create event json content
        //
        // cf: http://dev.splunk.com/view/event-collector/SP-CAAAE6P
        //
        JSONObject event = new JSONObject();
        // event timestamp and metadata
        putIfPresent(event, MetadataTimeTag, String.format(Locale.US, "%.3f", eventInfo.getTime()));
        putIfPresent(event, MetadataHostTag, metadata.get(MetadataHostTag));
        putIfPresent(event, MetadataIndexTag, metadata.get(MetadataIndexTag));
        putIfPresent(event, MetadataSourceTag, metadata.get(MetadataSourceTag));
        putIfPresent(event, MetadataSourceTypeTag, metadata.get(MetadataSourceTypeTag));
        
        // Parse message on the basis of format
        final Object parsedMessage = parseEventMessage(eventInfo.getMessage(), this.messageFormat);
        
        // event body
        JSONObject body = new JSONObject();
        putIfPresent(body, "severity", eventInfo.getSeverity());
        putIfPresent(body, "message", parsedMessage);
        putIfPresent(body, "logger", eventInfo.getLoggerName());
        putIfPresent(body, "thread", eventInfo.getThreadName());
        // add an exception record if and only if there is one
        // in practice, the message also has the exception information attached
        if (eventInfo.getExceptionMessage() != null) {
            putIfPresent(body, "exception", eventInfo.getExceptionMessage());
        }

        // add properties if and only if there are any
        final Map<String,String> props = eventInfo.getProperties();
        if (props != null && !props.isEmpty()) {
            body.put("properties", props);
        }
        // add marker if and only if there is one
        final Serializable marker = eventInfo.getMarker();
        if (marker != null) {
            putIfPresent(body, "marker", marker.toString());
        }
        // join event and body
        event.put("event", body);
        return event.toString();
    }

    /**
     * Parses the event message on the basis of message format.
     * 
     * @param message the event message string
     * @param format the event message format
     * 
     * @return parsed event message based on format
     */
    private Object parseEventMessage(final String message, final MessageFormat messageFormat) {
        // if message is null or blank then return without parsing
        if (message == null || message.trim().length() == 0) {
            return message;
        }

        switch (messageFormat) {
            case JSON:
                return parseJsonEventMessage(message);

            case TEXT:
            default:
                // Treat message type as text if format is not defined or defined as text
                return message;
        }
    }

    /**
     * Parses the event message JSON string into JSON object. If parsing fails then the input message is returned as is.
     *
     * @param message the event message string
     * @return the parsed event message JSON object or input message if parsing fails
     */
    private Object parseJsonEventMessage(final String message) {
        final Object jsonObject = JSONValue.parse(message);
        if (jsonObject == null) {
            // If JSON parsing failed then it is likely a text message or a malformed JSON message. Return input message string in
            // such an event.
            return message;
        } else {
            return jsonObject;
        }
    }

    private void startHttpClient() {
        if (httpClient != null) {
            // http client is already started
            return;
        }
        // limit max  number of async requests in sequential mode, 0 means "use
        // default limit"
        int maxConnTotal = sendMode == SendMode.Sequential ? 1 : 0;
        if (! disableCertificateValidation) {
            // create an http client that validates certificates
            httpClient = HttpAsyncClients.custom()
                    .setMaxConnTotal(maxConnTotal)
                    .build();
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
                        .setMaxConnTotal(maxConnTotal)
                        .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                        .setSSLContext(sslContext)
                        .build();
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

    private void postEventsAsync(final List<HttpEventCollectorEventInfo> events) {
        this.middleware.postEvents(events, this, new HttpEventCollectorMiddleware.IHttpSenderCallback() {
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
                        eventsBatch,
                        new HttpEventCollectorErrorHandler.ServerErrorException(ex.getMessage()));
            }
        });
    }

    public void postEvents(final List<HttpEventCollectorEventInfo> events,
                           final HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        startHttpClient(); // make sure http client is started
        final String encoding = "utf-8";
        // convert events list into a string
        StringBuilder eventsBatchString = new StringBuilder();
        for (HttpEventCollectorEventInfo eventInfo : events)
            eventsBatchString.append(serializeEventInfo(eventInfo));
        // create http request
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(
                AuthorizationHeaderTag,
                String.format(AuthorizationHeaderScheme, token));
        StringEntity entity = new StringEntity(eventsBatchString.toString(), encoding);
        entity.setContentType(HttpContentType);
        httpPost.setEntity(entity);
        httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse response) {
                String reply = "";
                int httpStatusCode = response.getStatusLine().getStatusCode();
                // read reply only in case of a server error
                if (httpStatusCode != 200) {
                    try {
                        reply = EntityUtils.toString(response.getEntity(), encoding);
                    } catch (IOException e) {
                        reply = e.getMessage();
                    }
                }
                callback.completed(httpStatusCode, reply);
            }

            @Override
            public void failed(Exception ex) {
                callback.failed(ex);
            }

            @Override
            public void cancelled() {}
        });
    }
}
