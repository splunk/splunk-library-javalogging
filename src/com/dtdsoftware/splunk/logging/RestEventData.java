package com.dtdsoftware.splunk.logging;

/**
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class RestEventData {

	private String source = "";
	private String sourcetype = "";
	private String index = "";
	private String host = "";
	private String hostRegex = "";

	public RestEventData() {
	}

	public RestEventData(String source, String sourcetype, String index,
			String host, String hostRegex) {
		this.source = source;
		this.sourcetype = sourcetype;
		this.index = index;
		this.host = host;
		this.hostRegex = hostRegex;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourcetype() {
		return sourcetype;
	}

	public void setSourcetype(String sourcetype) {
		this.sourcetype = sourcetype;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHostRegex() {
		return hostRegex;
	}

	public void setHostRegex(String hostRegex) {
		this.hostRegex = hostRegex;
	}

}
