/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.splunk.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author ghendrey
 */
public class EventBatch implements SerializedEventProducer {
  
  private final long maxEventsBatchCount;
  private final long maxEventsBatchSize;
  private final long flushInterval;
  private Map<String, String> metadata;
  private final Timer timer;
  private final TimerTask flushTask = new ScheduledFlush();
  private final List<HttpEventCollectorEventInfo> eventsBatch = new ArrayList();
  private final HttpEventCollectorSender sender;
  private final StringBuilder stringBuilder = new StringBuilder();
  private boolean flushed = false;


  public EventBatch(HttpEventCollectorSender sender, long maxEventsBatchCount, long maxEventsBatchSize,
          long flushInterval, Map<String, String> metadata, Timer timer) {
    this.sender = sender;
    // when size configuration setting is missing it's treated as "infinity",
    // i.e., any value is accepted.
    if (maxEventsBatchCount == 0 && maxEventsBatchSize > 0) {
      maxEventsBatchCount = Long.MAX_VALUE;
    } else if (maxEventsBatchSize == 0 && maxEventsBatchCount > 0) {
      maxEventsBatchSize = Long.MAX_VALUE;
    }    
    this.maxEventsBatchCount = maxEventsBatchCount;
    this.maxEventsBatchSize = maxEventsBatchSize;
    this.metadata = metadata;
    this.flushInterval = flushInterval;
    this.timer = timer;
    if (this.flushInterval > 0) {
      // start scheduled flush timer
      timer.scheduleAtFixedRate(this.flushTask, flushInterval, flushInterval);
    }    
  }
  
  
  public synchronized void add(HttpEventCollectorEventInfo event){
    if(flushed){
      throw new IllegalStateException("Events cannot be added to a flushed EventBatch");
    }
    if(null == this.metadata){
      throw new RuntimeException("Metadata not set for events");
    }
    
    eventsBatch.add(event);
    stringBuilder.append(event.toString(metadata));
    sender.flush(); //will call back to EventBatch.isFlushable and might then flush accordingly       
  }
  
  protected synchronized boolean isFlushable(){
   //technically the serialized size that we compate to maxEventsBatchSize should take into account
    //the character encoding. it's very difficult to compute statically. We use the stringBuilder length
    //which is a character count. Will be same as serialized size only for single-byte encodings like
    //US-ASCII of ISO-8859-1    
    return eventsBatch.size() >= maxEventsBatchCount || serializedCharCount() > maxEventsBatchSize;   
  }
  
  protected synchronized void flush() {
      flushTask.cancel();
      sender.postEventsAsync(this);
      flushed = true;
  }
  
  /**
   * Close events sender
   */
  public synchronized void close() {
    //send any pending events, regardless of how many or how big 
    flush();
  }  
 
  @Override
  public String toString() {
    return this.stringBuilder.toString();
 }

  @Override
  public void setEventMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  int serializedCharCount() {
    return stringBuilder.length();
  }
  
  int getNumEvents(){
    return eventsBatch.size();
  }
  
  public int size(){
    return eventsBatch.size();
  }
  
  public HttpEventCollectorEventInfo get(int idx){
    return this.eventsBatch.get(idx);
  }
  
public List<HttpEventCollectorEventInfo>  getEvents(){
  return this.eventsBatch;
}

  /**
   * @return the maxEventsBatchCount
   */
  public long getMaxEventsBatchCount() {
    return maxEventsBatchCount;
  }

  /**
   * @return the maxEventsBatchSize
   */
  public long getMaxEventsBatchSize() {
    return maxEventsBatchSize;
  }

  /**
   * @return the flushInterval
   */
  public long getFlushInterval() {
    return flushInterval;
  }

  /**
   * @return the metadata
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }
  
  private class ScheduledFlush extends TimerTask{

    @Override
    public void run() {
        flush();      
    }
    
  }
  
}
