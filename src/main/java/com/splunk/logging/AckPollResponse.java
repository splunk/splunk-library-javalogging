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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 * @author ghendrey
 */
public class AckPollResponse {
  private Map<String, Boolean> acks = new ConcurrentSkipListMap<>();

  public AckPollResponse() {
  } 
  
  Collection<?> getSuccessIds() {
    Set<Long> successful = new HashSet<>();
    for(Map.Entry<String,Boolean> e:acks.entrySet()){
      if(e.getValue()){
        successful.add(Long.parseLong(e.getKey()));
      }
    }
    return successful;
  }

  /**
   * @return the acks
   */
  public Map<String, Boolean> getAcks() {
    return acks;
  }

  /**
   * @param acks the acks to set
   */
  public void setAcks(
          Map<String, Boolean> acks) {
    this.acks = acks;
  }
  
}
