package com.splunk.logging;

/**
 *
 * Define the interface to allow users to define their own event body serializer for HTTP event adapter:
 * Simply create a class implementing this interface, and add the full class name as a property (`eventBodySerializer`) to the adapter.
 *
 * @see com.splunk.logging.serialization.PlainTextEventBodySerializer
 *
*/
public interface EventBodySerializer {

    String serializeEventBody(
            HttpEventCollectorEventInfo eventInfo,
            Object formattedMessage
    );

    /**
     * Timestamp to be sent with custom message.
     * @return 0 if do not want to send timestamp with message, otherwise number of seconds, between the current time and midnight, January 1, 1970 UTC.
     */
    default double getEventTime(HttpEventCollectorEventInfo eventInfo) {
        return 0;
    }
}
