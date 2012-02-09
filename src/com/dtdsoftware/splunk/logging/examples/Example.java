package com.dtdsoftware.splunk.logging.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtdsoftware.splunk.logging.SplunkLogEvent;

/**
 * <pre>
 * Developers just log as per usual using a logging facade or directly with the log framework implementation.
 * For this example I am using the SLF4J facade, and you can plug in jdk logging, log4j or logback as the implementation.
 * </pre>
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class Example {

	public static void main(String[] args) {

		// get your logger
		Logger logger = LoggerFactory.getLogger("splunk.logger");

		// log a regular string
		logger.debug("REST for the wicked");

		// create a SplunkLogEvent
		SplunkLogEvent event = new SplunkLogEvent("Failed Login",
				"sshd:failure");

		// add CIM fields either using setter methods or static variables

		// event.addPair(SplunkLogEvent.AUTH_APP, "myapp");
		event.setAuthApp("myapp");

		// event.addPair(SplunkLogEvent.AUTH_USER, "jane");
		event.setAuthUser("jane");

		// add a custom field
		event.addPair("somefieldname", "foobar");

		// log a splunk log event generated string
		logger.info(event.toString());

	}

}
