/*
 * Copyright 2013-2014 Splunk, Inc.
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

import com.splunk.logging.HttpEventCollectorSender;
import org.json.simple.JSONObject;
import com.splunk.logging.EventHeaderSerializer;
import com.splunk.logging.HttpEventCollectorEventInfo;

import java.util.Dictionary;
import java.util.Locale;

// Implement the interface of EventHeaderSerializer for testing
public class TestEventHeaderSerializer implements EventHeaderSerializer {

    @Override
    public JSONObject serializeEventHeader(
            final HttpEventCollectorEventInfo eventInfo,
            final Dictionary<String, String> metadata
    ) {
        JSONObject event = new JSONObject();

        HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataTimeTag, String.format(Locale.US, "%.3f", eventInfo.getTime()));
        HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataHostTag, metadata.get(HttpEventCollectorSender.MetadataHostTag));
        HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataSourceTag, metadata.get(HttpEventCollectorSender.MetadataSourceTag));
        HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataSourceTypeTag, metadata.get(HttpEventCollectorSender.MetadataSourceTypeTag));

        String index = eventInfo.getMessage();
        index = index.substring(index.indexOf(':') + 1);
        HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataIndexTag, index);

        return event;
    }
}

