package com.splunk.logging;

/**
 * A helper class to encapsulate URL parameters for submitting events to Splunk
 * via REST endpoints
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class RestEventData {

	// REST URL parameter keys
	public static String RECEIVERS_SIMPLE_ARG_INDEX = "index";
	public static String RECEIVERS_SIMPLE_ARG_SOURCE = "source";
	public static String RECEIVERS_SIMPLE_ARG_SOURCETYPE = "sourcetype";
	public static String RECEIVERS_SIMPLE_ARG_HOST = "host";
	public static String RECEIVERS_SIMPLE_ARG_HOSTREGEX = "host_regex";

	// URL parameter values
	private String source = "";
	private String sourcetype = "";
	private String index = "";
	private String host = "";
	private String hostRegex = "";

	/**
	 * Default constructor
	 */
	public RestEventData() {
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            The source value to fill in the metadata for this input's
	 *            events.
	 * @param sourcetype
	 *            The sourcetype to apply to events from this input.
	 * @param index
	 *            The index to send events from this input to.
	 * @param host
	 *            The value to populate in the host field for events from this
	 *            data input.
	 * @param hostRegex
	 *            A regular expression used to extract the host value from each
	 *            event.
	 */
	public RestEventData(String source, String sourcetype, String index,
			String host, String hostRegex) {
		this.source = source;
		this.sourcetype = sourcetype;
		this.index = index;
		this.host = host;
		this.hostRegex = hostRegex;
	}

	/**
	 * The source value to fill in the metadata for this input's events.
	 * 
	 * @return
	 */
	public String getSource() {
		return source;
	}

	/**
	 * The source value to fill in the metadata for this input's events.
	 * 
	 * @param source
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * The sourcetype to apply to events from this input.
	 * 
	 * @return
	 */
	public String getSourcetype() {
		return sourcetype;
	}

	/**
	 * The sourcetype to apply to events from this input.
	 * 
	 * @param sourcetype
	 */
	public void setSourcetype(String sourcetype) {
		this.sourcetype = sourcetype;
	}

	/**
	 * The index to send events from this input to.
	 * 
	 * @return
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * The index to send events from this input to.
	 * 
	 * @param index
	 */
	public void setIndex(String index) {
		this.index = index;
	}

	/**
	 * The value to populate in the host field for events from this data input.
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	/**
	 * The value to populate in the host field for events from this data input.
	 * 
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * A regular expression used to extract the host value from each event.
	 * 
	 * @return
	 */
	public String getHostRegex() {
		return hostRegex;
	}

	/**
	 * A regular expression used to extract the host value from each event.
	 * 
	 * @param hostRegex
	 */
	public void setHostRegex(String hostRegex) {
		this.hostRegex = hostRegex;
	}

}
