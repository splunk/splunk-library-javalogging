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

/**
 *
 * @author ghendrey
 */
public class AckLifecycleState {
  public enum State {
    PRE_EVENT_POST, EVENT_POST_OK, EVENT_POST_NOT_OK, EVENT_POST_FAILURE, PRE_ACK_POLL, ACK_POLL_OK, ACK_POLL_NOT_OK, ACK_POLL_FAILURE
  };  
  private final State currentState;
  private final EventBatch events;

  public AckLifecycleState(State currentState, EventBatch events) {
    this.currentState = currentState;
    this.events = events;
  }

  /**
   * @return the currentState
   */
  public State getCurrentState() {
    return currentState;
  }

  /**
   * @return the events
   */
  public EventBatch getEvents() {
    return events;
  }
}
