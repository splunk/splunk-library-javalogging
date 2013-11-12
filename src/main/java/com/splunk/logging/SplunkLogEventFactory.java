package com.splunk.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory class that allows you to set SplunkLogEvent templates and generate
 * clones. Useful for reuse when you log many events with the same key=value
 * pair signatures.
 * 
 * <code>
 * 
 * SplunkLogEvent eventTemplate = new SplunkLogEvent("Login Event","foo");
 * SplunkLogEventFactory.addTemplate("foo", eventTemplate);
 * 	
 * SplunkLogEvent event = SplunkLogEventFactory.getInstanceFromTemplate("foo");	
 * event.addPair("someparam", "helloworld");
 * 
 * </code>
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public abstract class SplunkLogEventFactory {

	// map to hold the templates
	private static Map<String, SplunkLogEvent> templates = new HashMap<String, SplunkLogEvent>();

	/**
	 * Add a template SplunkLogEvent object
	 * 
	 * @param name
	 * @param template
	 */
	public static void addTemplate(String name, SplunkLogEvent template) {

		if (template != null)
			templates.put(name, template);
	}

	/**
	 * Remove a template SplunkLogEvent object
	 * 
	 * @param name
	 */
	public static void removeTemplate(String name) {

		templates.remove(name);
	}

	/**
	 * Get a new SplunkLogEvent object that is a clone of a template
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static SplunkLogEvent getInstanceFromTemplate(String name)
			throws Exception {

		SplunkLogEvent template = templates.get(name);
		if (template != null) {
			return template.clone();
		} else
			throw new Exception("SplunkLogEvent template " + name
					+ " does not exist");
	}
}
