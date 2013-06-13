package com.splunk.logging.log4j.appender;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import com.splunk.logging.RestEventData;
import com.splunk.logging.SplunkRestInput;

/**
 * Log4j Appender for sending events to Splunk via REST
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkRestAppender extends AppenderSkeleton {

	public static final String STREAM = "stream";
	public static final String SIMPLE = "simple";

	// connection settings
	private String user = "";
	private String pass = "";
	private String host = "";
	private int port = 8089;
	private String delivery = STREAM; // stream or simple

	// event meta data
	private String metaSource = "";;
	private String metaSourcetype = "";;
	private String metaIndex = "";;
	private String metaHostRegex = "";;
	private String metaHost = "";;

	//queuing settings
	private String maxQueueSize; 
	private boolean dropEventsOnQueueFull;
	
	private SplunkRestInput sri;
	private RestEventData red = new RestEventData();

	/**
	 * Constructor
	 */
	public SplunkRestAppender() {
	}

	/**
	 * Constructor
	 * 
	 * @param layout
	 *            the layout to apply to the log event
	 */
	public SplunkRestAppender(Layout layout) {

		this.layout = layout;
	}

	/**
	 * Log the message
	 */
	@Override
	protected void append(LoggingEvent event) {

		try {
			if (sri == null) {
				sri = new SplunkRestInput(user, pass, host, port, red, delivery
						.equals(STREAM) ? true : false);
				sri.setMaxQueueSize(maxQueueSize);
				sri.setDropEventsOnQueueFull(dropEventsOnQueueFull);
			}
		} catch (Exception e) {
			errorHandler
					.error("Couldn't establish REST service for SplunkRestAppender named \""
							+ this.name + "\".");
			return;
		}

		String formatted = layout.format(event);
		if (delivery.equals(STREAM))
			sri.streamEvent(formatted);
		else if (delivery.equals(SIMPLE))
			sri.sendEvent(formatted);
		else {
			errorHandler
					.error("Unsupported delivery setting for SplunkRestAppender named \""
							+ this.name + "\".");
			return;
		}

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

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
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

	public String getDelivery() {
		return delivery;
	}

	public void setDelivery(String delivery) {
		this.delivery = delivery;
	}

	public String getMetaSource() {
		return metaSource;
	}

	public void setMetaSource(String metaSource) {
		this.metaSource = metaSource;
		red.setSource(metaSource);
	}

	public String getMetaSourcetype() {
		return metaSourcetype;
	}

	public void setMetaSourcetype(String metaSourcetype) {
		this.metaSourcetype = metaSourcetype;
		red.setSourcetype(metaSourcetype);
	}

	public String getMetaIndex() {
		return metaIndex;
	}

	public void setMetaIndex(String metaIndex) {
		this.metaIndex = metaIndex;
		red.setIndex(metaIndex);
	}

	public String getMetaHostRegex() {
		return metaHostRegex;
	}

	public void setMetaHostRegex(String metaHostRegex) {
		this.metaHostRegex = metaHostRegex;
		red.setHostRegex(metaHostRegex);
	}

	public String getMetaHost() {
		return metaHost;
	}

	public void setMetaHost(String metaHost) {
		this.metaHost = metaHost;
		red.setHost(metaHost);
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


}
