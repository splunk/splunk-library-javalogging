/*
 Copyright © 2019 Splunk Inc.
 SPLUNK CONFIDENTIAL – Use or disclosure of this material in whole or in part
 without a valid written license from Splunk Inc. is PROHIBITED.
 */
package com.splunk.logging.serialization;

import com.google.gson.*;
import com.splunk.logging.EventBodySerializer;
import com.splunk.logging.HttpEventCollectorEventInfo;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventInfoTypeAdapter implements JsonSerializer<HttpEventCollectorEventInfo> {

    @Override
    public JsonElement serialize(HttpEventCollectorEventInfo src, Type typeOfSrc, JsonSerializationContext context) {
        Map<String, Object> event = new HashMap<>();
        // TODO: JsonParser constructor is deprecated in favor of static methods in gson 1.8.6,
        // but Spring Boot does some Gradle magic that downgrades (as of 11/2019) to 1.8.5. This
        // should move to static methods once 1.8.6 has widespread adoption.
        JsonParser parser = new JsonParser();
        if (src.getTime() > 0) {
            event.put("time", String.format(Locale.US, "%.3f", src.getTime()));
        }
        if (src.getSeverity() != null) {
            event.put("severity", src.getSeverity());
        }

        // Always put a message, even if it's empty.
        try {
            // TODO: Move to JsonParser.parseString (see note above)
            event.put("message", parser.parse(src.getMessage()));
        } catch (JsonSyntaxException e) {
            event.put("message", src.getMessage());
        }

        if (src.getLoggerName() != null && !src.getLoggerName().isEmpty()) {
            event.put("logger", src.getLoggerName());
        }

        if (src.getThreadName() != null && !src.getThreadName().isEmpty()) {
            event.put("thread", src.getThreadName());
        }

        if (src.getExceptionMessage() != null && ! src.getExceptionMessage().isEmpty()) {
            event.put("exception", src.getExceptionMessage());
        }

        Map<String, String> props = src.getProperties();
        if (props != null && props.size() > 0) {
            event.put("properties", props);
        }

        if (src.getMarker() != null) {
            String markerString = src.getMarker().toString();
            if (!markerString.isEmpty()) {
                event.put("marker", src.getMarker().toString());
            }
        }

        return context.serialize(event);
    }
}
