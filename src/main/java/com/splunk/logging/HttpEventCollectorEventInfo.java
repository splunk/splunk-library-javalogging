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

import java.io.Serializable;
import java.util.Map;

/**
 * Container for Splunk http event collector event data
 */
public class HttpEventCollectorEventInfo {
    private final double time; // time in fractional seconds since "unix epoch" format
    private final String severity;
    private final String message;
    private final String logger_name;
    private final String thread_name;
    private final Map<String, String> properties;
    private final String exception_message;
    private final Serializable marker;

    /**
     * Create a new HttpEventCollectorEventInfo container
     * @param severity of event
     * @param message is an event content
     * @param logger_name name of the logger
     * @param thread_name name of the thread
     * @param properties additional properties for this event
     * @param exception_message text of an exception to log
     * @param marker event marker
     */
    public HttpEventCollectorEventInfo(
            final String severity,
            final String message,
            final String logger_name,
            final String thread_name,
            final Map<String, String> properties,
            final String exception_message,
            final Serializable marker
    ) {
        this.time = System.currentTimeMillis() / 1000.0;
        this.severity = severity;
        this.message = message;
        this.logger_name = logger_name;
        this.thread_name = thread_name;
        this.properties = properties;
        this.exception_message = exception_message;
        this.marker = marker;
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
        return severity;
    }

    /**
     * @return event message
     */
    public final String getMessage() {
        return message;
    }

    /**
     * @return event logger name
     */
    public final String getLoggerName() { return logger_name; }

    /**
     * @return event thread name
     */
    public final String getThreadName() { return thread_name; }

    /**
     * @return event MDC properties
     */
    public Map<String,String> getProperties() { return properties; }

    /**
     * @return event's exception message
     */
    public final String getExceptionMessage() { return exception_message; }

    /**
     * @return event marker
     */
    public Serializable getMarker() { return marker; }
}
