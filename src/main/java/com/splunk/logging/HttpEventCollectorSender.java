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

import com.google.gson.*;
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
public class HttpEventCollectorSender extends TimerTask implements HttpEventCollectorMiddleware.IHttpSender {
    private static final String SPLUNKREQUESTCHANNELTag = "X-Splunk-Request-Channel";
    private static final String AuthorizationHeaderTag = "Authorization";
    private static final String AuthorizationHeaderScheme = "Splunk %s";
    private static final String HttpEventCollectorUriPath = "/services/collector/event/1.0";
    private static final String HttpRawCollectorUriPath = "/services/collector/raw";
    private static final String HttpContentType = "application/json; profile=urn:splunk:event:1.0; charset=utf-8";
    private static final String SendModeSequential = "sequential";
    private static final String SendModeSParallel = "parallel";
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(HttpEventCollectorEventInfo.class, new EventInfoTypeAdapter())
            .create();

    private final HecJsonSerializer serializer;


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
     * Recommended default values for events batching.
     */
    public static final int DefaultBatchInterval = 10 * 1000; // 10 seconds
    public static final int DefaultBatchSize = 10 * 1024; // 10KB
    public static final int DefaultBatchCount = 10; // 10 events

    private String url;
    private String token;
    private String channel;
    private String type;
    private long maxEventsBatchCount;
    private long maxEventsBatchSize;
    private Timer timer;
    private List<HttpEventCollectorEventInfo> eventsBatch = new LinkedList<HttpEventCollectorEventInfo>();
    private long eventsBatchSize = 0; // estimated total size of events batch
    private static OkHttpClient httpClient = null;
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
     * @param channel unique GUID for the client to send raw events to the server
     * @param type event data type
     */
    public HttpEventCollectorSender(
            final String Url, final String token, final String channel, final String type,
            long delay, long maxEventsBatchCount, long maxEventsBatchSize,
            String sendModeStr,
            Map<String, String> metadata) {
        this.url = Url + HttpEventCollectorUriPath;
        this.token = token;
        this.channel = channel;
        this.type = type;

        if ("Raw".equalsIgnoreCase(type)) {
            this.url = Url + HttpRawCollectorUriPath;
        }

        // when size configuration setting is missing it's treated as "infinity",
        // i.e., any value is accepted.
        if (maxEventsBatchCount == 0 && maxEventsBatchSize > 0) {
            maxEventsBatchCount = Long.MAX_VALUE;
        } else if (maxEventsBatchSize == 0 && maxEventsBatchCount > 0) {
            maxEventsBatchSize = Long.MAX_VALUE;
        }
        this.maxEventsBatchCount = maxEventsBatchCount;
        this.maxEventsBatchSize = maxEventsBatchSize;

        serializer = new HecJsonSerializer(metadata);
        final String format = metadata.get(MetadataTags.MESSAGEFORMAT);
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
     * Send a single logging event in case of batching the event isn't sent immediately
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
     * Send a single logging event with message only in case of batching the event isn't sent immediately
     * @param message event text
     */
    public synchronized void send(final String message) {
        send("", message, "", "", null, null, "");
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
        eventsBatch = new LinkedList<>();
        eventsBatchSize = 0;
    }

    public synchronized void flush(boolean close) {
        flush();
        if (close) {
            stopHttpClient();
        }
    }

    /**
     * Close events sender
     */
    void close() {
        if (timer != null)
            timer.cancel();
        flush();
        stopHttpClient();
        super.cancel();
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

    public void setEventBodySerializer(EventBodySerializer eventBodySerializer) {
        serializer.setEventBodySerializer(eventBodySerializer);
    }

    public void setEventHeaderSerializer(EventHeaderSerializer eventHeaderSerializer) {
        serializer.setEventHeaderSerializer(eventHeaderSerializer);
    }

    public static void putIfPresent(JsonObject collection, String tag, Object value) {
        if (value != null) {
            if (value instanceof String && ((String) value).length() == 0) {
                // Do not add blank string
                return;
            }
            collection.add(tag, gson.toJsonTree(value));
        }
    }


    private void stopHttpClient() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient = null;
        }
    }

    private void startHttpClient() {
        if (httpClient != null) {
            // http client is already started
            return;
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // limit max  number of async requests in sequential mode
        if (sendMode == SendMode.Sequential) {
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(1);
            builder.dispatcher(dispatcher);
        }

        if (disableCertificateValidation) {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            try {
                // install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // create an ssl socket factory with the all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            } catch (Exception ignored) { /* nop */ }

            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        httpClient = builder.build();
    }

    private void postEventsAsync(final List<HttpEventCollectorEventInfo> events) {
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

    public void postEvents(final List<HttpEventCollectorEventInfo> events,
                           final HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        startHttpClient(); // make sure http client is started
        // convert events list into a string
        StringBuilder eventsBatchString = new StringBuilder();
        for (HttpEventCollectorEventInfo eventInfo : events) {
            eventsBatchString.append(serializer.serialize(eventInfo));
        }
        // create http request
        Request.Builder requestBldr = new Request.Builder()
                .url(url)
                .addHeader(AuthorizationHeaderTag, String.format(AuthorizationHeaderScheme, token))
                .post(RequestBody.create(MediaType.parse(HttpContentType), eventsBatchString.toString()));

        if ("Raw".equalsIgnoreCase(type) && channel != null && !channel.trim().equals("")) {
            requestBldr.addHeader(SPLUNKREQUESTCHANNELTag, channel);
        }

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
