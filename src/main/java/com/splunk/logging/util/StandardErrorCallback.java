package com.splunk.logging.util;

import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorEventInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Print errors to standard out because sending events to Splunk has exceptions and
 * logging frameworks might be disabled or broken.
 *
 * Enable printStackTraces via property: <code>-Dcom.splunk.logging.util.StandardErrorCallback.enablePrintStackTrace=true</code>
 */
public class StandardErrorCallback implements HttpEventCollectorErrorHandler.ErrorCallback {

    public static final Locale DEFAULT_LOCALE = Locale.US;

    private static final AtomicInteger eventCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);

    private static final DateTimeFormatter HUMAN_READABLE_WITH_MILLIS =
        new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss.SSS").toFormatter(DEFAULT_LOCALE);

    private final boolean enablePrintStackTrace;

    public StandardErrorCallback() {
        this(checkEnablePrintStackTrace());
    }

    private static boolean checkEnablePrintStackTrace() {
        return "true".equalsIgnoreCase(System.getProperty("com.splunk.logging.util.StandardErrorCallback.enablePrintStackTrace", "false"));
    }

    public StandardErrorCallback(boolean enablePrintStackTrace) {
        this.enablePrintStackTrace = enablePrintStackTrace;
    }

    @Override
    public void error(List<HttpEventCollectorEventInfo> data, Exception ex) {
        int totalErrorCount = errorCount.incrementAndGet();
        int totalEventCount = eventCount.addAndGet(data == null ? 0 : data.size());

        String threadName = Thread.currentThread().getName();

        String fullMessage = createErrorMessage(data, ex, totalErrorCount, totalEventCount, threadName);

        printError(fullMessage);

        printStackTrace(ex);
    }

    private String createErrorMessage(List<HttpEventCollectorEventInfo> data, Exception ex, int totalErrorCount, int totalEventCount, String threadName) {

        String timestamp = HUMAN_READABLE_WITH_MILLIS.format(LocalDateTime.now());
        String exceptionMessage = ex == null ? "unknown (exception null)" : ex.getClass().getSimpleName() + ": " + ex.getMessage();

        final String batchOrSingleText = createBatchOrSingleText(data);

        String fullMessage = timestamp
            + " [" + threadName + "] HttpEventCollectorError exception for "
            + batchOrSingleText + ". Total errors/events: " + totalErrorCount + "/" + totalEventCount + ". Message: " + exceptionMessage;

        return fullMessage;
    }

    private String createBatchOrSingleText(List<HttpEventCollectorEventInfo> data) {
        final String batchOrSingleText;
        if (data == null) {
            batchOrSingleText = "unknown events (data is null)";
        }
        else {
            int size = data.size();
            if (size == 1) {
                batchOrSingleText = "log event";
            } else {
                batchOrSingleText = "batch of " + size + " log events";
            }
        }
        return batchOrSingleText;
    }

    private void printStackTrace(Exception ex) {
        if (enablePrintStackTrace) {
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    }

    private void printError(String fullMessage) {
        System.err.println(fullMessage);
    }

    public int getEventCount() {
        return eventCount.get();
    }

    public int getErrorCount() {
        return errorCount.get();
    }

    public void resetCounters() {
        // should maybe be atomic thread safe action, complicated, good enough?
        errorCount.set(0);
        eventCount.set(0);
    }

    public boolean isPrintStackTraceEnabled() {
        return enablePrintStackTrace;
    }

}
