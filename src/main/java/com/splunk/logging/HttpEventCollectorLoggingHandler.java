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

import com.google.gson.Gson;
import com.splunk.logging.hec.MetadataTags;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.*;

/**
 * An input handler for Splunk http event collector. This handler can be used by
 * by specifying handlers = com.splunk.logging.HttpEventCollectorLoggingHandler in java.util.logging
 * properties file.
 */
public final class HttpEventCollectorLoggingHandler extends Handler {
    private HttpEventCollectorSender sender = null;
    private final String includeLoggerNameConfTag = "include_logger_name";
    private final boolean includeLoggerName;
    private final String includeThreadNameConfTag = "include_thread_name";
    private final boolean includeThreadName;
    private final String includeExceptionConfTag = "include_exception";
    private boolean includeException;


    private final String batchDelayConfTag = "batch_interval";
    private final String batchCountConfTag = "batch_size_count";
    private final String batchSizeConfTag = "batch_size_bytes";
    private final String retriesOnErrorTag = "retries_on_error";
    private final String urlConfTag = "url";
    private final String sendModeTag = "send_mode";
    private final String middlewareTag = "middleware";

    private final String connectTimeoutConfTag = "connect_timeout";
    private final String callTimeoutConfTag = "call_timeout";
    private final String readTimeoutConfTag = "read_timeout";
    private final String writeTimeoutConfTag = "write_timeout";
    private final String terminationTimeoutConfTag = "termination_timeout";

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
        String url = getConfigurationProperty(urlConfTag, null);

        // app token
        String token = getConfigurationProperty("token", null);

        //app channel
        String channel = getConfigurationProperty("channel", null);

        //app type
        String type = getConfigurationProperty("type", null);

        // batching properties
        long delay = getConfigurationNumericProperty(batchDelayConfTag, HttpEventCollectorSender.DefaultBatchInterval);
        long batchCount = getConfigurationNumericProperty(batchCountConfTag, HttpEventCollectorSender.DefaultBatchCount);
        long batchSize = getConfigurationNumericProperty(batchSizeConfTag, HttpEventCollectorSender.DefaultBatchSize);
        long retriesOnError = getConfigurationNumericProperty(retriesOnErrorTag, 0);
        String sendMode = getConfigurationProperty(sendModeTag, "sequential");
        String eventHeaderSerializer = getConfigurationProperty("eventHeaderSerializer", "");
        String middleware = getConfigurationProperty(middlewareTag, null);
        String eventBodySerializer = getConfigurationProperty("eventBodySerializer", null);
        String errorCallbackClass = getConfigurationProperty("errorCallback", null);

        includeLoggerName = getConfigurationBooleanProperty(includeLoggerNameConfTag, true);
        includeThreadName = getConfigurationBooleanProperty(includeThreadNameConfTag, true);
        includeException = getConfigurationBooleanProperty(includeExceptionConfTag, true);

        HttpEventCollectorSender.TimeoutSettings timeoutSettings = new HttpEventCollectorSender.TimeoutSettings(
            getConfigurationNumericProperty(connectTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_CONNECT_TIMEOUT),
            getConfigurationNumericProperty(callTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_CALL_TIMEOUT),
            getConfigurationNumericProperty(readTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_READ_TIMEOUT),
            getConfigurationNumericProperty(writeTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_WRITE_TIMEOUT),
            getConfigurationNumericProperty(terminationTimeoutConfTag, HttpEventCollectorSender.TimeoutSettings.DEFAULT_TERMINATION_TIMEOUT)
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

        if (errorCallbackClass != null && !errorCallbackClass.isEmpty()) {
            try {
                HttpEventCollectorErrorHandler.registerClassName(errorCallbackClass);
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

        boolean isExceptionOccured = false;
        String exceptionDetail = null;
        /*
        Exception details are only populated when any SEVERE error occurred & exception is actually thrown
         */
        if (Level.SEVERE.equals(record.getLevel()) && record.getThrown() != null) {

            // Exception thrown in application is wrapped with relevant information instead of just a message.
            Map<Object, Object> exceptionDetailMap = new LinkedHashMap<>();
            StackTraceElement[] elements = record.getThrown().getStackTrace();
            exceptionDetailMap.put("detailMessage", record.getThrown().getMessage());
            exceptionDetailMap.put("exceptionClass", record.getThrown().getClass().toString());

            // Retrieving first element from elements array is because the throws exception detail would be available as a first element.
            if (elements != null && elements.length > 0 && elements[0] != null) {
                exceptionDetailMap.put("fileName", elements[0].getFileName());
                exceptionDetailMap.put("lineNumber", String.valueOf(elements[0].getLineNumber()));
                exceptionDetailMap.put("methodName", elements[0].getMethodName());
            }
            exceptionDetail = new Gson().toJson(exceptionDetailMap);
            isExceptionOccured = true;
        }

        /*
        Initializing a formatter for Java Util Logging.
        This will be used when placeholders are used for event logging in log methods.
         */
        Formatter messageFormatter = getFormatter();
        if (messageFormatter == null) {
            messageFormatter = new SimpleFormatter();
        }

        this.sender.send(
                record.getMillis(),
                record.getLevel().toString(),
                messageFormatter.formatMessage(record),
                includeLoggerName ? record.getLoggerName() : null,
                includeThreadName ? String.format(Locale.US, "%d", record.getThreadID()) : null,
                null, // no property map available
                (includeException && isExceptionOccured) ? exceptionDetail : null,
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
