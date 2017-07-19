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
import java.util.Date;
import java.util.Observable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 * @author ghendrey
 */
public class ChannelMetrics extends Observable implements AckLifecycle {

  private static final ObjectMapper mapper = new ObjectMapper(); //JSON serializer
  private final ConcurrentMap<Long, Long> birthTimes = new ConcurrentSkipListMap<>(); //ackid -> creation time
  private long oldestUnackedBirthtime = Long.MIN_VALUE;
  private long mostRecentTimeToSuccess = 0;
  private final HttpEventCollectorSender sender;
  private long eventPostCount;
  private long eventPostOKCount;
  private long eventPostNotOKCount;
  private long eventPostFailureCount;
  private long ackPollCount;
  private long ackPollOKCount;
  private long ackPollNotOKCount;
  private long ackPollFailureCount;

  ChannelMetrics(HttpEventCollectorSender sender) {
    this.sender = sender;
  }

  @Override
  public String toString() {
    try {
      return "METRICS ---> " + mapper.writeValueAsString(this);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }

  public void ackIdCreated(long ackId) {
    long birthtime = System.currentTimeMillis();
    birthTimes.put(ackId, birthtime);
    if (oldestUnackedBirthtime == Long.MIN_VALUE) { //not set yet id MIN_VALUE
      oldestUnackedBirthtime = birthtime; //this happens only once. It's a dumb firt run edgecase
      this.setChanged();
      this.notifyObservers(sender);
    }
  }

  private void ackIdSucceeded(long ackId) {

    Long birthTime;
    if (null != (birthTime = birthTimes.remove(ackId))) {
      this.mostRecentTimeToSuccess = System.currentTimeMillis() - birthTime;
      if (oldestUnackedBirthtime == this.mostRecentTimeToSuccess) { //in this case we just processed the oldest ack
        oldestUnackedBirthtime = scanForOldestUnacked();//so we need to figure out which unacked id is now oldest
      }
    } else {
      throw new IllegalStateException("no birth time recorded for ackId: " + ackId);
    }

  }

  private long scanForOldestUnacked() {
    long oldest = Long.MAX_VALUE;
    for (long birthtime : birthTimes.values()) { //O(n) acceptable 'cause window gonna be small
      if (birthtime < oldest) {
        oldest = birthtime;
      }
    }
    return oldest;
  }

  /**
   * @return the unacknowledgedCount
   */
  public int getUnacknowledgedCount() {
    return birthTimes.size();
  }

  public String getOldest() {
    return new Date(oldestUnackedBirthtime).toString();
  }

  /**
   * @return the mostRecentTimeToSuccess
   */
  public long getMostRecentTimeToSuccess() {
    return mostRecentTimeToSuccess; //Duration.ofMillis(mostRecentTimeToSuccess).toString();
  }

  /**
   * @return the birthTimes
   */
  public ConcurrentMap<Long, Long> getBirthTimes() {
    return birthTimes;
  }

  /**
   * @return the oldestUnackedBirthtime
   */
  public long getOldestUnackedBirthtime() {
    return oldestUnackedBirthtime;
  }

  @Override
  public void preEventsPost(EventBatch batch) {
    eventPostCount++;
    //setChanged();
    //notifyObservers(PRE_POST);
  }

  @Override
  public void eventPostOK(EventBatch events) {
    eventPostOKCount++;
  }

  @Override
  public void eventPostNotOK(int code, String msg, EventBatch events) {
    eventPostNotOKCount++;
  }

  @Override
  public void eventPostFailure(Exception ex) {
    eventPostFailureCount++;
  }

  @Override
  public void preAckPoll() {
    ackPollCount++;
  }

  @Override
  public void ackPollOK(EventBatch events) {
    ackPollOKCount++;
    ackIdSucceeded(events.getAckId());
    setChanged();
    AckLifecycleState state = new AckLifecycleState(
            AckLifecycleState.State.ACK_POLL_OK, events);
    notifyObservers(state);
  }

  @Override
  public void ackPollNotOK(int statusCode, String reply) {
    ackPollNotOKCount++;
  }

  @Override
  public void ackPollFailed(Exception ex) {
    ackPollFailureCount++;
  }

}
