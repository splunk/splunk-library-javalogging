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

import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * An input handler for Splunk http event collector. This handler can be used by
 * by specifying handlers = com.splunk.logging.HttpEventCollectorLoggingHandler in java.util.logging
 * properties file.
 */
public final class HttpEventCollectorLoggingHandler extends Handler {
    private HttpEventCollectorSender sender = null;
    private final String IncludeLoggerNameConfTag = "include_logger_name";
    private final boolean includeLoggerName;
    private final String IncludeThreadNameConfTag = "include_thread_name";
    private final boolean includeThreadName;
    private final String IncludeExceptionConfTag = "include_exception";
    private final boolean includeException;


    private final String BatchDelayConfTag = "batch_interval";
    private final String BatchCountConfTag = "batch_size_count";
    private final String BatchSizeConfTag = "batch_size_bytes";
    private final String RetriesOnErrorTag = "retries_on_error";
    private final String UrlConfTag = "url";
    private final String SendModeTag = "send_mode";
    private final String MiddlewareTag = "middleware";

    private final String ConnectTimeoutConfTag = "connect_timeout";
    private final String CallTimeoutConfTag = "call_timeout";
    private final String ReadTimeoutConfTag = "read_timeout";
    private final String WriteTimeoutConfTag = "write_timeout";
    private final String TerminationTimeoutConfTag = "termination_timeout";

    /** HttpEventCollectorLoggingHandler c-or */
    public HttpEventCollectorLoggingHandler() {
        // read configuration settings
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataTags.HOST,
                getConfigurationProperty(MetadataTags.HOST, null));

        metadata.put(MetadataTags.INDEX,
                getConfigurationProperty(MetadataTags.INDEX, null));

        metadata.put(MetadataTags.SOURCE,
                getConfigurationProperty(MetadataTags.SOURCE, null));

        metadata.put(MetadataTags.SOURCETYPE,
                getConfigurationProperty(MetadataTags.SOURCETYPE, null));

        // Extract message format value
        metadata.put(MetadataTags.MESSAGEFORMAT,
            getConfigurationProperty(MetadataTags.MESSAGEFORMAT, null));

        // http event collector endpoint properties
        String url = getConfigurationProperty(UrlConfTag, null);

        // app token
        String token = getConfigurationProperty("token", null);

        //app channel
        String channel = getConfigurationProperty("channel", null);

        //app type
        String type = getConfigurationProperty("type", null);

        // batching properties
        long delay = getConfigurationNumericProperty(BatchDelayConfTag, HttpEventCollectorSender.DefaultBatchInterval);
        long batchCount = getConfigurationNumericProperty(BatchCountConfTag, HttpEventCollectorSender.DefaultBatchCount);
        long batchSize = getConfigurationNumericProperty(BatchSizeConfTag, HttpEventCollectorSender.DefaultBatchSize);
        long retriesOnError = getConfigurationNumericProperty(RetriesOnErrorTag, 0);
        String sendMode = getConfigurationProperty(SendModeTag, "sequential");
        String eventHeaderSerializer = getConfigurationProperty("eventHeaderSerializer", "");
        String middleware = getConfigurationProperty(MiddlewareTag, null);
        String eventBodySerializer = getConfigurationProperty("eventBodySerializer", null);

        includeLoggerName = getConfigurationBooleanProperty(IncludeLoggerNameConfTag, true);
        includeThreadName = getConfigurationBooleanProperty(IncludeThreadNameConfTag, true);
        includeException = getConfigurationBooleanProperty(IncludeExceptionConfTag, true);

        HttpEventCollectorSender.TimeoutSettings timeoutSettings = new HttpEventCollectorSender.TimeoutSettings(
            getConfigurationNumericProperty(ConnectTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_CONNECT_TIMEOUT),
            getConfigurationNumericProperty(CallTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_CALL_TIMEOUT),
            getConfigurationNumericProperty(ReadTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_READ_TIMEOUT),
            getConfigurationNumericProperty(WriteTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_WRITE_TIMEOUT),
            getConfigurationNumericProperty(TerminationTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_TERMINATION_TIMEOUT)
        );

        if ("raw".equalsIgnoreCase(type)) {
            if (batchCount != HttpEventCollectorSender.DefaultBatchCount
                        || batchSize != HttpEventCollectorSender.DefaultBatchSize
                        || delay != HttpEventCollectorSender.DefaultBatchInterval) {
                throw new IllegalArgumentException("Batching configuration and sending type of raw are incompatible.");
            }
            batchCount = 1;
        }

        // delegate all configuration params to event sender
        this.sender = new HttpEventCollectorSender(
                url, token, channel, type, delay, batchCount, batchSize, sendMode, metadata, timeoutSettings);

        // plug a user middleware
        if (middleware != null && !middleware.isEmpty()) {
            try {
                this.sender.addMiddleware((HttpEventCollectorMiddleware.HttpSenderMiddleware)(Class.forName(middleware).newInstance()));
            } catch (Exception ignored) {}
        }

        if (eventBodySerializer != null && !eventBodySerializer.isEmpty()) {
            try {
                this.sender.setEventBodySerializer((EventBodySerializer) Class.forName(eventBodySerializer).newInstance());
            } catch (final Exception ex) {
                //output error msg but not fail, it will default to use the default EventBodySerializer
                System.out.println(ex);
            }
        }

        if (eventHeaderSerializer != null && !eventHeaderSerializer.isEmpty()) {
            try {
                this.sender.setEventHeaderSerializer((EventHeaderSerializer) Class.forName(eventHeaderSerializer).newInstance());
            } catch (final Exception ex) {
                //output error msg but not fail, it will default to use the default EventHeaderSerializer
                System.out.println(ex);
            }
        }

        // plug retries middleware
        if (retriesOnError > 0) {
            this.sender.addMiddleware(new HttpEventCollectorResendMiddleware(retriesOnError));
        }

        if (getConfigurationProperty("disableCertificateValidation", "false").equalsIgnoreCase("true")) {
            this.sender.disableCertificateValidation();
        }
    }

    /**
     * java.util.logging data handler callback
     * @param record is a logging record
     */
    @Override
    public void publish(LogRecord record) {
        this.sender.send(
        		record.getMillis(),
                record.getLevel().toString(),
                record.getMessage(),
                includeLoggerName ? record.getLoggerName() : null,
                includeThreadName ? String.format(Locale.US, "%d", record.getThreadID()) : null,
                null, // no property map available
                (!includeException || record.getThrown() == null) ? null : record.getThrown().getMessage(),
                null // no marker available
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
     * @throws SecurityException throw security exception
     */
    @Override
    public void close() throws SecurityException {
        this.sender.close();
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
        return value;
    }

    private long getConfigurationNumericProperty(
            final String property, long defaultValue) {
        return Integer.parseInt(
                getConfigurationProperty(property, String.format("%d", defaultValue)));
    }

    private boolean getConfigurationBooleanProperty(
            final String property, boolean defaultValue) {
        return Boolean.parseBoolean(
                getConfigurationProperty(property, String.valueOf(defaultValue)));
    }
}
