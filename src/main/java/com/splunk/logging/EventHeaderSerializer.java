package com.splunk.logging;

import java.util.Map;

/**
 *
 * Define the interface to allow users to define their own event header serializer for HTTP event adapter:
 * Simply create a class implementing this interface, and add the full class name as a property (`eventHeaderSerializer`) to the adapter.
 *
*/
public interface EventHeaderSerializer {

    Map<String, Object> serializeEventHeader(
            final HttpEventCollectorEventInfo eventInfo,
            final Map<String, Object> metadata
    );
}
