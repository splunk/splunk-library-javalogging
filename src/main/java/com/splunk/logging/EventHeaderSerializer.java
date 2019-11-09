package com.splunk.logging;

import org.json.simple.JSONObject;

import java.util.Dictionary;
import java.util.Locale;

/**
 *
 * Define the interface to allow users to define their own event header serializer for HTTP event adapter:
 * Simply create a class implementing this interface, and add the full class name as a property (`eventHeaderSerializer`) to the adapter.
 *
*/
public interface EventHeaderSerializer {


    JSONObject serializeEventHeader(
            final HttpEventCollectorEventInfo eventInfo,
            final Dictionary<String, String> metadata
    );

    class Default implements EventHeaderSerializer {

        @Override
        public JSONObject serializeEventHeader(
                final HttpEventCollectorEventInfo eventInfo,
                final Dictionary<String, String> metadata
        ) {
            JSONObject event = new JSONObject();

            // event timestamp and metadata
            HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataTimeTag, String.format(Locale.US, "%.3f", eventInfo.getTime()));
            HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataHostTag, metadata.get(HttpEventCollectorSender.MetadataHostTag));
            HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataIndexTag, metadata.get(HttpEventCollectorSender.MetadataIndexTag));
            HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataSourceTag, metadata.get(HttpEventCollectorSender.MetadataSourceTag));
            HttpEventCollectorSender.putIfPresent(event, HttpEventCollectorSender.MetadataSourceTypeTag, metadata.get(HttpEventCollectorSender.MetadataSourceTypeTag));

            return event;
        }
    }
}
