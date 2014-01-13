import com.splunk.logging.SplunkCimLogEvent;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by fross on 1/13/14.
 */
public class SplunkCimLogEventUnitTest {
    // Testing: char, byte, short, int, long, float, double, boolean, String


    @Test
    public void addFieldWithCharValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", '\u4126');

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=\u4126\"", event.toString());
    }

    @Test
    public void addFieldWithByteValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", (byte)125);

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=125\"", event.toString());
    }

    @Test
    public void addFieldWithShortValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", (short)129);

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=129\"", event.toString());
    }

    @Test
    public void addFieldWithIntValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", (int)129);

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=129\"", event.toString());
    }

    @Test
    public void addFieldWithLongValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", (long)129L);

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=129\"", event.toString());
    }

    @Test
    public void addFieldWithFloatValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", (float)129.32);

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=129.32\"", event.toString());
    }

    @Test
    public void addFieldWithDoubleValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", (double)129.32);

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=129.32\"", event.toString());
    }

    @Test
    public void addFieldWithBooleanValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", true);

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=true\"", event.toString());
    }

    @Test
    public void addFieldWithStringValue() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", "some \u4406\u4261");

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=some \u4406\u4261\"", event.toString());
    }

    @Test
    public void addFieldWithObjectValue() {
        final String valueString = "Hello world \u4406!";
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", new Object() {
            public String toString() {
                return valueString;
            }
        });

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=" + valueString + "\"", event.toString());
    }





}
