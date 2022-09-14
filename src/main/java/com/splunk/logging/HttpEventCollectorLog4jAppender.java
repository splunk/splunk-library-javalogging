package com.splunk.logging;
/*
 * Copyright 2013-2014 Splunk, Inc.
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

import com.google.gson.Gson;
import com.splunk.logging.hec.MetadataTags;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Splunk Http Appender.
 */
@Plugin(name = "SplunkHttp", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("serial")
public final class HttpEventCollectorLog4jAppender extends AbstractAppender
{
    private HttpEventCollectorSender sender = null;
    private final boolean includeLoggerName;
    private final boolean includeThreadName;
    private final boolean includeMDC;
    private final boolean includeException;
    private final boolean includeMarker;

    private HttpEventCollectorLog4jAppender(final String name,
                                            final String url,
                                            final String token,
                                            final String channel,
                                            final String type,
                                            final String source,
                                            final String sourcetype,
                                            final String messageFormat,
                                            final String host,
                                            final String index,
                                            final Filter filter,
                                            final Layout<? extends Serializable> layout,
                                            final boolean includeLoggerName,
                                            final boolean includeThreadName,
                                            final boolean includeMDC,
                                            final boolean includeException,
                                            final boolean includeMarker,
                                            final boolean ignoreExceptions,
                                            long batchInterval,
                                            long batchCount,
                                            long batchSize,
                                            long retriesOnError,
                                            String sendMode,
                                            String middleware,
                                            final String disableCertificateValidation,
                                            final String eventBodySerializer,
                                            final String eventHeaderSerializer,
                                            HttpEventCollectorSender.TimeoutSettings timeoutSettings)
    {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MetadataTags.HOST, host != null ? host : "");
        metadata.put(MetadataTags.INDEX, index != null ? index : "");
        metadata.put(MetadataTags.SOURCE, source != null ? source : "");
        metadata.put(MetadataTags.SOURCETYPE, sourcetype != null ? sourcetype : "");
        metadata.put(MetadataTags.MESSAGEFORMAT, messageFormat != null ? messageFormat : "");

        this.sender = new HttpEventCollectorSender(url, token, channel, type, batchInterval, batchCount, batchSize, sendMode, metadata, timeoutSettings);

        // plug a user middleware
        if (middleware != null && !middleware.isEmpty()) {
            try {
                this.sender.addMiddleware((HttpEventCollectorMiddleware.HttpSenderMiddleware)(Class.forName(middleware).newInstance()));
            } catch (Exception ignored) {}
        }

        if (eventBodySerializer != null && !eventBodySerializer.isEmpty()) {
            try {
                this.sender.setEventBodySerializer((EventBodySerializer) Class.forName(eventBodySerializer).newInstance());
            } catch (final Exception ignored) {}
        }

        if (eventHeaderSerializer != null && !eventHeaderSerializer.isEmpty()) {
            try {
                this.sender.setEventHeaderSerializer((EventHeaderSerializer) Class.forName(eventHeaderSerializer).newInstance());
            } catch (final Exception ignored) {}
        }

        // plug resend middleware
        if (retriesOnError > 0) {
            this.sender.addMiddleware(new HttpEventCollectorResendMiddleware(retriesOnError));
        }

        if (disableCertificateValidation != null && disableCertificateValidation.equalsIgnoreCase("true")) {
            this.sender.disableCertificateValidation();
        }

        this.includeLoggerName = includeLoggerName;
        this.includeThreadName = includeThreadName;
        this.includeMDC = includeMDC;
        this.includeException = includeException;
        this.includeMarker = includeMarker;
    }

    /**
     * Create a Http Appender.
     * @return The Http Appender.
     */
    @PluginFactory
    public static HttpEventCollectorLog4jAppender createAppender(
            // @formatter:off
            @PluginAttribute("url") final String url,
            @PluginAttribute("token") final String token,
            @PluginAttribute("channel") final String channel,
            @PluginAttribute("type") final String type,
            @PluginAttribute("name") final String name,
            @PluginAttribute("source") final String source,
            @PluginAttribute("sourcetype") final String sourcetype,
            @PluginAttribute("messageFormat") final String messageFormat,
            @PluginAttribute("host") final String host,
            @PluginAttribute("index") final String index,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final String ignoreExceptions,
            @PluginAttribute("batch_size_bytes") final String batchSize,
            @PluginAttribute("batch_size_count") final String batchCount,
            @PluginAttribute("batch_interval") final String batchInterval,
            @PluginAttribute("retries_on_error") final String retriesOnError,
            @PluginAttribute("send_mode") final String sendMode,
            @PluginAttribute("middleware") final String middleware,
            @PluginAttribute("disableCertificateValidation") final String disableCertificateValidation,
            @PluginAttribute("eventBodySerializer") final String eventBodySerializer,
            @PluginAttribute("eventHeaderSerializer") final String eventHeaderSerializer,
            @PluginAttribute("errorCallback") final String errorCallback,
            @PluginAttribute(value = "includeLoggerName", defaultBoolean = true) final boolean includeLoggerName,
            @PluginAttribute(value = "includeThreadName", defaultBoolean = true) final boolean includeThreadName,
            @PluginAttribute(value = "includeMDC", defaultBoolean = true) final boolean includeMDC,
            @PluginAttribute(value = "includeException", defaultBoolean = true) final boolean includeException,
            @PluginAttribute(value = "includeMarker", defaultBoolean = true) final boolean includeMarker,
            @PluginAttribute(value = "connect_timeout", defaultLong = HttpEventCollectorSender.TimeoutSettings.DEFAULT_CONNECT_TIMEOUT) final long connectTimeout,
            @PluginAttribute(value = "call_timeout", defaultLong = HttpEventCollectorSender.TimeoutSettings.DEFAULT_CALL_TIMEOUT) final long callTimeout,
            @PluginAttribute(value = "read_timeout", defaultLong = HttpEventCollectorSender.TimeoutSettings.DEFAULT_READ_TIMEOUT) final long readTimeout,
            @PluginAttribute(value = "write_timeout", defaultLong = HttpEventCollectorSender.TimeoutSettings.DEFAULT_WRITE_TIMEOUT) final long writeTimeout,
            @PluginAttribute(value = "termination_timeout", defaultLong = HttpEventCollectorSender.TimeoutSettings.DEFAULT_TERMINATION_TIMEOUT) final long terminationTimeout,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter
    )
    {
        // The raw endpoint presumes that a single post is a single event.
        // The batch size should be 1 if "type" is raw, and we should error if batch
        // configuration is specified.
        int clampedBatchCountDefault = HttpEventCollectorSender.DefaultBatchCount;

        if ("raw".equalsIgnoreCase(type)) {
            if (batchSize != null || batchCount != null || batchInterval != null) {
                LOGGER.error("batch configuration is not compatible with the raw endpoint");
                return null;
            }
            clampedBatchCountDefault = 1;
        }

        if (name == null)
        {
            LOGGER.error("No name provided for HttpEventCollectorLog4jAppender");
            return null;
        }

        if (url == null)
        {
            LOGGER.error("No Splunk URL provided for HttpEventCollectorLog4jAppender");
            return null;
        }

        if (token == null)
        {
            LOGGER.error("No token provided for HttpEventCollectorLog4jAppender");
            return null;
        }

        if (layout == null)
        {
            layout = PatternLayout.newBuilder()
                    .withPattern("%m")
                    .withCharset(StandardCharsets.UTF_8)
                    .withAlwaysWriteExceptions(true)
                    .withNoConsoleNoAnsi(false)
                    .build();
        }

        if (errorCallback != null) {
            HttpEventCollectorErrorHandler.registerClassName(errorCallback);
        }

        final boolean ignoreExceptionsBool = Boolean.getBoolean(ignoreExceptions);

        return new HttpEventCollectorLog4jAppender(
                name, url, token,  channel, type,
                source, sourcetype, messageFormat, host, index,
                filter, layout,
                includeLoggerName, includeThreadName, includeMDC, includeException, includeMarker,
                ignoreExceptionsBool,
                parseInt(batchInterval, HttpEventCollectorSender.DefaultBatchInterval),
                parseInt(batchCount, clampedBatchCountDefault),
                parseInt(batchSize, HttpEventCollectorSender.DefaultBatchSize),
                parseInt(retriesOnError, 0),
                sendMode,
                middleware,
                disableCertificateValidation,
                eventBodySerializer,
                eventHeaderSerializer,
                new HttpEventCollectorSender.TimeoutSettings(connectTimeout, callTimeout, readTimeout, writeTimeout, terminationTimeout)
        );
    }


    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event) {

        String exceptionDetail = generateErrorDetail(event);
        // This flag will be true when some other layout is used instead of PatternLayout.
        boolean enableLayoutSerializer = !getLayout().getClass().equals(PatternLayout.class);

        // if an exception was thrown
        this.sender.send(
                event.getTimeMillis(),
                event.getLevel().toString(),
                getLayout().toSerializable(event).toString(),
                includeLoggerName ? event.getLoggerName() : null,
                includeThreadName ? event.getThreadName() : null,
                includeMDC ? event.getContextData().toMap() : null,
                includeException ? exceptionDetail : null,
                includeMarker ? event.getMarker() : null,
                enableLayoutSerializer
        );

    }

    /**
     * Method used to generate proper exception message if any exception encountered.
     *
     * @param event
     * @return the processed string of all exception detail
     */
    private String generateErrorDetail(final LogEvent event) {

        String exceptionDetail = null;

        /*
        Exception details are only populated when any ERROR OR FATAL event occurred
         */
        if (Level.ERROR.equals(event.getLevel()) || Level.FATAL.equals(event.getLevel())) {
            if (event.getThrown() == null && (((MutableLogEvent) event).getParameters()).length <= 0) {
                return exceptionDetail;
            }
            // Exception thrown in application is wrapped with relevant information instead of just a message.
            Map<String, String> exceptionDetailMap = new LinkedHashMap<>();
            if (event.getThrown() != null) {
                Throwable throwable = event.getThrown();
                exceptionDetailMap.put("detailMessage", throwable.getMessage());
                exceptionDetailMap.put("exceptionClass", throwable.getClass().toString());

            } else if ((((MutableLogEvent) event).getParameters()).length > 0) {
                exceptionDetailMap.put("exceptionClass", Arrays.toString(Arrays.stream(((MutableLogEvent) event).getParameters()).toArray()));
            }

            exceptionDetailMap.put("fileName", event.getSource().getFileName());
            exceptionDetailMap.put("methodName", event.getSource().getMethodName());
            exceptionDetailMap.put("lineNumber", String.valueOf(event.getSource().getLineNumber()));
            exceptionDetail = new Gson().toJson(exceptionDetailMap);
        }
        return exceptionDetail;
    }

    public void flush() {
        sender.flush();
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        this.sender.close();
        return super.stop(timeout, timeUnit);
    }
}
