/*
 Copyright © 2019 Splunk Inc.
 SPLUNK CONFIDENTIAL – Use or disclosure of this material in whole or in part
 without a valid written license from Splunk Inc. is PROHIBITED.
 */
package com.splunk.logging.serialization;

import com.google.gson.*;
import com.splunk.logging.HttpEventCollectorEventInfo;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EventInfoTypeAdapter implements JsonSerializer<HttpEventCollectorEventInfo> {

    @Override
    public JsonElement serialize(HttpEventCollectorEventInfo src, Type typeOfSrc, JsonSerializationContext context) {
        Map<String, Object> event = new HashMap<>();

        /*
        When layout other than Pattern Layout is used then this block will get executed.
         */
        if (src.isLayoutSerializerEnabled()) {
            // Always put a message, even if it's empty.
            try {
                JsonElement parsed = JsonParser.parseString(src.getMessage());
                JsonObject jsonObject = parsed.getAsJsonObject();
                Set<String> keySet = jsonObject.keySet();

                for (String key : keySet) {
                    event.put(key, jsonObject.get(key));
                }

                return context.serialize(event);
            } catch (JsonSyntaxException e) {
                // No action performed here when json parsing fails.
                // Message will be automatically serialized based on default method mentioned below.
            }
        }

        if (src.getSeverity() != null) {
            event.put("severity", src.getSeverity());
        }

        // Always put a message, even if it's empty.
        try {
            JsonElement parsed = JsonParser.parseString(src.getMessage());
            if(parsed instanceof JsonNull && !src.getMessage().isEmpty()) {
                event.put("message", src.getMessage());
            } else {
                event.put("message", parsed);
            }
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
