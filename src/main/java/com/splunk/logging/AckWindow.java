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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps track of acks that we are waiting for success on. Updates
 * ChannelMetrics every time an ackId is created, and also when success is
 * received on an ackId. This is not really a window in the sense of a sliding
 * window but "window" seems apropos to describe it.
 *
 * @author ghendrey
 */
public class AckWindow {

  private static final Logger LOG = Logger.getLogger(AckWindow.class.getName());

  private final static ObjectMapper mapper = new ObjectMapper();
  private final Map<Long, EventBatch> polledAcks = new ConcurrentSkipListMap<>(); //key ackID
  private final Map<String, EventBatch> postedEventBatches = new ConcurrentSkipListMap<>();//key EventBatch ID
  private final ChannelMetrics channelMetrics;

  AckWindow(ChannelMetrics channelMetrics) {
    this.channelMetrics = channelMetrics;
  }

  @Override
  public String toString() {

    try {
      Map json = new HashMap();
      json.put("acks", polledAcks.keySet()); //{"acks":[1,2,3...]}
      return mapper.writeValueAsString(json); //this class itself marshals out to {"acks":[id,id,id]}
    } catch (JsonProcessingException ex) {
      Logger.getLogger(AckWindow.class.getName()).log(Level.SEVERE, null, ex);
      throw new RuntimeException(ex.getMessage(), ex);
    }

  }

  public boolean isEmpty() {
    return this.channelMetrics.isChannelEmpty();//polledAcks.isEmpty() && postedEventBatches.isEmpty();
  }

  public void preEventPost(EventBatch batch) {
    postedEventBatches.put(batch.getId(), batch);  //track what we attempt to post, so in case fail we can try again  
  }

  public void handleEventPostResponse(EventPostResponse epr,
          EventBatch events) {
    Long ackId = epr.getAckId();
    postedEventBatches.remove(events.getId()); //we are now sure the server reveived the events POST
    polledAcks.put(ackId, events);
    channelMetrics.ackIdCreated(ackId, events);
  }

  public void handleAckPollResponse(AckPollResponse apr) {
    try {
      Collection<Long> succeeded = apr.getSuccessIds();
      System.out.println("success acked ids: " + succeeded);
      if (succeeded.isEmpty()) {
        return;
      }

      for (long ackId : succeeded) {
        EventBatch events = this.polledAcks.get(ackId);
        System.out.println("got ack on channel=" + events.getSender().getChannel() + ", seqno=" + events.getId() +", ackid=" + events.getAckId());
        if(ackId != events.getAckId()){
          String msg = "ackId mismatch key ackID=" +ackId + " recordedAckId=" + events.getAckId();
          LOG.severe(msg);
          throw new IllegalStateException(msg);
        }
        if (null == events) {
          Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                  "Unable to find EventBatch in buffer for successfully acknowledged ackId: {0}",
                  ackId);
        }
        
        channelMetrics.ackPollOK(events);
        //polledAcks.remove(ackId);      
      }
      //System.out.println("polledAcks was " + polledAcks.keySet());
      polledAcks.keySet().removeAll(succeeded);
      //System.out.println("polledAcks now " + polledAcks.keySet());
    } catch (Exception e) {
      LOG.severe("caught exception in handleAckPollResponse: " + e.getMessage());
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  ChannelMetrics getChannelMetrics() {
    return this.channelMetrics;
  }

  public Set<EventBatch> getUnacknowleldgedEvents() {
    Set<EventBatch> unacked = new HashSet<>();
    unacked.addAll(this.postedEventBatches.values());
    unacked.addAll(this.polledAcks.values());
    return unacked;
  }

}
