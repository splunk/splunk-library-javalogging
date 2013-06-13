package com.splunk.logging.logback.appender;

import com.splunk.logging.RestEventData;
import com.splunk.logging.SplunkRestInput;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

/**
 * LogBack Appender for sending events to Splunk via REST
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkRestAppender extends AppenderBase<ILoggingEvent> {

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

	private Layout<ILoggingEvent> layout;

	/**
	 * Constructor
	 */
	public SplunkRestAppender() {
	}

	/**
	 * Log the message
	 */
	@Override
	protected void append(ILoggingEvent event) {

		if (sri != null) {

			String formatted = layout.doLayout(event);
			if (delivery.equals(STREAM))
				sri.streamEvent(formatted);
			else if (delivery.equals(SIMPLE))
				sri.sendEvent(formatted);
			else {
				addError("Unsupported delivery setting for SplunkRestAppender named \""
						+ this.name + "\".");
				return;
			}
		}
	}

	/**
	 * Initialisation logic
	 */
	@Override
	public void start() {

		if (this.layout == null) {
			addError("No layout set for the appender named [" + name + "].");
			return;
		}

		if (sri == null) {
			try {
				sri = new SplunkRestInput(user, pass, host, port, red, delivery
						.equals(STREAM) ? true : false);
				sri.setMaxQueueSize(maxQueueSize);
				sri.setDropEventsOnQueueFull(dropEventsOnQueueFull);
			} catch (Exception e) {
				addError("Couldn't establish REST service for SplunkRestAppender named \""
						+ this.name + "\".");
			}
		}
		super.start();
	}

	/**
	 * Clean up resources
	 */
	@Override
	public void stop() {
		if (sri != null) {
			try {
				sri.closeStream();
				sri = null;
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				sri = null;
			}
		}
		super.stop();
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

	public Layout<ILoggingEvent> getLayout() {
		return layout;
	}

	public void setLayout(Layout<ILoggingEvent> layout) {
		this.layout = layout;
	}

}
