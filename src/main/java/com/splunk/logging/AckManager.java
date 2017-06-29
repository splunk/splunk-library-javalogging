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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author ghendrey
 */
public class AckManager {
  private static final ObjectMapper mapper = new ObjectMapper();
  private HttpEventCollectorSender sender;
  private final AckPollScheduler ackPollController = new AckPollScheduler();
  private final AckPollRequest ackPollReq = new AckPollRequest();


  AckManager(HttpEventCollectorSender sender) {
    this.sender = sender;
  }

  /**
   * @return the ackPollReq
   */
  public AckPollRequest getAckPollReq() {
    return ackPollReq;
  }
  
  public ChannelMetrics getChannelMetrics(){
    return ackPollReq.getChannelMetrics();
  }

  public void consumeEventPostResponse(String resp) {    
    System.out.println(resp);
    EventPostResponse epr;
    try {
      Map<String, Object> map = mapper.readValue(resp,
              new TypeReference<Map<String, Object>>() {
      });
      epr = new EventPostResponse(map);
    } catch (IOException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
    ackPollReq.add(epr);
    if(!ackPollController.isStarted()){
      ackPollController.start(this);
    }    
   
  }
  
    public void consumeAckPollResponse(String resp) {
    try {
      System.out.println(resp);
      AckPollResponse ackPollResp = mapper.
              readValue(resp, AckPollResponse.class);
      this.ackPollReq.remove(ackPollResp);
      if(this.ackPollReq.isEmpty()){
        this.ackPollController.stop();
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }

  }

  
  public void pollAcks(){
    sender.pollAcks(this,
            new HttpEventCollectorMiddleware.IHttpSenderCallback() {
      @Override
      public void completed(int statusCode, String reply) {
        consumeAckPollResponse(reply);
      }

      @Override
      public void failed(Exception ex) {
        throw new RuntimeException(ex.getMessage(), ex);
      }
    });
  }


  /**
   * @return the sender
   */
  HttpEventCollectorSender getSender() {
    return sender;
  }

}
