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
 * Container for Splunk http event collector event data
 */
public class HttpEventCollectorEventInfo {
    private LoggerEvent event;
    //metadata
    private MetaData metaData;
    private double time; // time in "epoch" format

    /**
     * Create a new HttpEventCollectorEventInfo container
     *
     * @param severity of event
     * @param message  is an event content
     */
    public HttpEventCollectorEventInfo(final String severity, final String message) {
        this(new MetaData(null, null, null, null), new LoggerEvent(severity, message, null));
    }

    /**
     * @param metaData which contains
     * @param event
     */
    public HttpEventCollectorEventInfo(MetaData metaData,
                                       LoggerEvent event) {
        this.time = System.currentTimeMillis() / 1000.0;
        this.metaData = metaData;
        this.event = event;
    }

    /**
     * @return event timestamp in epoch format
     */
    public double getTime() {
        return time;
    }

    /**
     * @return event severity
     */
    public final String getSeverity() {
        return event.getSeverity();
    }

    /**
     * @return event message
     */
    public final String getMessage() {
        return event.getMessage();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append("time=").append(time).append(" severity=").append(event.severity).append(" message=")
                .append(event.message).append(" data=").append(event.data).toString();
    }

    public long getSize() {
        long messageLength = getMessage() == null ? 0 : getMessage().length();
        long dataLength = event.data == null ? 0 : event.data.toString().length();
        long logLevelLength = getSeverity() == null ? 0 : getSeverity().length();
        return logLevelLength + messageLength + dataLength;
    }

    public LoggerEvent getEvent() {
        return event;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * Meta information for sending to Splunk
     */
    public static class MetaData {
        private String index;
        private String source;
        private String sourcetype;
        private String host;

        public MetaData(String index, String source, String sourcetype, String host) {
            this.index = emptyToNull(index);
            this.source = emptyToNull(source);
            this.sourcetype = emptyToNull(sourcetype);
            this.host = emptyToNull(host);
        }
    }

    /**
     * Message and Auxiliary event data
     */
    public static class LoggerEvent {
        private String severity;
        private String message;
        private Object data;

        public LoggerEvent(String message) {
            this("INFO", message, null);
        }

        public LoggerEvent(String severity, String message) {
            this(severity, message, null);
        }

        public LoggerEvent(String severity, String message, Object data) {
            this.severity = severity;
            this.message = message;
            this.data = data;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    public static String emptyToNull(String string) {
        return (string == "") ? null : string;
    }
}