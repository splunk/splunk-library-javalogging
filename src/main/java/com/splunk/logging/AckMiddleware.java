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

import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ghendrey
 */
class AckMiddleware extends HttpEventCollectorMiddleware.HttpSenderMiddleware {
  
  final AckManager ackMgr;

  AckMiddleware(final HttpEventCollectorSender sender) {
    this.ackMgr = new AckManager(sender);
    ackMgr.getChannelMetrics().
            addObserver((Observable o, Object arg) -> {
      System.out.println(o); //print out channel metrics
    });
  }

  @Override
  public void postEvents(final EventBatch events,
          HttpEventCollectorMiddleware.IHttpSender sender,
          HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
    callNext(events, sender,
            new HttpEventCollectorMiddleware.IHttpSenderCallback() {
      @Override
      public void completed(int statusCode, final String reply) {
        System.out.println("reply: " + reply);
        if (statusCode == 200) {
          try {
            ackMgr.consumeEventPostResponse(reply);
          } catch (Exception ex) {
            Logger.getLogger(AckMiddleware.class.getName()).
                    log(Level.SEVERE, null, ex);
          }
        } else {
          Logger.getLogger(AckMiddleware.class.getName()).
                  log(Level.SEVERE, "server didn't return ack ids");
        }
      }

      @Override
      public void failed(final Exception ex) {
        System.out.println("ooops failed");
        throw new RuntimeException(ex.getMessage(), ex);
      }
    });
  }
  
  public ChannelMetrics getChannelMetrics(){
    return this.ackMgr.getChannelMetrics();
  }
  
}
