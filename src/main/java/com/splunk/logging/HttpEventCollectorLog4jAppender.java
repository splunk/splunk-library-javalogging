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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Splunk Http Appender.
 */
@Plugin(name = "Http", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("serial")
public final class HttpEventCollectorLog4jAppender extends AbstractAppender
{
    private HttpEventCollectorSender sender = null;

    private HttpEventCollectorLog4jAppender(final String name,
                         final String url,
                         final String token,
                         final String source,
                         final String sourcetype,
                         final String host,
                         final String index,
                         final Filter filter,
                         final Layout<? extends Serializable> layout,
                         final boolean ignoreExceptions,
                         long batchInterval,
                         long batchCount,
                         long batchSize,
                         long retriesOnError,
                         String sendMode,
                         String middleware,
                         final String disableCertificateValidation,
			     boolean ack,
			     String ackUrl)
    {
        super(name, filter, layout, ignoreExceptions);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(HttpEventCollectorSender.MetadataHostTag, host != null ? host : "");
        metadata.put(HttpEventCollectorSender.MetadataIndexTag, index != null ? index : "");
        metadata.put(HttpEventCollectorSender.MetadataSourceTag, source != null ? source : "");
        metadata.put(HttpEventCollectorSender.MetadataSourceTypeTag, sourcetype != null ? sourcetype : "");

        this.sender = new HttpEventCollectorSender(url, token, batchInterval, batchCount, batchSize, sendMode, ack, ackUrl, metadata);

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
     * Create a Http Appender.
     * @return The Http Appender.
     */
    @PluginFactory
    public static HttpEventCollectorLog4jAppender createAppender(
            // @formatter:off
            @PluginAttribute("url") final String url,
            @PluginAttribute("token") final String token,
            @PluginAttribute("name") final String name,
            @PluginAttribute("source") final String source,
            @PluginAttribute("sourcetype") final String sourcetype,
            @PluginAttribute("host") final String host,
            @PluginAttribute("index") final String index,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginAttribute("batch_size_bytes") final String batchSize,
            @PluginAttribute("batch_size_count") final String batchCount,
            @PluginAttribute("batch_interval") final String batchInterval,
            @PluginAttribute("retries_on_error") final String retriesOnError,
            @PluginAttribute("send_mode") final String sendMode,
            @PluginAttribute("middleware") final String middleware,
            @PluginAttribute("disableCertificateValidation") final String disableCertificateValidation,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute(value="ack", defaultBoolean=false) final boolean ack,
            @PluginAttribute("ackUrl") final String ackUrl			
    )
    {
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
            layout = PatternLayout.createLayout("%m", null, null, Charset.forName("UTF-8"), true, false, null, null);
        }
        
        if (ack && (null==ackUrl || ackUrl.isEmpty())){        
          LOGGER.error("AckUrl must be presetnt when ack=true");
          return null;
        }

        final boolean ignoreExceptions = true;

        return new HttpEventCollectorLog4jAppender(
                name, url, token,
                source, sourcetype, host, index,
                filter, layout, ignoreExceptions,
                parseInt(batchInterval, HttpEventCollectorSender.DefaultBatchInterval),
                parseInt(batchCount, HttpEventCollectorSender.DefaultBatchCount),
                parseInt(batchSize, HttpEventCollectorSender.DefaultBatchSize),
                parseInt(retriesOnError, 0),
                sendMode,
                middleware,
                disableCertificateValidation,
                ack, 
                ackUrl);
    }


    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event)
    {
        // if an exception was thrown
        this.sender.send(
                event.getLevel().toString(),
                event.getMessage().getFormattedMessage(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getContextMap(),
                event.getThrown() == null ? null : event.getThrown().getMessage(),
                event.getMarker()
        );
    }

    @Override
    public void stop() {
        this.sender.flush();
        super.stop();
    }
}
