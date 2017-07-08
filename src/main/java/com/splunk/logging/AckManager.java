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
 * AckManager is the mediator between sending and receiving messages to splunk (as such it is the 
 * only piece of the Ack-system that touches the HttpEventCollectorSender). AckManager sends via the sender 
 * and receives and unmarshals responses.  From these responses it  maintains the
 * ack window by adding newly received ackIds to the ack window, or removing them on success. It also owns
 * the AckPollScheduler which will periodically call back "pollAcks" on this, which sends the content of the 
 * ackWindow to Splunk via the sender, to check their status.
 * @author ghendrey
 */
public class AckManager {
  private static final ObjectMapper mapper = new ObjectMapper();
  private HttpEventCollectorSender sender;
  private final AckPollScheduler ackPollController = new AckPollScheduler();
  private final AckWindow ackWindow = new AckWindow();


  AckManager(HttpEventCollectorSender sender) {
    this.sender = sender;
  }

  /**
   * @return the ackPollReq
   */
  public AckWindow getAckPollReq() {
    return ackWindow;
  }
  
  public ChannelMetrics getChannelMetrics(){
    return ackWindow.getChannelMetrics();
  }

  public void consumeEventPostResponse(String resp) {    
    EventPostResponse epr;
    try {
      Map<String, Object> map = mapper.readValue(resp,
              new TypeReference<Map<String, Object>>() {
      });
      epr = new EventPostResponse(map);
    } catch (IOException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
    ackWindow.add(epr);
    if(!ackPollController.isStarted()){
      ackPollController.start(this);
    }    
   
  }
  
    public void consumeAckPollResponse(String resp) {
    try {
      AckPollResponse ackPollResp = mapper.
              readValue(resp, AckPollResponse.class);
      this.ackWindow.remove(ackPollResp);
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
