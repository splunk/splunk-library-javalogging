package com.splunk.logging;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpEventCollectorResendMiddlewareTest extends TestCase {

  public void testPostEvents_whenSuccesShouldNotRetry() {
    // Arrange
    HttpEventCollectorResendMiddleware middleware = new HttpEventCollectorResendMiddleware(3);
    final AtomicInteger callCount = new AtomicInteger(0);
    HttpEventCollectorMiddleware.IHttpSender sender = getSender(callCount, 0);
    final List<Integer> recordedStatusCodes = new ArrayList<>();
    final List<String> recordedReplies = new ArrayList<>();
    final List<Exception> recordedExceptions = new ArrayList<>();
    HttpEventCollectorMiddleware.IHttpSenderCallback callback = getCallback(recordedStatusCodes, recordedReplies, recordedExceptions);

    // Act
    middleware.postEvents(null, sender, callback);


    // Assert
    assertEquals(1, callCount.get());
    assertEquals(1, recordedStatusCodes.size());
    assertEquals(1, recordedReplies.size());
    assertEquals(0, recordedExceptions.size());
    assertEquals(200, recordedStatusCodes.get(0).intValue());
    assertEquals("Success", recordedReplies.get(0));
  }

  public void testPostEvents_whenUnavailableThenSuccessShouldRetry() {
    // Arrange
    HttpEventCollectorResendMiddleware middleware = new HttpEventCollectorResendMiddleware(3);
    final AtomicInteger callCount = new AtomicInteger(0);
    HttpEventCollectorMiddleware.IHttpSender sender = getSender(callCount, 2);
    final List<Integer> recordedStatusCodes = new ArrayList<>();
    final List<String> recordedReplies = new ArrayList<>();
    final List<Exception> recordedExceptions = new ArrayList<>();
    HttpEventCollectorMiddleware.IHttpSenderCallback callback = getCallback(recordedStatusCodes, recordedReplies, recordedExceptions);

    // Act
    middleware.postEvents(null, sender, callback);


    // Assert
    assertEquals(3, callCount.get());
    assertEquals(1, recordedStatusCodes.size());
    assertEquals(1, recordedReplies.size());
    assertEquals(0, recordedExceptions.size());
    assertEquals(200, recordedStatusCodes.get(0).intValue());
  }

  public void testPostEvents_whenUnavailableShouldRetryThenStop() {
    // Arrange
    HttpEventCollectorResendMiddleware middleware = new HttpEventCollectorResendMiddleware(3);
    final AtomicInteger callCount = new AtomicInteger(0);
    HttpEventCollectorMiddleware.IHttpSender sender = getSender(callCount, 10);
    final List<Integer> recordedStatusCodes = new ArrayList<>();
    final List<String> recordedReplies = new ArrayList<>();
    final List<Exception> recordedExceptions = new ArrayList<>();
    HttpEventCollectorMiddleware.IHttpSenderCallback callback = getCallback(recordedStatusCodes, recordedReplies, recordedExceptions);

    // Act
    middleware.postEvents(null, sender, callback);


    // Assert
    assertEquals(4, callCount.get());
    assertEquals(1, recordedStatusCodes.size());
    assertEquals(1, recordedReplies.size());
    assertEquals(0, recordedExceptions.size());
    assertEquals(503, recordedStatusCodes.get(0).intValue());
  }

  private static HttpEventCollectorMiddleware.IHttpSender getSender(AtomicInteger callCount, int errorCount) {
    return new HttpEventCollectorMiddleware.IHttpSender() {
      @Override
      public void postEvents(List<HttpEventCollectorEventInfo> events, HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        callCount.incrementAndGet();
        if (callCount.get() > errorCount) {
          callback.completed(200, "Success");
        } else {
          callback.completed(503, "Service Unavailable");
        }
      }
    };
  }

  private static HttpEventCollectorMiddleware.IHttpSenderCallback getCallback(List<Integer> recordedStatusCodes, List<String> recordedReplies, List<Exception> recordedExceptions) {
    return new HttpEventCollectorMiddleware.IHttpSenderCallback() {

      @Override
      public void completed(int statusCode, String reply) {
        recordedStatusCodes.add(statusCode);
        recordedReplies.add(reply);
      }

      @Override
      public void failed(Exception ex) {
        recordedExceptions.add(ex);
      }
    };
  }
}