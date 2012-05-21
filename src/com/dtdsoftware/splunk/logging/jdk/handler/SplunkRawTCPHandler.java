package com.dtdsoftware.splunk.logging.jdk.handler;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import com.dtdsoftware.splunk.logging.SplunkRawTCPInput;

/**
 * java.util.logging handler for sending events to Splunk via Raw TCP
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkRawTCPHandler extends Handler {

	// connection settings
	private String host = "";
	private int port = 5150;
	
	//queuing settings
	private String maxQueueSize; 
	private boolean dropEventsOnQueueFull;

	private SplunkRawTCPInput sri;

	/**
	 * Constructor
	 */
	public SplunkRawTCPHandler() {

		configure();

		try {

			sri = new SplunkRawTCPInput(this.host, this.port);
			sri.setMaxQueueSize(maxQueueSize);
			sri.setDropEventsOnQueueFull(dropEventsOnQueueFull);
		} catch (Exception e) {

		}

	}

	/**
	 * Read in the handler properties from the config file
	 */
	private void configure() {

		LogManager manager = LogManager.getLogManager();
		String cname = getClass().getName();

		setHost(manager.getProperty(cname + ".host"));
		setPort(Integer.parseInt(manager.getProperty(cname + ".port")));
		setMaxQueueSize(manager.getProperty(cname + ".maxQueueSize"));
		setDropEventsOnQueueFull(Boolean.parseBoolean(manager.getProperty(cname + ".dropEventsOnQueueFull")));
		setLevel(Level.parse(manager.getProperty(cname + ".level")));
		setFilter(null);
		setFormatter(new SplunkFormatter());

		try {
			setEncoding(manager.getProperty(cname + ".encoding"));
		} catch (Exception ex) {
			try {
				setEncoding(null);
			} catch (Exception ex2) {
			}
		}

	}

	/**
	 * Clean up resources
	 */
	@Override
	synchronized public void close() throws SecurityException {

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

	/**
	 * Log the message
	 */
	@Override
	public void publish(LogRecord record) {

		if (!isLoggable(record)) {
			return;
		}
		if (sri == null) {
			return;
		}

		String formatted = getFormatter().format(record);

		sri.streamEvent(formatted);

	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		if (host != null)
			this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		if (port > 0)
			this.port = port;
	}
	
	public String getMaxQueueSize() {
		return maxQueueSize;
	}

	public void setMaxQueueSize(String maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}

	public boolean isDropEventsOnQueueFull() {
		return dropEventsOnQueueFull;
	}

	public void setDropEventsOnQueueFull(boolean dropEventsOnQueueFull) {
		this.dropEventsOnQueueFull = dropEventsOnQueueFull;
	}


	@Override
	public void flush() {
	}

}
