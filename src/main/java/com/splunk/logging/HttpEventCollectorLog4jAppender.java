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

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.google.gson.Gson;
import com.splunk.logging.HttpEventCollectorMiddleware.HttpSenderMiddleware;
import com.splunk.logging.hec.MetadataTags;

/**
 * Splunk Http Appender.
 */
@Plugin(name = "SplunkHttp", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("serial")
public final class HttpEventCollectorLog4jAppender extends AbstractAppender
{
    public static final String BODY_SERIALIZER_TYPE   = "eventBodySerializer";
    public static final String HEADER_SERIALIZER_TYPE = "eventHeaderSerializer";
    public static final String MIDDLEWARE_TYPE        = "middleware";

    public static class Builder extends AbstractAppender.Builder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<HttpEventCollectorLog4jAppender> {

        @PluginBuilderAttribute
        private String url;
        @PluginBuilderAttribute
        private String token;
        @PluginBuilderAttribute
        private String channel;
        @PluginBuilderAttribute
        private String type;
        @PluginBuilderAttribute
        private String source;
        @PluginBuilderAttribute
        private String sourcetype;
        @PluginBuilderAttribute
        private String messageFormat;
        @PluginBuilderAttribute
        private String host;
        @PluginBuilderAttribute
        private String index;
        @PluginBuilderAttribute("batch_size_bytes")
        private String batchSize;
        @PluginBuilderAttribute("batch_size_count")
        private String batchCount;
        @PluginBuilderAttribute("batch_interval")
        private String batchInterval;
        @PluginBuilderAttribute("retries_on_error")
        private String retriesOnError;
        @PluginBuilderAttribute("send_mode")
        private String sendMode;
        @PluginElement(MIDDLEWARE_TYPE)
        HttpSenderMiddleware middleware;
        @PluginBuilderAttribute("middleware")
        private String middlewareClassName;
        @PluginBuilderAttribute
        private String disableCertificateValidation;
        @PluginElement(BODY_SERIALIZER_TYPE)
        EventBodySerializer eventBodySerializer;
        @PluginBuilderAttribute("eventBodySerializer")
        private String eventBodySerializerClassName;
        @PluginElement(HEADER_SERIALIZER_TYPE)
        EventHeaderSerializer eventHeaderSerializer;
        @PluginBuilderAttribute("eventHeaderSerializer")
        private String eventHeaderSerializerClassName;
        @PluginBuilderAttribute
        private String errorCallback;
        @PluginBuilderAttribute
        private boolean includeLoggerName = true;
        @PluginBuilderAttribute
        private boolean includeThreadName = true;
        @PluginBuilderAttribute
        private boolean includeMDC = true;
        @PluginBuilderAttribute
        private boolean includeException = true;
        @PluginBuilderAttribute
        private boolean includeMarker = true;
        @PluginBuilderAttribute("connect_timeout")
        private long connectTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_CONNECT_TIMEOUT;
        @PluginBuilderAttribute("call_timeout")
        private long callTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_CALL_TIMEOUT;
        @PluginBuilderAttribute("read_timeout")
        private long readTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_READ_TIMEOUT;
        @PluginBuilderAttribute("write_timeout") private long writeTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_WRITE_TIMEOUT;
        @PluginBuilderAttribute("termination_timeout")
        private long terminationTimeout = HttpEventCollectorSender.TimeoutSettings.DEFAULT_TERMINATION_TIMEOUT;

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setChannel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setSource(String source) {
            this.source = source;
            return this;
        }

        public Builder setSourcetype(String sourcetype) {
            this.sourcetype = sourcetype;
            return this;
        }

        public Builder setMessageFormat(String messageFormat) {
            this.messageFormat = messageFormat;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setIndex(String index) {
            this.index = index;
            return this;
        }

        public Builder setBatchSize(String batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder setBatchCount(String batchCount) {
            this.batchCount = batchCount;
            return this;
        }

        public Builder setBatchInterval(String batchInterval) {
            this.batchInterval = batchInterval;
            return this;
        }

        public Builder setRetriesOnError(String retriesOnError) {
            this.retriesOnError = retriesOnError;
            return this;
        }

        public Builder setSendMode(String sendMode) {
            this.sendMode = sendMode;
            return this;
        }

        public Builder setMiddleware(HttpSenderMiddleware middleware) {
            this.middleware = middleware;
            return this;
        }

        public Builder setMiddlewareClassName(String middlewareClassName) {
            this.middlewareClassName = middlewareClassName;
            return this;
        }

        public Builder setDisableCertificateValidation(String disableCertificateValidation) {
            this.disableCertificateValidation = disableCertificateValidation;
            return this;
        }

        public Builder setEventBodySerializer(EventBodySerializer eventBodySerializer) {
            this.eventBodySerializer = eventBodySerializer;
            return this;
        }

        public Builder setEventBodySerializerClassName(String eventBodySerializerClassName) {
            this.eventBodySerializerClassName = eventBodySerializerClassName;
            return this;
        }

        public Builder setEventHeaderSerializer(EventHeaderSerializer eventHeaderSerializer) {
            this.eventHeaderSerializer = eventHeaderSerializer;
            return this;
        }

        public Builder setEventHeaderSerializerClassName(String eventHeaderSerializerClassName) {
            this.eventHeaderSerializerClassName = eventHeaderSerializerClassName;
            return this;
        }

        public Builder setErrorCallback(String errorCallback) {
            this.errorCallback = errorCallback;
            return this;
        }

        public Builder setIncludeLoggerName(boolean includeLoggerName) {
            this.includeLoggerName = includeLoggerName;
            return this;
        }

        public Builder setIncludeThreadName(boolean includeThreadName) {
            this.includeThreadName = includeThreadName;
            return this;
        }

        public Builder setIncludeMDC(boolean includeMDC) {
            this.includeMDC = includeMDC;
            return this;
        }

        public Builder setIncludeException(boolean includeException) {
            this.includeException = includeException;
            return this;
        }

        public Builder setIncludeMarker(boolean includeMarker) {
            this.includeMarker = includeMarker;
            return this;
        }

        public Builder setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder setCallTimeout(long callTimeout) {
            this.callTimeout = callTimeout;
            return this;
        }

        public Builder setReadTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder setWriteTimeout(long writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Builder setTerminationTimeout(long terminationTimeout) {
            this.terminationTimeout = terminationTimeout;
            return this;
        }

        public Layout<? extends Serializable> getOrCreateLayout() {
            return getOrCreateLayout(StandardCharsets.UTF_8);
        }

        public Layout<? extends Serializable> getOrCreateLayout(final Charset charset) {
            final Layout<? extends Serializable> layout = getLayout();
            if (layout == null) {
                return PatternLayout.newBuilder()
                        .withPattern("%m")
                        .withCharset(charset)
                        .withAlwaysWriteExceptions(true)
                        .withNoConsoleNoAnsi(false)
                        .withConfiguration(getConfiguration())
                        .build();
            }
            return layout;
        }

        public HttpEventCollectorLog4jAppender build() {
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

                if (getName() == null)
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

                // Fallback on instantiating classes
                if (middleware == null && middlewareClassName != null && !middlewareClassName.isEmpty()) {
                    try {
                        middleware = (HttpSenderMiddleware) Class.forName(middlewareClassName).newInstance();
                    } catch (Exception e) {
                        LOGGER.warn("The middleware {} could not be instantiated.", middlewareClassName, e);
                    }
                }

                if (eventBodySerializer == null && eventBodySerializerClassName != null && !eventBodySerializerClassName.isEmpty()) {
                    try {
                        eventBodySerializer = (EventBodySerializer) Class.forName(eventBodySerializerClassName).newInstance();
                    } catch (final Exception e) {
                        LOGGER.warn("The event body serializer {} could not be instantiated.", eventBodySerializerClassName, e);
                    }
                }

                if (eventHeaderSerializer == null && eventHeaderSerializerClassName != null && !eventHeaderSerializerClassName.isEmpty()) {
                    try {
                        eventHeaderSerializer = (EventHeaderSerializer) Class.forName(eventHeaderSerializerClassName).newInstance();
                    } catch (final Exception e) {
                        LOGGER.warn("The event header serializer {} could not be instantiated.", eventHeaderSerializerClassName, e);
                    }
                }

                if (errorCallback != null) {
                    HttpEventCollectorErrorHandler.registerClassName(errorCallback);
                }

                return new HttpEventCollectorLog4jAppender(
                        getName(), url, token,  channel, type,
                        source, sourcetype, messageFormat, host, index,
                        getFilter(), getOrCreateLayout(),
                        includeLoggerName, includeThreadName, includeMDC, includeException, includeMarker,
                        isIgnoreExceptions(),
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
        }
    }

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
                                            final HttpSenderMiddleware middleware,
                                            final String disableCertificateValidation,
                                            final EventBodySerializer eventBodySerializer,
                                            final EventHeaderSerializer eventHeaderSerializer,
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
        if (middleware != null) {
            this.sender.addMiddleware(middleware);
        }

        if (eventBodySerializer != null) {
            this.sender.setEventBodySerializer(eventBodySerializer);
        }

        if (eventHeaderSerializer != null) {
            this.sender.setEventHeaderSerializer(eventHeaderSerializer);
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

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Create a Http Appender.
     * @return The Http Appender.
     */
    @Deprecated
    public static HttpEventCollectorLog4jAppender createAppender(
            // @formatter:off
            final String url,
            final String token,
            final String channel,
            final String type,
            final String name,
            final String source,
            final String sourcetype,
            final String messageFormat,
            final String host,
            final String index,
            final String ignoreExceptions,
            final String batchSize,
            final String batchCount,
            final String batchInterval,
            final String retriesOnError,
            final String sendMode,
            final String middleware,
            final String disableCertificateValidation,
            final String eventBodySerializer,
            final String eventHeaderSerializer,
            final String errorCallback,
            final boolean includeLoggerName,
            final boolean includeThreadName,
            final boolean includeMDC,
            final boolean includeException,
            final boolean includeMarker,
            final long connectTimeout,
            final long callTimeout,
            final long readTimeout,
            final long writeTimeout,
            final long terminationTimeout,
            Layout<? extends Serializable> layout,
            final Filter filter
    )
    {
        return newBuilder().setUrl(url)
                .setToken(token)
                .setChannel(channel)
                .setType(type)
                .setName(name)
                .setSource(source)
                .setSourcetype(sourcetype)
                .setMessageFormat(messageFormat)
                .setHost(host)
                .setIndex(index)
                .setIgnoreExceptions(Boolean.getBoolean(ignoreExceptions))
                .setBatchSize(batchSize)
                .setBatchCount(batchCount)
                .setBatchInterval(batchInterval)
                .setRetriesOnError(retriesOnError)
                .setSendMode(sendMode)
                .setMiddlewareClassName(middleware)
                .setDisableCertificateValidation(disableCertificateValidation)
                .setEventBodySerializerClassName(eventBodySerializer)
                .setEventHeaderSerializerClassName(eventHeaderSerializer)
                .setErrorCallback(errorCallback)
                .setIncludeLoggerName(includeLoggerName)
                .setIncludeThreadName(includeThreadName)
                .setIncludeMDC(includeMDC)
                .setIncludeException(includeException)
                .setIncludeMarker(includeMarker)
                .setConnectTimeout(connectTimeout)
                .setCallTimeout(callTimeout)
                .setReadTimeout(readTimeout)
                .setWriteTimeout(writeTimeout)
                .setTerminationTimeout(terminationTimeout)
                .setLayout(layout)
                .setFilter(filter)
                .build();
    }


    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event) {

        String exceptionDetail = generateErrorDetail(event);

        // if an exception was thrown
        this.sender.send(
                event.getTimeMillis(),
                event.getLevel().toString(),
                getLayout().toSerializable(event).toString(),
                includeLoggerName ? event.getLoggerName() : null,
                includeThreadName ? event.getThreadName() : null,
                includeMDC ? event.getContextData().toMap() : null,
                includeException ? exceptionDetail : null,
                includeMarker ? event.getMarker() : null
        );

    }

    /**
     * Method used to generate proper exception message if any exception encountered.
     *
     * @param event
     * @return the processed string of all exception detail
     */
    private String generateErrorDetail(final LogEvent event) {

        String exceptionDetail = "";

        /*
        Exception details are only populated when any ERROR OR FATAL event occurred
         */
        try {
            // Exception thrown in application is wrapped with relevant information instead of just a message.
            Map<String, String> exceptionDetailMap = new LinkedHashMap<>();

            if (Level.ERROR.equals(event.getLevel()) || Level.FATAL.equals(event.getLevel())) {
                Throwable throwable = event.getThrown();
                if (throwable == null) {
                    return exceptionDetail;
                }

                exceptionDetailMap.put("detailMessage", throwable.getMessage());
                exceptionDetailMap.put("exceptionClass", throwable.getClass().toString());

                StackTraceElement[] elements = throwable.getStackTrace();
                // Retrieving first element from elements array is because the throws exception detail would be available as a first element.
                if (elements != null && elements.length > 0 && elements[0] != null) {
                    exceptionDetailMap.put("fileName", elements[0].getFileName());
                    exceptionDetailMap.put("methodName", elements[0].getMethodName());
                    exceptionDetailMap.put("lineNumber", String.valueOf(elements[0].getLineNumber()));
                }
                exceptionDetail = new Gson().toJson(exceptionDetailMap);
            }
        } catch (Exception e) {
            // No action here
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
