package com.splunk.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static com.splunk.logging.HttpEventCollectorEventInfo.MetaData;
import static com.splunk.logging.HttpEventCollectorEventInfo.LoggerEvent;

public class HttpEventCollectorEventInfoSerializerTest {
    private Gson gson = new GsonBuilder().registerTypeAdapter(HttpEventCollectorEventInfo.class,
            new HttpEventCollectorEventInfoSerializer()).create();

    @Test
    public void testToJsonString() {
        //test NULL metadata and aux data
        HttpEventCollectorEventInfo ping = new HttpEventCollectorEventInfo("DEBUG", "ping");
        String pingString = gson.toJson(ping);
        //should not contain NULL value such as "host":blah,"source":blah, "data":blah
        Assert.assertThat(pingString, not(containsString("\"host\"")));
        Assert.assertThat(pingString, not(containsString("\"source\"")));
        Assert.assertThat(pingString, not(containsString("\"data\"")));
        //should contains "event":{"severity":"DEBUG","message":"ping"}

        MetaData metaData=new MetaData("main001","junit","_json","");
        Map auxData=new HashMap();
        auxData.put("build_url","/jenkins/job/test01");
        auxData.put("line_number","1");
        LoggerEvent eventData=new LoggerEvent("DEBUG","hello",auxData);
        HttpEventCollectorEventInfo payloadWithAuxData=new HttpEventCollectorEventInfo(metaData,eventData);
        /* the http event info is
        {"index":"main001","source":"junit","sourcetype":"_json","time":"epoch",
         "event":{"severity":"DEBUG","message":"hello",
                 "data":{"line_number":"1","build_url":"/jenkins/job/test01"}}
               }
         */
        String payload1=gson.toJson(payloadWithAuxData);
        Assert.assertThat(payload1, not(containsString("\"host\"")));
        Assert.assertThat(payload1, containsString("\"index\":\"main001\""));
        Assert.assertThat(payload1, containsString("\"source\":\"junit\""));
        Assert.assertThat(payload1, containsString("\"sourcetype\":\"_json\""));
        Assert.assertThat(payload1, containsString("\"data\":{"));

    }
}
