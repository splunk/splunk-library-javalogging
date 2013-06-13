package com.splunk.logging.jdk.handler;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Custom formatter that simply echos the logged message
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkFormatter extends Formatter {

	/**
	 * Just echo the logged message
	 */
	@Override
	public String format(LogRecord record) {

		return record.getMessage();
	}

}
