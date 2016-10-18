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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * log4j 1.2 Splunk Http Appender.
 */
public final class HttpEventCollectorLog4j1Appender extends AppenderSkeleton
{
    private HttpEventCollectorSender sender = null;
    private String url;
    private String token;
    private String source;
    private String sourcetype;
    private String host;
    private String index;
    private Filter filter;
    private boolean ignoreExceptions;
    private long batchInterval;
    private long batchCount;
    private long batchSize;
    private long retriesOnError;
    private String sendMode;
    private String middleware;
    private String disableCertificateValidation;

    @Override
    public void activateOptions() {
        super.activateOptions();
        if (name == null)
        {
            errorHandler.error("No name provided for HttpEventCollectorLog4j1Appender");
        }

        if (url == null)
        {
            errorHandler.error("No Splunk URL provided for HttpEventCollectorLog4j1Appender");
        }

        if (token == null)
        {
            errorHandler.error("No token provided for HttpEventCollectorLog4j1Appender");
        }

        Dictionary<String, String> metadata = new Hashtable<String, String>();
        metadata.put(HttpEventCollectorSender.MetadataHostTag, host != null ? host : "");
        metadata.put(HttpEventCollectorSender.MetadataIndexTag, index != null ? index : "");
        metadata.put(HttpEventCollectorSender.MetadataSourceTag, source != null ? source : "");
        metadata.put(HttpEventCollectorSender.MetadataSourceTypeTag, sourcetype != null ? sourcetype : "");

        this.sender = new HttpEventCollectorSender(url, token, batchInterval, batchCount, batchSize, sendMode, metadata);

        // plug a user middleware
        if (middleware != null && !middleware.isEmpty()) {
            try {
                this.sender.addMiddleware((HttpEventCollectorMiddleware.HttpSenderMiddleware)(Class.forName(middleware).newInstance()));
            } catch (Exception e) {}
        }

        // plug resend middleware
        if (retriesOnError > 0) {
            this.sender.addMiddleware(new HttpEventCollectorResendMiddleware(retriesOnError));
        }

        if (disableCertificateValidation != null && disableCertificateValidation.equalsIgnoreCase("true")) {
            this.sender.disableCertificateValidation();
        }
    }


    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void append(final LoggingEvent event) {
        this.sender.send(
            event.getLevel().toString(),
            event.getRenderedMessage(),
            event.getLoggerName(),
            event.getThreadName(),
            event.getProperties(),
            event.getThrowableInformation() == null || event.getThrowableInformation().getThrowable() == null ?
                null : event.getThrowableInformation().getThrowable().getMessage(),
            null
        );
    }


    @Override
    public void close() {
        this.sender.flush();
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }


    public HttpEventCollectorSender getSender() {
        return sender;
    }


    public void setSender(HttpEventCollectorSender sender) {
        this.sender = sender;
    }


    public String getUrl() {
        return url;
    }


    public void setUrl(String url) {
        this.url = url;
    }


    public String getToken() {
        return token;
    }


    public void setToken(String token) {
        this.token = token;
    }


    public String getSource() {
        return source;
    }


    public void setSource(String source) {
        this.source = source;
    }


    public String getSourcetype() {
        return sourcetype;
    }


    public void setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
    }


    public String getHost() {
        return host;
    }


    public void setHost(String host) {
        this.host = host;
    }


    public String getIndex() {
        return index;
    }


    public void setIndex(String index) {
        this.index = index;
    }


    public Filter getFilter() {
        return filter;
    }


    public void setFilter(Filter filter) {
        this.filter = filter;
    }


    public boolean isIgnoreExceptions() {
        return ignoreExceptions;
    }


    public void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }


    public long getBatchInterval() {
        return batchInterval;
    }


    public void setBatchInterval(long batchInterval) {
        this.batchInterval = batchInterval;
    }


    public long getBatchCount() {
        return batchCount;
    }


    public void setBatchCount(long batchCount) {
        this.batchCount = batchCount;
    }


    public long getBatchSize() {
        return batchSize;
    }


    public void setBatchSize(long batchSize) {
        this.batchSize = batchSize;
    }


    public long getRetriesOnError() {
        return retriesOnError;
    }


    public void setRetriesOnError(long retriesOnError) {
        this.retriesOnError = retriesOnError;
    }


    public String getSendMode() {
        return sendMode;
    }


    public void setSendMode(String sendMode) {
        this.sendMode = sendMode;
    }


    public String getMiddleware() {
        return middleware;
    }


    public void setMiddleware(String middleware) {
        this.middleware = middleware;
    }


    public String getDisableCertificateValidation() {
        return disableCertificateValidation;
    }


    public void setDisableCertificateValidation(String disableCertificateValidation) {
        this.disableCertificateValidation = disableCertificateValidation;
    }
}
