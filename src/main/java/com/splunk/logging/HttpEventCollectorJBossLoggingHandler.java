package com.splunk.logging;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

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
 * A handler for java.util.logging that works with Splunk http event collector.
 *
 * details
 * This is a Splunk custom java.util.logging handler that intercepts logging
 * information and forwards it to a Splunk server through http event collector.
 * @todo - link to http event collector documentation
 * java.util.logging is configure by specifying java.util.logging.config.file
 * properties file. For example:
 * -Djava.util.logging.config.file=splunk-http-input.properties
 * Properties file has include logging handler and its properties.
 *
 * # Splunk http event collector handler
 * handlers = com.splunk.logging.HttpEventCollectorHandler
 *
 * # Http event collector application token
 * com.splunk.logging.HttpEventCollectorLoggingHandler.token=<token guid>
 *
 * # Splunk logging input url.
 * com.splunk.logging.HttpEventCollectorHandler.url
 *
 * # Logging events metadata.
 * com.splunk.logging.HttpEventCollectorLoggingHandler.index
 * com.splunk.logging.HttpEventCollectorLoggingHandler.source
 * com.splunk.logging.HttpEventCollectorLoggingHandler.sourcetype
 *
 * # Events batching parameters:
 * # Delay in millisecond between sending events, by default this value is 0, i.e., and events
 * # are sending immediately
 * com.splunk.logging.HttpEventCollectorLoggingHandler.batch_interval
 *
 * # Max number of events in a batch. By default - 0, i.e., no batching
 * com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count
 *
 * # Max size of events in a batch. By default - 0, i.e., no batching
 * com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_bytes
 *
 * An example of logging properties file:
 * handlers = com.splunk.logging.HttpEventCollectorLoggingHandler
 * com.splunk.logging.HttpEventCollectorLoggingHandler.token=81029a58-63db-4bef-9c6f-f6b7e500f098
 *
 * # Splunk server
 * com.splunk.logging.HttpEventCollectorLoggingHandler.url=https://localhost:8089
 *
 * # Metadata
 * com.splunk.logging.HttpEventCollectorLoggingHandler.index=default
 * com.splunk.logging.HttpEventCollectorLoggingHandler.source=localhost
 * com.splunk.logging.HttpEventCollectorLoggingHandler.sourcetype=syslog
 *
 * # Batching
 * com.splunk.logging.HttpEventCollectorLoggingHandler.batch_interval = 500
 * com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count = 1000
 * com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count = 65536
 *
 * # To improve system performance tracing events are sent asynchronously and
 * events with the same timestamp (that has 1 millisecond resolution) may
 * be indexed out of order by Splunk. send_mode parameter triggers
 * "sequential mode" that guarantees preserving events order. In
 * "sequential mode" performance of sending events to the server is lower.
 * com.splunk.logging.HttpEventCollectorLoggingHandler.send_mode=sequential
 */

import com.splunk.logging.hec.MetadataTags;

/**
 * An input handler for Splunk http event collector. This handler can be used by by specifying handlers = com.splunk.logging.HttpEventCollectorLoggingHandler in java.util.logging properties file.
 */
public class HttpEventCollectorJBossLoggingHandler extends Handler {
    private String url;
    private String token;
    private String channel;
    private String type;
    private long delay = HttpEventCollectorSender.DefaultBatchInterval;
    private long batchCount = HttpEventCollectorSender.DefaultBatchCount;
    private long batchSize = HttpEventCollectorSender.DefaultBatchSize;
    private long retriesOnError;
    private String sendMode = "sequential";
    private String eventHeaderSerializer = "";
    private String middleware;
    private String eventBodySerializer;
    private String errorCallbackClass;

    private boolean includeLoggerName = true;
    private boolean includeThreadName = true;
    private boolean includeException = true;

    // Metadata
    private String host;
    private String index;
    private String source;
    private String sourcetype;
    private String messageFormat;

    // Timeout settings
    private long connectTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_CONNECT_TIMEOUT;
    private long callTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_CALL_TIMEOUT;
    private long readTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_READ_TIMEOUT;
    private long writeTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_WRITE_TIMEOUT;
    private long terminationTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_TERMINATION_TIMEOUT;

    private boolean disableCertificateValidation;

    private HttpEventCollectorSender sender = null;

    private void init() {
        // Metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataTags.HOST, host);
        metadata.put(MetadataTags.INDEX, index);
        metadata.put(MetadataTags.SOURCE, source);
        metadata.put(MetadataTags.SOURCETYPE, sourcetype);
        // Extract message format value
        metadata.put(MetadataTags.MESSAGEFORMAT, messageFormat);

        // Timeout settings
        HttpEventCollectorSender.TimeoutSettings timeoutSettings = new HttpEventCollectorSender.TimeoutSettings(connectTimeout, callTimeout, readTimeout, writeTimeout, terminationTimeout);

        if ("raw".equalsIgnoreCase(type)) {
            if (batchCount != HttpEventCollectorSender.DefaultBatchCount || batchSize != HttpEventCollectorSender.DefaultBatchSize || delay != HttpEventCollectorSender.DefaultBatchInterval) {
                throw new IllegalArgumentException("Batching configuration and sending type of raw are incompatible.");
            }
            batchCount = 1;
        }

        // delegate all configuration params to event sender
        this.sender = new HttpEventCollectorSender(url, token, channel, type, delay, batchCount, batchSize, sendMode, metadata, timeoutSettings);

        // plug a user middleware
        if (middleware != null && !middleware.isEmpty()) {
            try {
                this.sender.addMiddleware((HttpEventCollectorMiddleware.HttpSenderMiddleware) (Class.forName(middleware).newInstance()));
            }
            catch (Exception ignored) {
            }
        }

        if (eventBodySerializer != null && !eventBodySerializer.isEmpty()) {
            try {
                this.sender.setEventBodySerializer((EventBodySerializer) Class.forName(eventBodySerializer).newInstance());
            }
            catch (final Exception ex) {
                // output error msg but not fail, it will default to use the default EventBodySerializer
                System.out.println(ex);
            }
        }

        if (eventHeaderSerializer != null && !eventHeaderSerializer.isEmpty()) {
            try {
                this.sender.setEventHeaderSerializer((EventHeaderSerializer) Class.forName(eventHeaderSerializer).newInstance());
            }
            catch (final Exception ex) {
                // output error msg but not fail, it will default to use the default EventHeaderSerializer
                System.out.println(ex);
            }
        }

        if (errorCallbackClass != null && !errorCallbackClass.isEmpty()) {
            try {
                HttpEventCollectorErrorHandler.registerClassName(errorCallbackClass);
            }
            catch (final Exception ex) {
                // output error msg but not fail, it will default to use the default EventHeaderSerializer
                System.out.println(ex);
            }
        }

        // plug retries middleware
        if (retriesOnError > 0) {
            this.sender.addMiddleware(new HttpEventCollectorResendMiddleware(retriesOnError));
        }

        if (disableCertificateValidation) {
            this.sender.disableCertificateValidation();
        }
    }

    /**
     * java.util.logging data handler callback
     *
     * @param record is a logging record
     */
    @Override
    public void publish(LogRecord record) {
        if (this.sender == null) {
            init();
        }
        this.sender.send(record.getMillis(), record.getLevel().toString(), getFormatter() == null ? record.getMessage() : getFormatter().format(record),
            includeLoggerName ? record.getLoggerName() : null, includeThreadName ? String.format(Locale.US, "%d", record.getThreadID()) : null, null, // no property map available
            (!includeException || record.getThrown() == null) ? null : record.getThrown().getMessage(), null // no marker available
        );
    }

    /**
     * java.util.logging data handler callback
     */
    @Override
    public void flush() {
        this.sender.flush();
    }

    /**
     * java.util.logging data handler close callback
     *
     * @throws SecurityException throw security exception
     */
    @Override
    public void close() throws SecurityException {
        this.sender.close();
    }

    /**** Setters only are needed ****/
    public void setUrl(String url) {
        this.url = url;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public void setBatchCount(long batchCount) {
        this.batchCount = batchCount;
    }

    public void setBatchSize(long batchSize) {
        this.batchSize = batchSize;
    }

    public void setRetriesOnError(long retriesOnError) {
        this.retriesOnError = retriesOnError;
    }

    public void setSendMode(String sendMode) {
        this.sendMode = sendMode;
    }

    public void setEventHeaderSerializer(String eventHeaderSerializer) {
        this.eventHeaderSerializer = eventHeaderSerializer;
    }

    public void setMiddleware(String middleware) {
        this.middleware = middleware;
    }

    public void setEventBodySerializer(String eventBodySerializer) {
        this.eventBodySerializer = eventBodySerializer;
    }

    public void setErrorCallbackClass(String errorCallbackClass) {
        this.errorCallbackClass = errorCallbackClass;
    }

    public void setIncludeLoggerName(boolean includeLoggerName) {
        this.includeLoggerName = includeLoggerName;
    }

    public void setIncludeThreadName(boolean includeThreadName) {
        this.includeThreadName = includeThreadName;
    }

    public void setIncludeException(boolean includeException) {
        this.includeException = includeException;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
    }

    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setCallTimeout(long callTimeout) {
        this.callTimeout = callTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setWriteTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public void setTerminationTimeout(long terminationTimeout) {
        this.terminationTimeout = terminationTimeout;
    }

    public void setDisableCertificateValidation(boolean disableCertificateValidation) {
        this.disableCertificateValidation = disableCertificateValidation;
    }

    public void setSender(HttpEventCollectorSender sender) {
        this.sender = sender;
    }
}
