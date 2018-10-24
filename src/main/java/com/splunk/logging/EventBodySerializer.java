package com.splunk.logging;

import java.io.Serializable;
import java.util.Map;
import org.json.simple.JSONObject;

public interface EventBodySerializer {

    String serializeEventBody(
        HttpEventCollectorEventInfo eventInfo,
        Object formattedMessage
    );

    JSONObject serializeEventBodyAsJson(
            HttpEventCollectorEventInfo eventInfo,
            Object formattedMessage
    );

    class Default implements EventBodySerializer {

        @Override
        public JSONObject serializeEventBodyAsJson(
                final HttpEventCollectorEventInfo eventInfo,
                final Object formattedMessage
        ) {
            final JSONObject body = new JSONObject();
            putIfPresent(body, "severity", eventInfo.getSeverity());
            putIfPresent(body, "message", formattedMessage);
            putIfPresent(body, "logger", eventInfo.getLoggerName());
            putIfPresent(body, "thread", eventInfo.getThreadName());
            // add an exception record if and only if there is one
            // in practice, the message also has the exception information attached
            if (eventInfo.getExceptionMessage() != null) {
                putIfPresent(body, "exception", eventInfo.getExceptionMessage());
            }

            // add properties if and only if there are any
            final Map<String,String> props = eventInfo.getProperties();
            if (props != null && !props.isEmpty()) {
                body.put("properties", props);
            }
            // add marker if and only if there is one
            final Serializable marker = eventInfo.getMarker();
            if (marker != null) {
                putIfPresent(body, "marker", marker.toString());
            }

            return body;
        }

        @Override
        public String serializeEventBody(
            final HttpEventCollectorEventInfo eventInfo,
            final Object formattedMessage
        ) {

            return this.serializeEventBodyAsJson(eventInfo,formattedMessage).toString();
        }

        private void putIfPresent(final JSONObject obj, String tag, Object value) {
            if (value != null && value instanceof String && ((String) value).isEmpty()) {
                return;
            }
            obj.put(tag, value);
        }
    }
}
