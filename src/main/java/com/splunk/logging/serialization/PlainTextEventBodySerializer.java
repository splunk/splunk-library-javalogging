package com.splunk.logging.serialization;

import com.splunk.logging.EventBodySerializer;
import com.splunk.logging.HttpEventCollectorEventInfo;

/**
 * Custom serializer which sends message in plain-text format and provides message timestamp with millisecond precision.
 */
public class PlainTextEventBodySerializer implements EventBodySerializer {

	@Override
	public String serializeEventBody(HttpEventCollectorEventInfo eventInfo, Object formattedMessage) {
		return String.valueOf(formattedMessage);
	}

	@Override
	public double getEventTime(HttpEventCollectorEventInfo eventInfo) {
		return eventInfo.getTime();
	}

}
