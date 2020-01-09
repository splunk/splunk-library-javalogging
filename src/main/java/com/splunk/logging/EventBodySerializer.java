package com.splunk.logging;

/**
 *
 * Define the interface to allow users to define their own event body serializer for HTTP event adapter:
 * Simply create a class implementing this interface, and add the full class name as a property (`eventBodySerializer`) to the adapter.
 *
*/
public interface EventBodySerializer {

    String serializeEventBody(
            HttpEventCollectorEventInfo eventInfo,
            Object formattedMessage
    );
}
