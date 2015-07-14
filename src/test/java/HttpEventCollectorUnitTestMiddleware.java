import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.HttpEventCollectorMiddleware;

import java.util.List;

/**
 * @copyright
 *
 * Copyright 2013-2015 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**
 * Middleware component that mimics a real http event collector server.
 */
public class HttpEventCollectorUnitTestMiddleware extends HttpEventCollectorMiddleware.HttpSenderMiddleware {
    @Override
    public void postEvents(List<HttpEventCollectorEventInfo> events,
                           HttpEventCollectorMiddleware.IHttpSender sender,
                           HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        eventsReceived += events.size();
        io.input(events);
        HttpResponse response = io.output();
        if (response.status > 0)
            callback.completed(response.status, response.reply);
        else
            callback.failed(new Exception(response.reply));
    }

    public static class HttpResponse {
        public int status = 200;
        public String reply = "{\"text\":\"Success\",\"code\":0}";
        public HttpResponse(int status, final String reply) {
            this.status = status;
            this.reply = reply;
        }
        public HttpResponse() {}
    }

    public static class IO {
        public void input(List<HttpEventCollectorEventInfo> events) {}
        public HttpResponse output() { return new HttpResponse(); }
    }

    public static IO io = new IO();

    public static int eventsReceived = 0;
}
