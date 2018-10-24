package com.splunk.logging;

import java.io.Serializable;
import java.util.Map;
import org.json.simple.JSONObject;

public interface EventBodySerializer {

    String serializeEventBody(
            HttpEventCollectorEventInfo eventInfo,
            Object formattedMessage
    );

    class Default implements EventBodySerializer {

        @Override
        public String serializeEventBody(
                final HttpEventCollectorEventInfo eventInfo,
                final Object formattedMessage
        ) {
            final JSONObject body = new JSONObject();
            HttpEventCollectorSender.putIfPresent(body, "severity", eventInfo.getSeverity());
            HttpEventCollectorSender.putIfPresent(body, "message", formattedMessage);
            HttpEventCollectorSender.putIfPresent(body, "logger", eventInfo.getLoggerName());
            HttpEventCollectorSender.putIfPresent(body, "thread", eventInfo.getThreadName());
            // add an exception record if and only if there is one
            // in practice, the message also has the exception information attached
            if (eventInfo.getExceptionMessage() != null) {
                HttpEventCollectorSender.putIfPresent(body, "exception", eventInfo.getExceptionMessage());
            }

            // add properties if and only if there are any
            final Map<String, String> props = eventInfo.getProperties();
            if (props != null && !props.isEmpty()) {
                body.put("properties", props);
            }
            // add marker if and only if there is one
            final Serializable marker = eventInfo.getMarker();
            if (marker != null) {
                HttpEventCollectorSender.putIfPresent(body, "marker", marker.toString());
            }

            return body.toString();
        }
    }
}
