/*
 Copyright © 2019 Splunk Inc.
 SPLUNK CONFIDENTIAL – Use or disclosure of this material in whole or in part
 without a valid written license from Splunk Inc. is PROHIBITED.
 */
package com.splunk.logging.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.splunk.logging.EventBodySerializer;
import com.splunk.logging.EventHeaderSerializer;
import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.hec.MetadataTags;

import java.util.*;
import java.util.stream.Collectors;

public class HecJsonSerializer {
    private static final Set<String> KEYWORDS = MetadataTags.HEC_TAGS;
    private Map<String, Object> template = new LinkedHashMap<>();
    private EventInfoTypeAdapter typeAdapter = new EventInfoTypeAdapter();
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(HttpEventCollectorEventInfo.class, typeAdapter)
            .disableHtmlEscaping()
            .create();
    private EventBodySerializer eventBodySerializer;
    private EventHeaderSerializer eventHeaderSerializer;

    public HecJsonSerializer(Map<String, String> metadata) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void setValue(String key, String value) {
        if (KEYWORDS.contains(key)) {
            template.put(key, value);
        } else {
            if (!template.containsKey("fields")) {
                template.put("fields", new HashMap<String, String>());
            }
            Object fields = template.get("fields");
            if (fields instanceof Map) {
                ((Map<String, String>) fields).put(key, value);
            }
        }
    }

    public String serialize(HttpEventCollectorEventInfo info) {
        Map<String, Object> event;
        if (this.eventHeaderSerializer != null) {
            event = eventHeaderSerializer.serializeEventHeader(info, new HashMap<>(template));
        } else {
            event = new HashMap<>(template);
            event.put("time", String.format(Locale.US, "%.3f", info.getTime()));
        }
        if (this.eventBodySerializer != null) {
            event.put("event", eventBodySerializer.serializeEventBody(info, info.getMessage()));
            double eventTime = eventBodySerializer.getEventTime(info);
            if (eventTime > 0) {
                event.put("time", String.format(Locale.US, "%.3f", eventTime));
            }
        } else {
            event.put("event", info);
        }
        return gson.toJson(event);
    }

    public void setEventBodySerializer(EventBodySerializer eventBodySerializer) {
        this.eventBodySerializer = eventBodySerializer;
    }

    public void setEventHeaderSerializer(EventHeaderSerializer eventHeaderSerializer) {
        this.eventHeaderSerializer = eventHeaderSerializer;
    }
}
