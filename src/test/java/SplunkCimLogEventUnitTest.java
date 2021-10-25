/*
 * Copyright 2014 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import com.splunk.logging.SplunkCimLogEvent;
import org.junit.Assert;
import org.junit.Test;

/**
 * Check that SplunkCimLogEvent produces what we expect it to.
 */
public class SplunkCimLogEventUnitTest {
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

    @Test
    public void addFieldWithStringValueContainingDoubleQuotes() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");
        event.addField("key", "I contain \" double quotes");

        Assert.assertEquals("\"name=name\" \"event_id=event-id\" \"key=I contain \\\" double quotes\"", event.toString());
    }

    @Test
    public void addThrowableWorks() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");

        try {
           throw new Exception("This is a test of the Java emergency broadcast system.");
        } catch (Exception e) {
            event.addThrowableWithStacktrace(e);
        }

        String expectedString = "\"name=name\" \"event_id=event-id\" " +
                "\"throwable_class=java.lang.Exception\" \"throwable_message=This is a test of the Java " +
                "emergency broadcast system.\" \"stacktrace_elements=SplunkCimLogEventUnitTest." +
                "addThrowableWorks(SplunkCimLogEventUnitTest.java:???),";
        String foundString = event.toString();
        foundString = foundString.replaceAll(":\\d+\\)", ":???)"); // Get rid of line numbers.

        Assert.assertEquals(expectedString, foundString.substring(0, expectedString.length()));

    }

    @Test
    public void addThrowableWorksWithDepth() {
        SplunkCimLogEvent event = new SplunkCimLogEvent("name", "event-id");

        try {
            throw new Exception("This is a test of the Java emergency broadcast system.");
        } catch (Exception e) {
            event.addThrowableWithStacktrace(e, 1);
        }

        String expected = "\"name=name\" \"event_id=event-id\" " +
                "\"throwable_class=java.lang.Exception\" \"throwable_message=This is a test of the Java " +
                "emergency broadcast system.\" \"stacktrace_elements=SplunkCimLogEventUnitTest." +
                "addThrowableWorksWithDepth(SplunkCimLogEventUnitTest.java:???)\"";
        Assert.assertEquals(expected, event.toString().replaceAll(":\\d+\\)", ":???)"));
    }

}
