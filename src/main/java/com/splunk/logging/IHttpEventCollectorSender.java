package com.splunk.logging;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Map;

/**
 * @copyright Copyright 2013-2015 Splunk, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

public interface IHttpEventCollectorSender extends Closeable {
    String SPLUNKREQUESTCHANNELTag = "X-Splunk-Request-Channel";
    String AuthorizationHeaderTag = "Authorization";
    String AuthorizationHeaderScheme = "Splunk %s";
    String HttpEventCollectorUriPath = "/services/collector/event/1.0";
    String HttpRawCollectorUriPath = "/services/collector/raw";
    String HttpContentType = "application/json; profile=urn:splunk:event:1.0; charset=utf-8";
    String SendModeSequential = "sequential";
    String SendModeSParallel = "parallel";

    /**
     * Recommended default values for events batching.
     */
    int DefaultBatchInterval = 10 * 1000; // 10 seconds
    int DefaultBatchSize = 10 * 1024; // 10KB
    int DefaultBatchCount = 10; // 10 events

    void send(final String severity,
              final String message,
              final String logger_name,
              final String thread_name,
              Map<String, String> properties,
              final String exception_message,
              Serializable marker);

    void flush();
}
