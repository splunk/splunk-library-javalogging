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

import ch.qos.logback.classic.pattern.MarkerConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.splunk.logging.hec.MetadataTags;

import java.util.*;

/**
 * Logback Appender which writes its events to Splunk http event collector rest endpoint.
 */
public class HttpEventCollectorLogbackAppender<E> extends AppenderBase<E> {
    private HttpEventCollectorSender sender = null;
    private Layout<E> _layout;
    private boolean _includeLoggerName = true;
    private boolean _includeThreadName = true;
    private boolean _includeMDC = true;
    private boolean _includeException = true;

    private String _source;
    private String _sourcetype;
    private String _messageFormat;
    private String _host;
    private String _index;
    private String _url;
    private String _token;
    private String _channel;
    private String _type;
    private String _disableCertificateValidation;
    private String _middleware;
    private String _eventBodySerializer;
    private long _batchInterval = 0;
    private long _batchCount = 0;
    private long _batchSize = 0;
    private String _sendMode;
    private long _retriesOnError = 0;

    @Override
    public void start() {
        if (started)
            return;

        // init events sender
        Map<String, String> metadata = new HashMap<>();
        if (_host != null)
            metadata.put(MetadataTags.HOST, _host);

        if (_index != null)
            metadata.put(MetadataTags.INDEX, _index);

        if (_source != null)
            metadata.put(MetadataTags.SOURCE, _source);

        if (_sourcetype != null)
            metadata.put(MetadataTags.SOURCETYPE, _sourcetype);
        
        if (_messageFormat != null)
            metadata.put(MetadataTags.MESSAGEFORMAT, _messageFormat);

        this.sender = new HttpEventCollectorSender(
                _url, _token, _channel, _type, _batchInterval, _batchCount, _batchSize, _sendMode, metadata);

        // plug a user middleware
        if (_middleware != null && !_middleware.isEmpty()) {
            try {
                this.sender.addMiddleware((HttpEventCollectorMiddleware.HttpSenderMiddleware)(Class.forName(_middleware).newInstance()));
            } catch (Exception ignored) {}
        }

        if (_eventBodySerializer != null && !_eventBodySerializer.isEmpty()) {
            try {
                this.sender.setEventBodySerializer((EventBodySerializer) Class.forName(_eventBodySerializer).newInstance());
            } catch (final Exception ignored) {}
        }

        // plug resend middleware
        if (_retriesOnError > 0) {
            this.sender.addMiddleware(new HttpEventCollectorResendMiddleware(_retriesOnError));
        }

        if (_disableCertificateValidation != null && _disableCertificateValidation.equalsIgnoreCase("true")) {
            sender.disableCertificateValidation();
        }

        super.start();
    }

    @Override
    public void stop() {
        if (!started)
            return;
        this.sender.close();
        super.stop();
    }

    @Override
    protected void append(E e) {
        if (e instanceof ILoggingEvent) {
            sendEvent((ILoggingEvent) e);
        } else {
            sendEvent(e);
        }
    }

    private void sendEvent(ILoggingEvent event) {
        event.prepareForDeferredProcessing();
        if (event.hasCallerData()) {
            event.getCallerData();
        }

        MarkerConverter c = new MarkerConverter();
        if (this.started) {
            this.sender.send(
                    event.getLevel().toString(),
                    _layout.doLayout((E) event),
                    _includeLoggerName ? event.getLoggerName() : null,
                    _includeThreadName ? event.getThreadName() : null,
                    _includeMDC ? event.getMDCPropertyMap() : null,
                    (!_includeException || event.getThrowableProxy() == null) ? null : event.getThrowableProxy().getMessage(),
                    c.convert(event)
            );
        }
    }

    // send non ILoggingEvent such as ch.qos.logback.access.spi.IAccessEvent
    private void sendEvent(E e) {
        String message = _layout.doLayout(e);
        if (message == null) {
            throw new IllegalArgumentException(String.format(
                    "The logback layout %s is probably incorrect, " +
                            "and fails to format the message.",
                    _layout.toString()));
        }
        this.sender.send(message);
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

    public void setChannel(String channel) {
        this._channel = channel;
    }

    public String getChannel() {
        return this._channel;
    }

    public void setType(String type) {
        this._type = type;
    }

    public String getType() {
        return this._type;
    }

    public void setLayout(Layout<E> layout) {
        this._layout = layout;
    }

    public Layout<E> getLayout() {
        return this._layout;
    }

    public boolean getIncludeLoggerName() {
        return _includeLoggerName;
    }

    public void setIncludeLoggerName(boolean includeLoggerName) {
        this._includeLoggerName = includeLoggerName;
    }

    public boolean getIncludeThreadName() {
        return _includeThreadName;
    }

    public void setIncludeThreadName(boolean includeThreadName) {
        this._includeThreadName = includeThreadName;
    }

    public boolean getIncludeMDC() {
        return _includeMDC;
    }

    public void setIncludeMDC(boolean includeMDC) {
        this._includeMDC = includeMDC;
    }

    public boolean getIncludeException() {
        return _includeException;
    }

    public void setIncludeException(boolean includeException) {
        this._includeException = includeException;
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
    
    public void setMessageFormat(String messageFormat) {
        this._messageFormat = messageFormat;
    }

    public String getMessageFormat() {
        return this._messageFormat;
    }

    public void setHost(String host) {
        this._host = host;
    }

    public String getHost() {
        return this._host;
    }

    public void setIndex(String index) {
        this._index = index;
    }

    public String getIndex() {
        return this._index;
    }

    public String getEventBodySerializer() {
        return _eventBodySerializer;
    }

    public void setDisableCertificateValidation(String disableCertificateValidation) {
        this._disableCertificateValidation = disableCertificateValidation;
    }

    public void setbatch_size_count(String value) {
        _batchCount = parseLong(value, HttpEventCollectorSender.DefaultBatchCount);
    }

    public void setbatch_size_bytes(String value) {
        _batchSize = parseLong(value, HttpEventCollectorSender.DefaultBatchSize);
    }

    public void setbatch_interval(String value) {
        _batchInterval = parseLong(value, HttpEventCollectorSender.DefaultBatchInterval);
    }

    public void setretries_on_error(String value) {
        _retriesOnError = parseLong(value, 0);
    }

    public void setsend_mode(String value) {
        _sendMode = value;
    }

    public void setmiddleware(String value) {
        _middleware = value;
    }

    public String getDisableCertificateValidation() {
        return _disableCertificateValidation;
    }

    public void setEventBodySerializer(String eventBodySerializer) {
        this._eventBodySerializer = eventBodySerializer;
    }

    private static long parseLong(String string, int defaultValue) {
        try {
            return Long.parseLong(string);
        }
        catch (NumberFormatException e ) {
            return defaultValue;
        }
    }
}

