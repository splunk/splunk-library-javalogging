package com.splunk.logging;
/*
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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Logback Appender which writes its events to Splunk http input rest endpoint.
 */
public class HttpLogbackAppender extends AppenderBase<ILoggingEvent> {
    private HttpInputEventSender _eventSender;
    private Layout<ILoggingEvent> _layout;
    private String _source;
    private String _sourcetype;
    private String _index;
    private String _url;
    private String _token;
    private String _disableCertificateValidation;
    private long _batchInterval = 0;
    private long _batchCount = 1;
    private long _batchSize = 0;

    @Override
    public void start() {
        if (started)
            return;

        // init events sender
        Dictionary<String, String> metadata = new Hashtable<String, String>();

        if (_index != null)
            metadata.put(HttpInputEventSender.MetadataIndexTag, _index);

        if (_source != null)
            metadata.put(HttpInputEventSender.MetadataSourceTag, _source);

        if (_sourcetype != null)
            metadata.put(HttpInputEventSender.MetadataSourceTypeTag, _sourcetype);

        _eventSender = new HttpInputEventSender(
            _url, _token,
            _batchInterval, _batchCount, _batchSize,
            metadata);

        if (_disableCertificateValidation != null && _disableCertificateValidation.equalsIgnoreCase("true"))
            _eventSender.disableCertificateValidation();

        super.start();
    }

    @Override
    public void stop() {
        if (!started)
            return;
        _eventSender.flush();
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        event.prepareForDeferredProcessing();
        event.getCallerData();
        if (event != null && started) {
            _eventSender.send(event.getLevel().toString(), _layout.doLayout(event));
        }
    }

    public void setUrl(String url) {
        this._url = url;
    }

    public String getUrl() {
        return this._url;
    }

    public void setToken(String token) {
        this._token = token;
    }

    public String getToken() {
        return this._token;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this._layout = layout;
    }

    public Layout<ILoggingEvent> getLayout() {
        return this._layout;
    }

    public void setSource(String source) {
        this._source = source;
    }

    public String getSource() {
        return this._source;
    }

    public void setSourcetype(String sourcetype) {
        this._sourcetype = sourcetype;
    }

    public String getSourcetype() {
        return this._sourcetype;
    }

    public void setIndex(String index) {
        this._index = index;
    }

    public String getIndex() {
        return this._index;
    }

    public void setDisableCertificateValidation(String disableCertificateValidation) {
        this._disableCertificateValidation = disableCertificateValidation;
    }

    public void setbatch_size_count(String value) {
        _batchCount = parseLong(value, 1);
    }

    public void setbatch_size_bytes(String value) {
        _batchSize = parseLong(value, 0);
    }

    public void setbatch_interval(String value) {
        _batchInterval = parseLong(value, 0);
    }

    public String getDisableCertificateValidation() {
        return _disableCertificateValidation;
    }

    private static long parseLong(String string, int defaultValue) {
        try {
            return Long.parseUnsignedLong(string);
        }
        catch (NumberFormatException e ) {
            return defaultValue;
        }
    }
}

