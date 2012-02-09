package com.dtdsoftware.splunk.logging.jdk.handler;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {

		return record.getMessage();
	}

}
