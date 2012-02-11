package com.dtdsoftware.splunk.logging.jdk.handler;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import com.dtdsoftware.splunk.logging.RestEventData;
import com.dtdsoftware.splunk.logging.SplunkRestInput;

/**
 * java.util.logging handler for sending events to Splunk via REST
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkRestHandler extends Handler {

	public static final String STREAM = "stream";
	public static final String SIMPLE = "simple";

	// connection settings
	private String user = "";
	private String pass = "";
	private String host = "";
	private int port = 8089;
	private String delivery = STREAM; // stream or simple

	// event meta data
	private String metaSource = "";
	private String metaSourcetype = "";
	private String metaIndex = "";
	private String metaHostRegex = "";
	private String metaHost = "";

	private SplunkRestInput sri;
	private RestEventData red = new RestEventData();

	/**
	 * Constructor
	 */
	public SplunkRestHandler() {

		configure();

		try {

			sri = new SplunkRestInput(this.user, this.pass, this.host,
					this.port, this.red, this.delivery.equals(STREAM) ? true
							: false);
		} catch (Exception e) {

		}

	}

	/**
	 * Read in the handler properties from the config file
	 */
	private void configure() {

		LogManager manager = LogManager.getLogManager();
		String cname = getClass().getName();

		setUser(manager.getProperty(cname + ".user"));
		setPass(manager.getProperty(cname + ".pass"));
		setHost(manager.getProperty(cname + ".host"));
		setDelivery(manager.getProperty(cname + ".delivery"));
		setPort(Integer.parseInt(manager.getProperty(cname + ".port")));

		setMetaSource(manager.getProperty(cname + ".metaSource"));
		setMetaHost(manager.getProperty(cname + ".metaHost"));
		setMetaHostRegex(manager.getProperty(cname + ".metaHostRegex"));
		setMetaIndex(manager.getProperty(cname + ".metaIndex"));
		setMetaSourcetype(manager.getProperty(cname + ".metaSourcetype"));

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
		if (delivery.equals(STREAM)) {
			sri.streamEvent(formatted);
		}
		else if (delivery.equals(SIMPLE))
			sri.sendEvent(formatted);
		else {
			return;
		}

	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		if (user != null)
			this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		if (pass != null)
			this.pass = pass;
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

	public String getDelivery() {
		return delivery;
	}

	public void setDelivery(String delivery) {
		if (delivery != null)
			this.delivery = delivery;
	}

	public String getMetaSource() {
		return metaSource;
	}

	public void setMetaSource(String metaSource) {
		if (metaSource != null) {
			this.metaSource = metaSource;
			red.setSource(metaSource);
		}
	}

	public String getMetaSourcetype() {
		return metaSourcetype;
	}

	public void setMetaSourcetype(String metaSourcetype) {
		if (metaSourcetype != null) {
			this.metaSourcetype = metaSourcetype;
			red.setSourcetype(metaSourcetype);
		}
	}

	public String getMetaIndex() {
		return metaIndex;
	}

	public void setMetaIndex(String metaIndex) {
		if (metaIndex != null) {
			this.metaIndex = metaIndex;
			red.setIndex(metaIndex);
		}
	}

	public String getMetaHostRegex() {
		return metaHostRegex;
	}

	public void setMetaHostRegex(String metaHostRegex) {
		if (metaHostRegex != null) {
			this.metaHostRegex = metaHostRegex;
			red.setHostRegex(metaHostRegex);
		}
	}

	public String getMetaHost() {
		return metaHost;
	}

	public void setMetaHost(String metaHost) {
		if (metaHost != null) {
			this.metaHost = metaHost;
			red.setHost(metaHost);
		}
	}

	@Override
	public void flush() {}

}
