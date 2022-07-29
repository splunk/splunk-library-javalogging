package com.splunk.logging;

import com.splunk.logging.util.StandardErrorCallback;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpEventCollectorErrorHandlerTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // reset error handler for each test
        HttpEventCollectorErrorHandler.onError(null);
    }

    public void testRegisterClassName() {
        // should not throw exceptions
        HttpEventCollectorErrorHandler.registerClassName(null);
        HttpEventCollectorErrorHandler.registerClassName("");
        HttpEventCollectorErrorHandler.registerClassName("NonExistentClass");
        HttpEventCollectorErrorHandler.registerClassName("com.splunk.logging.util.StandardErrorCallback");
        HttpEventCollectorErrorHandler.registerClassName("com.splunk.logging.util.StandardErrorCallback");
    }

    public void testOnErrorDoubleRegisterAndCounters() {
        StandardErrorCallback standardErrorCallback = new StandardErrorCallback();
        // double register should be ok
        HttpEventCollectorErrorHandler.onError(standardErrorCallback);
        HttpEventCollectorErrorHandler.onError(standardErrorCallback);

        HttpEventCollectorErrorHandler.error(createHttpEventCollectorEventInfos(), new RuntimeException("test"));

        assertEquals(1, standardErrorCallback.getErrorCount());
        assertEquals(2, standardErrorCallback.getEventCount());
    }

    public void testOnErrorCounters() {
        StandardErrorCallback standardErrorCallback = new StandardErrorCallback();
        HttpEventCollectorErrorHandler.onError(standardErrorCallback);

        HttpEventCollectorErrorHandler.error(createHttpEventCollectorEventInfos(), new RuntimeException("test"));

        assertEquals(1, standardErrorCallback.getErrorCount());
        assertEquals(2, standardErrorCallback.getEventCount());

        standardErrorCallback.resetCounters();

        assertEquals(0, standardErrorCallback.getErrorCount());
        assertEquals(0, standardErrorCallback.getEventCount());
    }

    public void testError() {
        HttpEventCollectorErrorHandler.error(null, null);

        HttpEventCollectorErrorHandler.onError((data, exception) -> {
            assertNull(data);
            assertNull(exception);
        });

        HttpEventCollectorErrorHandler.error(null, null);

        HttpEventCollectorErrorHandler.onError((data, exception) -> {
            assertTrue(exception.getMessage().contains("test exception"));
            assertEquals(2, data.size());
            assertEquals("FATAL", data.get(0).getSeverity());
        });

        List<HttpEventCollectorEventInfo> data = createHttpEventCollectorEventInfos();

        HttpEventCollectorErrorHandler.error(data, new IllegalArgumentException("test exception"));

        HttpEventCollectorErrorHandler.error(data, new HttpEventCollectorErrorHandler.ServerErrorException("{ 'text':'test exception', 'code':4}"));

    }

    @NotNull
    private List<HttpEventCollectorEventInfo> createHttpEventCollectorEventInfos() {
        List<HttpEventCollectorEventInfo> data = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("name", "value");
        data.add(new HttpEventCollectorEventInfo(0, "FATAL", "message", "logger-name", "thread-name", map, "exception-message", "marker"));
        data.add(new HttpEventCollectorEventInfo(1, "INFO", "message", "logger-name", "thread-name", map, "exception-message", "marker"));
        return data;
    }
}