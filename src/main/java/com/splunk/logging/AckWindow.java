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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Keeps track of acks that we are waiting for success on. Updates ChannelMetrics
 * every time an ackId is created, and also when success is received on an ackId. This 
 * is not really a window in the sense of a sliding window but "window" seems apropos to
 * describe it.
 * @author ghendrey
 */
public class AckWindow {

  private Set<Long> acks = new LinkedHashSet<>();
  @JsonIgnore
  private final ObjectMapper mapper = new ObjectMapper();
  @JsonIgnore
  private final ChannelMetrics channelMetrics;
  private final HttpEventCollectorSender sender;

  AckWindow(HttpEventCollectorSender sender) {
    this.sender = sender;
    this.channelMetrics = new ChannelMetrics(sender);
  }
 

  @Override
  public String toString() {
    try {
      return mapper.writeValueAsString(this); //this class itself marshals out to {"acks":[id,id,id]}
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }

  @JsonIgnore
  public boolean isEmpty(){
    return acks.isEmpty();
  }
  
  public void add(EventPostResponse epr) {
    Long ackId = epr.getAckId();
    acks.add(ackId);
    channelMetrics.ackIdCreated(ackId);
  }

  public void remove(AckPollResponse apr) {
    Collection<Long> succeeded = apr.getSuccessIds();
    if(succeeded.isEmpty()){
      return;
    }
    acks.removeAll(succeeded);
    channelMetrics.ackIdSucceeded(succeeded);
  }

  /**
   * @return the acks
   */
  public Set<Long> getAcks() {
    return acks;
  }

  /**
   * @param acks the acks to set
   */
  public void setAcks(Set<Long> acks) {
    this.acks = acks;
  }

  ChannelMetrics getChannelMetrics() {
    return this.channelMetrics;
  }

 

}
