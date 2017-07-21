package com.splunk.logging;

/**
 * @copyright
 *
 * Copyright 2013-2015 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.Map;

/**
 * This is an internal helper class that sends logging events to Splunk http
 * event collector.
 */
public final class HttpEventCollectorSender implements HttpEventCollectorMiddleware.IHttpSender {

  public static final String MetadataTimeTag = "time";
  public static final String MetadataHostTag = "host";
  public static final String MetadataIndexTag = "index";
  public static final String MetadataSourceTag = "source";
  public static final String MetadataSourceTypeTag = "sourcetype";
  private static final String AuthorizationHeaderTag = "Authorization";
  private static final String AuthorizationHeaderScheme = "Splunk %s";
  private static final String HttpContentType = "application/json; profile=urn:splunk:event:1.0; charset=utf-8";
  private static final String SendModeSequential = "sequential";
  private static final String SendModeSParallel = "parallel";
  private static final String ChannelHeader = "X-Splunk-Request-Channel";
  private long maxEventsBatchCount;
  private long maxEventsBatchSize;
  private final long delay;
  private final Map<String, String> metadata;

  public String getChannel() {
    return channel;
  }

  private static String newChannel() {
    return java.util.UUID.randomUUID().toString();
  }

  /**
   * Sender operation mode. Parallel means that all HTTP requests are
   * asynchronous and may be indexed out of order. Sequential mode guarantees
   * sequential order of the indexed events.
   */
  public enum SendMode {
    Sequential,
    Parallel
  };

  /**
   * Recommended default values for events batching.
   */
  public static final int DefaultBatchInterval = 10 * 1000; // 10 seconds
  public static final int DefaultBatchSize = 10 * 1024; // 10KB
  public static final int DefaultBatchCount = 10; // 10 events
  
  private Timer timer = new Timer();
  private String url;
  private String token;
  private EventBatch eventsBatch;// = new EventBatch();  
  private CloseableHttpAsyncClient httpClient;
  private boolean disableCertificateValidation = false;
  private SendMode sendMode = SendMode.Sequential;
  private HttpEventCollectorMiddleware middleware = new HttpEventCollectorMiddleware();
  private final String channel = newChannel();
  private boolean ack = false;
  private String ackUrl;
  private AckMiddleware ackMiddleware;

  /**
   * Initialize HttpEventCollectorSender
   *
   * @param Url http event collector input server
   * @param token application token
   * @param delay batching delay
   * @param maxEventsBatchCount max number of events in a batch
   * @param maxEventsBatchSize max size of batch
   * @param metadata events metadata
   */
  public HttpEventCollectorSender(
          final String Url, final String token,
          long delay, long maxEventsBatchCount, long maxEventsBatchSize,
          String sendModeStr,
          boolean ack,
          String ackUrl,
          Map<String, String> metadata) {
    this.url = Url;
    this.token = token;
    this.ack = ack;
    this.ackUrl = ackUrl;
        if (maxEventsBatchCount == 0 && maxEventsBatchSize > 0) {
      this.maxEventsBatchCount = Long.MAX_VALUE;
    } else if (maxEventsBatchSize == 0 && maxEventsBatchCount > 0) {
      this.maxEventsBatchSize = Long.MAX_VALUE;
    }
   this.delay= delay; 
   this.metadata=metadata;

    if (ack) {
      if (null == ackUrl || ackUrl.isEmpty()) {
        throw new RuntimeException(
                "AckUrl was not specified, but HttpEventCollectorSender set to use acks.");
      }
      this.ackMiddleware = new AckMiddleware(this);
      this.middleware.add(ackMiddleware);
    }
    
    if (sendModeStr != null) {
      if (sendModeStr.equals(SendModeSequential)) {
        this.sendMode = SendMode.Sequential;
      } else if (sendModeStr.equals(SendModeSParallel)) {
        this.sendMode = SendMode.Parallel;
      } else {
        throw new IllegalArgumentException(
                "Unknown send mode: " + sendModeStr);
      }
    }

  }
  
  public AckWindow getAckWindow(){
    return this.ackMiddleware.getAckManager().getAckWindow();
  }

  public void addMiddleware(
          HttpEventCollectorMiddleware.HttpSenderMiddleware middleware) {
    this.middleware.add(middleware);
  }

  /**
   * Send a single logging event
   *
   * @note in case of batching the event isn't sent immediately
   * @param severity event severity level (info, warning, etc.)
   * @param message event text
   */
  public synchronized void send(
          final String severity,
          final String message,
          final String logger_name,
          final String thread_name,
          Map<String, String> properties,
          final String exception_message,
          Serializable marker
  ) {
    this.eventsBatch = new EventBatch(this, 
                                              maxEventsBatchCount,
                                              maxEventsBatchSize, 
                                              delay, 
                                              metadata,
                                              timer);
    // create event info container and add it to the batch
    HttpEventCollectorEventInfo eventInfo
            = new HttpEventCollectorEventInfo(severity, message, logger_name,
                    thread_name, properties, exception_message, marker);
    eventsBatch.add(eventInfo);
  }
  
/**
   * Immediately send the EventBatch
   * @param events the batch of events to immediately send
   */
  public synchronized void sendBatch(EventBatch events) {
      if(events.isFlushed()){
        throw new IllegalStateException("Illegal attempt to send already-flushed batch. EventBatch is not reusable.");
      }
       this.eventsBatch = events;
       eventsBatch.setSender(this);
       eventsBatch.flush();

  }  

  /**
   * Flush all pending events
   */
  public synchronized void flush() {
    if(eventsBatch.isFlushable()){ //true if there were actually events to flush
      eventsBatch.flush();
      // Create new EventsBatch because events inside previous batch are
      // sending asynchronously and "previous" instance of EventBatch object
      // is still in use.
      eventsBatch = new EventBatch(this,
              eventsBatch.getMaxEventsBatchCount(),
              eventsBatch.getMaxEventsBatchSize(),
              eventsBatch.getFlushInterval(),
              eventsBatch.getMetadata(),
              timer);
    }

  }

  /**
   * Close events sender
   */
  public void close() {
    eventsBatch.close();
    this.ackMiddleware.close();
    timer.cancel();
  }

  /**
   * Disable https certificate validation of the splunk server. This
   * functionality is for development purpose only.
   */
  public void disableCertificateValidation() {
    disableCertificateValidation = true;
  }
  
  public ChannelMetrics getChannelMetrics(){
    return this.ackMiddleware.getChannelMetrics();
  }

  private void startHttpClient() {
    if (httpClient != null) {
      // http client is already started
      return;
    }
    // limit max  number of async requests in sequential mode, 0 means "use
    // default limit"
    int maxConnTotal = sendMode == SendMode.Sequential ? 1 : 0;
    if (!disableCertificateValidation) {
      // create an http client that validates certificates
      httpClient = HttpAsyncClients.custom()
              .setMaxConnTotal(maxConnTotal)
              .build();
    } else {
      // create strategy that accepts all certificates
      TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
        public boolean isTrusted(X509Certificate[] certificate,
                String type) {
          return true;
        }
      };
      SSLContext sslContext = null;
      try {
        sslContext = SSLContexts.custom().loadTrustMaterial(
                null, acceptingTrustStrategy).build();
        httpClient = HttpAsyncClients.custom()
                .setMaxConnTotal(maxConnTotal)
                .setHostnameVerifier(
                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).
                setSSLContext(sslContext)
                .build();
      } catch (Exception e) {
      }
    }
    httpClient.start();
  }

  // Currently we never close http client. This method is added for symmetry
  // with startHttpClient.
  private void stopHttpClient() throws SecurityException {
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (IOException e) {
      }
      httpClient = null;
    }
  }

  void postEventsAsync(final EventBatch events) {
    this.middleware.postEvents(events, this,
            new HttpEventCollectorMiddleware.IHttpSenderCallback() {
      @Override
      public void completed(int statusCode, String reply) {
        if (statusCode != 200) {          
          HttpEventCollectorErrorHandler.error(
                  events,
                  new HttpEventCollectorErrorHandler.ServerErrorException(
                          reply));
        }
      }

      @Override
      public void failed(Exception ex) {
        HttpEventCollectorErrorHandler.error(
                eventsBatch,
                new HttpEventCollectorErrorHandler.ServerErrorException(
                        ex.getMessage()));
      }
    });
  }

  @Override
  public void postEvents(final EventBatch events,
          final HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
    startHttpClient(); // make sure http client is started
    final String encoding = "utf-8";

    // create http request
    final HttpPost httpPost = new HttpPost(url);
    httpPost.setHeader(
            AuthorizationHeaderTag,
            String.format(AuthorizationHeaderScheme, token));
    if (ack) { //only send channel UUID if we are using acks 
      httpPost.setHeader(
              ChannelHeader,
              getChannel());
    }

    StringEntity entity = new StringEntity(eventsBatch.toString(),//eventsBatchString.toString(),
            encoding);
    entity.setContentType(HttpContentType);
    httpPost.setEntity(entity);
    httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
      @Override
      public void completed(HttpResponse response) {
        String reply = "";
        int httpStatusCode = response.getStatusLine().getStatusCode();
        // read reply only in case of a server error
        //if (httpStatusCode != 200) {
        try {
          reply = EntityUtils.toString(response.getEntity(), encoding);		
        } catch (IOException e) {
          //if IOException ocurrs toStringing response, this is not something we can expect client 
          //to handle
          throw new RuntimeException(e.getMessage(), e);
          //reply = e.getMessage();
        }
        //}
        callback.completed(httpStatusCode, reply);
      }

      @Override
      public void failed(Exception ex) {
        callback.failed(ex);
      }

      @Override
      public void cancelled() {
      }
    });
  }

  @Override
  public void pollAcks(AckManager ackMgr,
          HttpEventCollectorMiddleware.IHttpSenderCallback callback) {

    startHttpClient(); // make sure http client is started
    final String encoding = "utf-8";

    // create http request
    final HttpPost httpPost = new HttpPost(ackUrl);
    httpPost.setHeader(
            AuthorizationHeaderTag,
            String.format(AuthorizationHeaderScheme, token));
    if (!ack) {
      throw new RuntimeException(
              "getAcks called but acks have not been enabled.");
    }
    httpPost.setHeader(
            ChannelHeader,
            getChannel());

    StringEntity entity;
    try {
      String req = ackMgr.getAckPollReq().toString();
      System.out.println("posting acks: "+ req);      
      entity = new StringEntity(req);
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
    entity.setContentType(HttpContentType);
    httpPost.setEntity(entity);
    httpClient.execute(httpPost, new FutureCallback<HttpResponse>() {
      @Override
      public void completed(HttpResponse response) {
        String reply = "";
        int httpStatusCode = response.getStatusLine().getStatusCode();
        // read reply only in case of a server error
        //if (httpStatusCode != 200) {
        try {
          reply = EntityUtils.toString(response.getEntity(), encoding);
          //System.out.println("reply: " + reply);	//fixme undo hack 		
        } catch (IOException e) {
          //if IOException ocurrs toStringing response, this is not something we can expect client 
          //to handle
          throw new RuntimeException(e.getMessage(), e);
          //reply = e.getMessage();
        }
        //}
        callback.completed(httpStatusCode, reply);
      }

      @Override
      public void failed(Exception ex) {
        callback.failed(ex);
      }

      @Override
      public void cancelled() {
      }
    });

  }

}
