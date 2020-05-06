package com.splunk.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.splunk.logging.serialization.EventInfoTypeAdapter;
import com.splunk.logging.serialization.HecJsonSerializer;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateException;
import java.util.*;

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

public abstract class AHttpEventCollectorSender
        extends TimerTask implements IHttpEventCollectorSender {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(HttpEventCollectorEventInfo.class, new EventInfoTypeAdapter())
            .create();
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

    protected static OkHttpClient httpClient = null;
    protected final String url;
    protected final String token;
    protected final String channel;
    protected final String type;

    protected long maxEventsBatchCount = Long.MAX_VALUE;
    protected long maxEventsBatchSize = Long.MAX_VALUE;
    protected HttpEventCollectorMiddleware middleware = new HttpEventCollectorMiddleware();

    protected List<HttpEventCollectorEventInfo> eventsBatch = new LinkedList<>();
    protected long eventsBatchSize = 0; // estimated total size of events batch


    protected final HecJsonSerializer serializer;
    private Timer timer;
    private boolean disableCertificateValidation = false;

    public AHttpEventCollectorSender(final String url, final String token, final String channel, final String type,
                                     long delay, long maxEventsBatchCount, long maxEventsBatchSize, String sendModeStr,
                                     Map<String, String> metadata) {
        this.url = "Raw".equalsIgnoreCase(type) ? url + HttpRawCollectorUriPath : url + HttpEventCollectorUriPath;
        this.token = token;
        this.channel = channel;
        this.type = type;

        // when size configuration setting is missing it's treated as "infinity",
        // i.e., any value is accepted.
        if (maxEventsBatchCount > 0 || maxEventsBatchSize == 0) {
            this.maxEventsBatchCount = maxEventsBatchCount;
        }
        if (maxEventsBatchSize > 0 || maxEventsBatchCount == 0) {
            this.maxEventsBatchSize = maxEventsBatchSize;
        }

        this.serializer = new HecJsonSerializer(metadata);

        if (sendModeStr != null) {
            if (sendModeStr.equals(SendModeSequential))
                this.sendMode = HttpEventCollectorSender.SendMode.Sequential;
            else if (sendModeStr.equals(SendModeSParallel))
                this.sendMode = HttpEventCollectorSender.SendMode.Parallel;
            else
                throw new IllegalArgumentException("Unknown send mode: " + sendModeStr);
        }

        if (delay > 0) {
            // start heartbeat timer
            timer = new Timer(true);
            timer.scheduleAtFixedRate(this, delay, delay);
        }
    }

    protected abstract void postEventsMiddleware(List<HttpEventCollectorEventInfo> eventsBatch);

    public void addMiddleware(HttpEventCollectorMiddleware.HttpSenderMiddleware middleware) {
        this.middleware.add(middleware);
    }

    public void setEventBodySerializer(EventBodySerializer eventBodySerializer) {
        serializer.setEventBodySerializer(eventBodySerializer);
    }

    public void disableCertificateValidation() {
        this.disableCertificateValidation = true;
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

    /**
     * Send a single logging event in case of batching the event isn't sent immediately
     *
     * @param severity event severity level (info, warning, etc.)
     * @param message  event text
     */
    @Override
    public void send(String severity, String message, String logger_name, String thread_name, Map<String, String> properties, String exception_message, Serializable marker) {
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
     *
     * @param message event text
     */
    public synchronized void send(final String message) {
        send("", message, "", "", null, null, "");
    }

    protected OkHttpClient.Builder buildOkHttpClient() {
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

            builder.hostnameVerifier((hostname, session) -> true);
        }

        return builder;
    }

    protected synchronized void startHttpClient() {
        if (httpClient != null) {
            // http client is already started
            return;
        }

        httpClient = buildOkHttpClient().build();
    }

    @Override
    public synchronized void flush() {
        if (eventsBatch.size() == 0) {
            return;
        }
        postEventsMiddleware(eventsBatch);

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

    @Override
    public void run() {
        flush();
    }


    @Override
    public void close() {
        if (timer != null) {
            timer.cancel();
        }
        flush();
        stopHttpClient();
        super.cancel();
    }

    private void stopHttpClient() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient = null;
        }
    }

    protected Request.Builder requestBuilder(List<HttpEventCollectorEventInfo> events) {
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
        return requestBldr;
    }

    static HttpEventCollectorMiddleware.HttpSenderSuccess responseSuccess(Response response) {
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
        return new HttpEventCollectorMiddleware.HttpSenderSuccess(response.code(), reply);
    }
}
