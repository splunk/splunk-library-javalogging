package com.dtdsoftware.splunk.logging.log4j.appender;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import com.dtdsoftware.splunk.logging.SplunkRawTCPInput;

/**
 * Log4j Appender for sending events to Splunk via Raw TCP
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkRawTCPAppender extends AppenderSkeleton {

	// connection settings
	private String host = "";
	private int port = 5150;

	private SplunkRawTCPInput sri;

	/**
	 * Constructor
	 */
	public SplunkRawTCPAppender() {
	}

	/**
	 * Constructor
	 * 
	 * @param layout
	 *            the layout to apply to the log event
	 */
	public SplunkRawTCPAppender(Layout layout) {

		this.layout = layout;
	}

	/**
	 * Log the message
	 */
	@Override
	protected void append(LoggingEvent event) {

		try {
			if (sri == null) {
				sri = new SplunkRawTCPInput(host, port);
			}
		} catch (Exception e) {
			errorHandler
					.error("Couldn't establish Raw TCP connection for SplunkRawTCPAppender named \""
							+ this.name + "\".");
			return;
		}

		String formatted = layout.format(event);

		sri.streamEvent(formatted);

	}

	/**
	 * Clean up resources
	 */
	@Override
	synchronized public void close() {

		closed = true;
		if (sri != null) {
			try {
				sri.closeStream();
				sri = null;
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				sri = null;
			}
		}

	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
