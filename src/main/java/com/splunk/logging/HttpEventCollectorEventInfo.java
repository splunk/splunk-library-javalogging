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

import static com.splunk.logging.HttpEventCollectorSender.MetadataHostTag;
import static com.splunk.logging.HttpEventCollectorSender.MetadataIndexTag;
import static com.splunk.logging.HttpEventCollectorSender.MetadataSourceTag;
import static com.splunk.logging.HttpEventCollectorSender.MetadataSourceTypeTag;
import static com.splunk.logging.HttpEventCollectorSender.MetadataTimeTag;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;

/**
 * Container for Splunk http event collector event data
 */
public class HttpEventCollectorEventInfo {
    private double time; // time in fractional seconds since "unix epoch" format
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


  public String toString(Map<String, String> metadata) {
      // create event json content
    //
    // cf: http://dev.splunk.com/view/event-collector/SP-CAAAE6P
    //
    JSONObject event = new JSONObject();
    // event timestamp and metadata
    putIfPresent(event, MetadataTimeTag, String.format(Locale.US, "%.3f",
            getTime()));
    putIfPresent(event, MetadataHostTag, metadata.get(MetadataHostTag));
    putIfPresent(event, MetadataIndexTag, metadata.get(MetadataIndexTag));
    putIfPresent(event, MetadataSourceTag, metadata.get(MetadataSourceTag));
    putIfPresent(event, MetadataSourceTypeTag, metadata.get(
            MetadataSourceTypeTag));
    // event body
    JSONObject body = new JSONObject();
    putIfPresent(body, "severity", getSeverity());
    putIfPresent(body, "message", getMessage());
    putIfPresent(body, "logger", getLoggerName());
    putIfPresent(body, "thread", getThreadName());
    // add an exception record if and only if there is one
    // in practice, the message also has the exception information attached
    if (getExceptionMessage() != null) {
      putIfPresent(body, "exception", getExceptionMessage());
    }

    // add properties if and only if there are any
    final Map<String, String> props = getProperties();
    if (props != null && !props.isEmpty()) {
      body.put("properties", props);
    }
    // add marker if and only if there is one
    final Serializable marker = getMarker();
    if (marker != null) {
      putIfPresent(body, "marker", marker.toString());
    }
    // join event and body
    event.put("event", body);
    return event.toString();  
  }
    @SuppressWarnings("unchecked")
  private static void putIfPresent(JSONObject collection, String tag,
          String value) {
    if (value != null && value.length() > 0) {
      collection.put(tag, value);
    }
  }
}
