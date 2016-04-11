package com.splunk.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Dictionary;

public class HttpEventCollectorSender extends AbstractHttpEventCollectorSender {
    public HttpEventCollectorSender(String Url, String token, long delay, long maxEventsBatchCount,
                                    long maxEventsBatchSize, String sendModeStr, Dictionary<String, String> metadata) {
        super(Url, token, delay, maxEventsBatchCount, maxEventsBatchSize, sendModeStr, metadata);
    }

    // Gson escapes <>&='by default, need disable it
    private Gson gson = new GsonBuilder().registerTypeAdapter(HttpEventCollectorEventInfo.class,
            new HttpEventCollectorEventInfoSerializer()).disableHtmlEscaping().create();

    /**
     * Only care about message in HttpEventCollectorEventInfo, data is not used.
     *
     * @param eventInfo
     * @return
     */
    @Override
    public String serializeEventInfo(HttpEventCollectorEventInfo eventInfo) {
        String result = gson.toJson(eventInfo);
        return result;
    }
}
