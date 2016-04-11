package com.splunk.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Serialize HttpEventCollectorEventInfo like
 * {"index":"main","time":"1459521600.278","source":"debug","event":{"severity":"DEBUG","message":"ping test"}}
 */
public class HttpEventCollectorEventInfoSerializer implements JsonSerializer<HttpEventCollectorEventInfo> {
    @Override
    public JsonElement serialize(HttpEventCollectorEventInfo src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject logJson = (JsonObject) context.serialize(src.getMetaData());
        //add timestamp into log
        logJson.addProperty("time", String.format(Locale.US, "%.3f", src.getTime()));
        JsonElement eventObject = context.serialize(src.getEvent());
        //add event into log
        logJson.add("event", eventObject);
        return logJson;
    }
}
