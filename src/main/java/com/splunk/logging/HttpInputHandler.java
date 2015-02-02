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

/**
 * @brief A hadler for java.util.logging that works with Splunk http input.
 *
 * @details
 * This is a Splunk custom java.util.logging handler that intercepts logging
 * information and forwards it to a Splunk server through http input.
 * @todo - link to http input documentation
 * java.util.logging is configure by specifying java.util.logging.config.file
 * properties file. For example:
 * -Djava.util.logging.config.file=splunk-http-input.properties
 * Properties file has include logging handler and its properties.
 *
 * # Splunk http input logging handler
 * handlers = com.splunk.logging.HttpInputHandler
 *
 * # Http input application token
 * com.splunk.logging.HttpInputHandler.token=<token guid>
 *
 * # Splunk server scheme. This parameter is optional and its default value is https.
 * com.splunk.logging.HttpInputHandler.scheme=http|https
 *
 * # Splunk server host name. Host parameter is mandatory and cannot be omitted.
 * com.splunk.logging.HttpInputHandler.host=<host>
 *
 * # Splunk server port id. Default port value is 8089.
 * com.splunk.logging.HttpInputHandler.port=<port>
 *
 * # Logging events metadata.
 * com.splunk.logging.HttpInputHandler.index
 * com.splunk.logging.HttpInputHandler.source
 * com.splunk.logging.HttpInputHandler.sourcetype
 *
 * An example of logging properties file:
 * handlers = com.splunk.logging.HttpInputHandler
 * com.splunk.logging.HttpInputHandler.token=81029a58-63db-4bef-9c6f-f6b7e500f098
 *
 * # Splunk server
 * com.splunk.logging.HttpInputHandler.scheme=https
 * com.splunk.logging.HttpInputHandler.host=localhost
 * com.splunk.logging.HttpInputHandler.port=8089
 *
 * # Metadata
 * com.splunk.logging.HttpInputHandler.index=default
 * com.splunk.logging.HttpInputHandler.source=localhost
 * com.splunk.logging.HttpInputHandler.sourcetype=syslog
 */

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.json.simple.JSONObject;

/**
 * An input handler for Splunk http input logging. This handler can be used by
 * by specifying handlers = com.splunk.logging.HttpInputHandler in java.util.logging
 * properties file.
 */
public final class HttpInputHandler extends Handler {
    private final String DefaultScheme = "https";
    private final String DefaultPort = "8089";
    private final String HttpInputUrlPath = "/services/logging";
    private final String MetadataTimeTag = "time";
    private final String MetadataIndexTag = "index";
    private final String MetadataSourceTag = "source";
    private final String MetadataSourceTypeTag = "sourcetype";
    private final String AuthorizationHeaderTag = "Authorization";
    private final String AuthorizationHeaderScheme = "Splunk %s";

    private String httpInputUrl; // full url of an http input endpoint
    private String token; // http input application token

    // events metadata information
    private String index;
    private String source;
    private String sourceType;

    private CloseableHttpAsyncClient httpClient;

    /** HttpInputHandler c-or */
    public HttpInputHandler() {
        readConfiguration();
    }

    /**
     * java.util.logging data handler callback
     * @param record is a logging record
     */
    @Override
    public void publish(LogRecord record) {
        String severity = record.getLevel().toString();
        String message = record.getMessage();
        String event = createEvent(severity, message);
        postEventAsync(event);
    }

    /**
     * java.util.logging data handler close callback
     * @throws SecurityException
     */
    @Override public void close() throws SecurityException {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {}
            httpClient = null;
        }
    }

    private void postEventAsync(final String event) {
        startHttpClient();
        HttpPost httpPost = new HttpPost(httpInputUrl);
        httpPost.setHeader(
                AuthorizationHeaderTag,
                String.format(AuthorizationHeaderScheme, token));
        StringEntity entity = new StringEntity(event, "utf-8");
        entity.setContentType("application/json; charset=utf-8");
        httpPost.setEntity(entity);
        httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
            // @todo - handle reply
            public void completed(final HttpResponse response) {}
            public void failed(final Exception ex) {}
            public void cancelled() {}
        });
    }

    private void startHttpClient() {
        if (httpClient != null) {
            // http client is already started
            return;
        }
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
    }

    private String createEvent(final String severity, final String message) {
        // create event json content
        JSONObject event = new JSONObject();
        // event timestamp and metadata
        event.put(MetadataTimeTag, String.format("%d", System.currentTimeMillis() / 1000));
        if (index.length() > 0)
            event.put(MetadataIndexTag, index);
        if (source.length() > 0)
            event.put(MetadataSourceTag, source);
        if (sourceType.length() > 0)
            event.put(MetadataSourceTypeTag, sourceType);
        // event body
        JSONObject body = new JSONObject();
        body.put("severity", severity);
        body.put("message", message);
        // join event and body
        event.put("event", body);
        return event.toString();
    }

    private void readConfiguration() {
        // reconstruct http endpoint url
        String scheme = getConfigurationProperty("scheme", DefaultScheme);
        String host = getConfigurationProperty("host", null);
        String port = getConfigurationProperty("port", DefaultPort);
        httpInputUrl = String.format("%s://%s:%s%s",
                scheme, host, port, HttpInputUrlPath);
        // app token
        token = getConfigurationProperty("token", null);
        // metadata
        index = getConfigurationProperty(MetadataIndexTag, "");
        source = getConfigurationProperty(MetadataSourceTag, "");
        sourceType = getConfigurationProperty(MetadataSourceTypeTag, "");
    }

    // get configuration property from java.util.logging properties file,
    // defaultValue == null means that property is mandatory
    private String getConfigurationProperty(
            final String property, final String defaultValue) {
        String value = LogManager.getLogManager().getProperty(
            getClass().getName() + '.' + property
        );
        if (value == null) {
            value = defaultValue;
        }
        if (value == null) {
            throw new IllegalArgumentException(String.format(
                    "Configuration property %s is missing", property));
        }
        return value;
    }

    // java.util.logging.Handler abstract methods
    @Override public void flush() {}
}
