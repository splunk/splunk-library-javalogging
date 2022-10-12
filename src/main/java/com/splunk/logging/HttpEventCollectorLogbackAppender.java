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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.MarkerConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.google.gson.Gson;
import com.splunk.logging.hec.MetadataTags;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private String _eventHeaderSerializer;
    private String _errorCallback;
    private long _batchInterval = 0;
    private long _batchCount = 0;
    private long _batchSize = 0;
    private String _sendMode;
    private long _retriesOnError = 0;
    private Map<String, String> _metadata = new HashMap<>();
    private boolean _batchingConfigured = false;


    private HttpEventCollectorSender.TimeoutSettings timeoutSettings = new HttpEventCollectorSender.TimeoutSettings();

    @Override
    public void start() {
        if (started)
            return;

        Map<String, String> metadata = new HashMap<>(_metadata);
        // init events sender
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

        // This should have been caught at configuration time, but double-check at start
        if ("raw".equalsIgnoreCase(_type) && _batchingConfigured) {
            throw new IllegalArgumentException("Batching configuration and sending type of raw are incompatible.");
        }

        this.sender = new HttpEventCollectorSender(
                _url, _token, _channel, _type, _batchInterval, _batchCount, _batchSize, _sendMode, metadata, timeoutSettings);

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

        if (_eventHeaderSerializer != null && !_eventHeaderSerializer.isEmpty()) {
            try {
                this.sender.setEventHeaderSerializer((EventHeaderSerializer) Class.forName(_eventHeaderSerializer).newInstance());
            } catch (final Exception ignored) {}
        }

        if (_errorCallback != null && !_errorCallback.isEmpty()) {
            HttpEventCollectorErrorHandler.registerClassName(_errorCallback);
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

    public void flush() {
        if (started) {
            sender.flush();
        }
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

        boolean isExceptionOccured = false;
        String exceptionDetail = null;

        /*
        Exception details are only populated when any ERROR encountered & exception is actually thrown
         */
        try {
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (Level.ERROR.isGreaterOrEqual(event.getLevel()) && throwableProxy != null) {
                // Exception thrown in application is wrapped with relevant information instead of just a message.
                Map<Object, Object> exceptionDetailMap = new LinkedHashMap<>();

                exceptionDetailMap.put("detailMessage", throwableProxy.getMessage());
                exceptionDetailMap.put("exceptionClass", throwableProxy.getClassName());

                // Retrieving first element from elements array is because the throws exception detail would be available as a first element.
                StackTraceElementProxy[] elements = throwableProxy.getStackTraceElementProxyArray();
                if (elements != null && elements.length > 0 && elements[0] != null) {
                    exceptionDetailMap.put("fileName", elements[0].getStackTraceElement().getFileName());
                    exceptionDetailMap.put("methodName", elements[0].getStackTraceElement().getMethodName());
                    exceptionDetailMap.put("lineNumber", String.valueOf(elements[0].getStackTraceElement().getLineNumber()));
                }

                exceptionDetail = new Gson().toJson(exceptionDetailMap);
                isExceptionOccured = true;
            }
        } catch (Exception e) {
            // No actions here
        }

        MarkerConverter c = new MarkerConverter();
        if (this.started) {
            this.sender.send(
            		event.getTimeStamp(),
                    event.getLevel().toString(),
                    _layout.doLayout((E) event),
                    _includeLoggerName ? event.getLoggerName() : null,
                    _includeThreadName ? event.getThreadName() : null,
                    _includeMDC ? event.getMDCPropertyMap() : null,
                    (_includeException && isExceptionOccured) ? exceptionDetail : null,
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
        if ("raw".equalsIgnoreCase(type)) {
            validateNotBatchedAndRaw();
            this._batchCount = 1; // Enforce sending on every event
        }
    }

    private void validateNotBatchedAndRaw() {
        if ("raw".equalsIgnoreCase(_type)) {
            if (_batchingConfigured) {
                throw new IllegalArgumentException("Batching configuration and sending type of raw are incompatible.");
            }
        }
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

    public void addMetadata(String tag, String value){
        this._metadata.put(tag,value);
    }

    public String getEventBodySerializer() {
        return _eventBodySerializer;
    }

    public String getEventHeaderSerializer() {
        return _eventHeaderSerializer;
    }

    public String getErrorHandler(String errorHandlerClass) {
        return this._errorCallback;
    }

    public void setDisableCertificateValidation(String disableCertificateValidation) {
        this._disableCertificateValidation = disableCertificateValidation;
    }

    public void setbatch_size_count(String value) {
        _batchCount = parseLong(value, HttpEventCollectorSender.DefaultBatchCount);
        _batchingConfigured = true;
        validateNotBatchedAndRaw();
    }

    public void setbatch_size_bytes(String value) {
        _batchSize = parseLong(value, HttpEventCollectorSender.DefaultBatchSize);
        _batchingConfigured = true;
        validateNotBatchedAndRaw();
    }

    public void setbatch_interval(String value) {
        _batchInterval = parseLong(value, HttpEventCollectorSender.DefaultBatchInterval);
        _batchingConfigured = true;
        validateNotBatchedAndRaw();
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

    public void setEventHeaderSerializer(String eventHeaderSerializer) {
        this._eventHeaderSerializer = eventHeaderSerializer;
    }

    public void setErrorCallback(String errorHandlerClass) {
        this._errorCallback = errorHandlerClass;
    }

    public String getErrorCallback() {
        return this._errorCallback;
    }

    public void setConnectTimeout(long milliseconds) {
        this.timeoutSettings.connectTimeout = milliseconds;
    }

    public long getConnectTimeout(long milliseconds) {
        return this.timeoutSettings.connectTimeout = milliseconds;
    }

    public void setCallTimeout(long milliseconds) {
        this.timeoutSettings.callTimeout = milliseconds;
    }

    public long getCallTimeout(long milliseconds) {
        return this.timeoutSettings.callTimeout = milliseconds;
    }


    public void setReadTimeout(long milliseconds) {
        this.timeoutSettings.readTimeout = milliseconds;
    }

    public long getReadTimeout(long milliseconds) {
        return this.timeoutSettings.readTimeout = milliseconds;
    }

    public void setWriteTimeout(long milliseconds) {
        this.timeoutSettings.writeTimeout = milliseconds;
    }

    public long getWriteTimeout(long milliseconds) {
        return this.timeoutSettings.writeTimeout = milliseconds;
    }

    public void setTerminationTimeout(long milliseconds) {
        this.timeoutSettings.terminationTimeout = milliseconds;
    }

    public long getTerminationTimeout(long milliseconds) {
        return this.timeoutSettings.terminationTimeout = milliseconds;
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

