import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.splunk.logging.EventBodySerializer;
import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.serialization.HecJsonSerializer;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class HecJsonSerializerTest {
    private final Gson gson = new GsonBuilder()
        .disableHtmlEscaping()
        .create();

    private final HttpEventCollectorEventInfo event = new HttpEventCollectorEventInfo(1636473405870L, "INFO", "Test Message", "MyLogger", "Thread", Collections.emptyMap(), null, null);

    @Test
    public void itShouldPopulateTheTime() {
        HecJsonSerializer serializer = new HecJsonSerializer(Collections.emptyMap());
        String result = serializer.serialize(event);
        Map<String, Object> map = (Map<String, Object>) gson.fromJson(result, Map.class);
        Assert.assertEquals("1636473405.870", map.get("time"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itShouldNotPopulateTheTimeIfAlreadyPopulated() {
        HecJsonSerializer serializer = new HecJsonSerializer(Collections.singletonMap("time", "1234.567"));
        String result = serializer.serialize(event);
        Map<String, Object> map = (Map<String, Object>) gson.fromJson(result, Map.class);
        Assert.assertEquals("1234.567", map.get("time"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itShouldNotPopulateTheTimeIfHeaderPopulates() {
        HecJsonSerializer serializer = new HecJsonSerializer(Collections.emptyMap());
        serializer.setEventHeaderSerializer((eventInfo, metadata) -> new HashMap<>(Collections.singletonMap("time", "12345.678")));
        String result = serializer.serialize(event);
        Map<String, Object> map = (Map<String, Object>) gson.fromJson(result, Map.class);
        Assert.assertEquals("12345.678", map.get("time"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itShouldNotPopulateTheTimeIfBodyPopulates() {
        HecJsonSerializer serializer = new HecJsonSerializer(Collections.emptyMap());
        serializer.setEventBodySerializer(new EventBodySerializer() {
            @Override
            public String serializeEventBody(final HttpEventCollectorEventInfo eventInfo, final Object formattedMessage) {
                return "XXX";
            }

            @Override
            public double getEventTime(final HttpEventCollectorEventInfo eventInfo) {
                return 1.0d;
            }
        });
        String result = serializer.serialize(event);
        Map<String, Object> map = (Map<String, Object>) gson.fromJson(result, Map.class);
        Assert.assertEquals("1.000", map.get("time"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itShouldPopulateTheTimeIfBodyOverrideDoesntSetTime() {
        HecJsonSerializer serializer = new HecJsonSerializer(Collections.emptyMap());
        serializer.setEventBodySerializer(new EventBodySerializer() {
            @Override
            public String serializeEventBody(final HttpEventCollectorEventInfo eventInfo, final Object formattedMessage) {
                return "XXX";
            }
        });
        String result = serializer.serialize(event);
        Map<String, Object> map = (Map<String, Object>) gson.fromJson(result, Map.class);
        Assert.assertEquals("1636473405.870", map.get("time"));
    }
}
