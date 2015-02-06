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
 * @brief A handler for java.util.logging that works with Splunk http input.
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
 * # Events batching parameters:
 * # Delay in millisecond between sending events, by default this value is 0, i.e., and events
 * # are sending immediately
 * com.splunk.logging.HttpInputHandler.delay
 *
 * # Max number of events in a batch. By default - 0, i.e., no batching
 * com.splunk.logging.HttpInputHandler.batchCount
 *
 * # Max size of events in a batch. By default - 0, i.e., no batching
 * com.splunk.logging.HttpInputHandler.batchSize
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
 *
 * # Batching
 * com.splunk.logging.HttpInputHandler.delay = 500
 * com.splunk.logging.HttpInputHandler.batchCount = 1000
 * com.splunk.logging.HttpInputHandler.batchSize = 65536
 */

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

/**
 * An input handler for Splunk http input logging. This handler can be used by
 * by specifying handlers = com.splunk.logging.HttpInputHandler in java.util.logging
 * properties file.
 */
public final class HttpInputHandler extends Handler {
    private HttpInputEventSender eventSender;

    private final String BatchDelayConfTag = "delay";
    private final String BatchCountConfTag = "batchCount";
    private final String BatchSizeConfTag = "batchSize";

    private final String DefaultScheme = "https";
    private final String DefaultPort = "8089";
    private final String HttpInputUrlPath = "/services/receivers/token";


    /** HttpInputHandler c-or */
    public HttpInputHandler() {
        // read configuration settings
        Dictionary<String, String> metadata = new Hashtable<String, String>();
        metadata.put(HttpInputEventSender.MetadataIndexTag,
            getConfigurationProperty(HttpInputEventSender.MetadataIndexTag, ""));

        metadata.put(HttpInputEventSender.MetadataSourceTag,
            getConfigurationProperty(HttpInputEventSender.MetadataSourceTag, ""));

        metadata.put(HttpInputEventSender.MetadataSourceTypeTag,
            getConfigurationProperty(HttpInputEventSender.MetadataSourceTypeTag, ""));

        // http input endpoint properties
        String scheme = getConfigurationProperty("scheme", DefaultScheme);
        String host = getConfigurationProperty("host", null);
        String port = getConfigurationProperty("port", DefaultPort);
        String httpInputUrl = String.format("%s://%s:%s%s",
            scheme, host, port, HttpInputUrlPath);

        // app token
        String token = getConfigurationProperty("token", null);

        // batching properties
        long delay = getConfigurationNumericProperty(BatchDelayConfTag, 0);
        long batchCount = getConfigurationNumericProperty(BatchCountConfTag, 0);
        long batchSize = getConfigurationNumericProperty(BatchSizeConfTag, 0);

        // delegate all configuration params to event sender
        eventSender = new HttpInputEventSender(
            httpInputUrl, token, delay, batchCount, batchSize, metadata);

        if (getConfigurationProperty("disableCertificateValidation", "false").equalsIgnoreCase("true")) {
            eventSender.disableCertificateValidation();
        }
    }

    /**
     * java.util.logging data handler callback
     * @param record is a logging record
     */
    @Override
    public void publish(LogRecord record) {
        eventSender.send(record.getLevel().toString(), record.getMessage());
    }

    /**
     * java.util.logging data handler callback
     */
    @Override
    public void flush() {
        eventSender.flush();
    }

    /**
     * java.util.logging data handler close callback
     * @throws SecurityException
     */
    @Override public void close() throws SecurityException {
        eventSender.close();
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

    private long getConfigurationNumericProperty(
        final String property, long defaultValue) {
        return Integer.parseInt(
            getConfigurationProperty(property, String.format("%d", defaultValue)));
    }
}
