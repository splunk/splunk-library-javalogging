package com.splunk.logging;
/*
 * Copyright 2013-2014 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Splunk Http Appender.
 */
@Plugin(name = "Http", category = "Core", elementType = "appender", printObject = true)
public final class HttpAppender extends AbstractAppender
{
    private HttpInputEventSender _eventSender;

	private HttpAppender(final String name,
			             final String url, 
			             final String token, 
			             final String source, 
			             final String sourcetype, 
			             final String index, 
			             final Filter filter, 
			             final Layout<? extends Serializable> layout, 
			             final boolean ignoreExceptions,
                         final String disableCertificateValidation)
	{
     	super(name, filter, layout, ignoreExceptions);
        // init events sender
        Dictionary<String, String> metadata = new Hashtable<String, String>();
        metadata.put(HttpInputEventSender.MetadataIndexTag, index);
        metadata.put(HttpInputEventSender.MetadataSourceTag, source);
        metadata.put(HttpInputEventSender.MetadataSourceTypeTag, sourcetype);
        // @todo - batching SPL-96375
        _eventSender = new HttpInputEventSender(url, token, 0, 0, 0, metadata);
        if (disableCertificateValidation.equalsIgnoreCase("true")) {
            _eventSender.disableCertificateValidation();
        }
	}
			
	/**
     * Create a Http Appender.
     * @return The Http Appender.
     */
    @PluginFactory
    public static HttpAppender createAppender(
            // @formatter:off
            @PluginAttribute("url") final String url,
            @PluginAttribute("protocol") final String protocol,            
            @PluginAttribute("token") final String token,
            @PluginAttribute("name") final String name,
            @PluginAttribute("source") final String source,
            @PluginAttribute("sourcetype") final String sourcetype, 
            @PluginAttribute("index") final String index,             
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginAttribute("disableCertificateValidation") final String disableCertificateValidation,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter
    		)
    {
    	if (name == null)
    	{
            LOGGER.error("No name provided for HttpAppender");
            return null;
        }
    	
    	if (url == null)
    	{
            LOGGER.error("No Splunk URL provided for HttpAppender");
            return null;
        }
    	
    	if (token == null)
    	{
    	    LOGGER.error("No token provided for HttpAppender");
            return null;
        }
    	
    	if (protocol == null)
    	{
    	    LOGGER.error("No valid protocol provided for HttpAppender");
            return null;
        }
    	
    	if (layout == null)
    	{
    		layout = PatternLayout.createLayout("%m", null, null, Charset.forName("UTF-8"), true, false, null, null);    		
        }
    	
    	final boolean ignoreExceptions = true;
    	
    	return new HttpAppender(name, protocol + "://" + url, token, source, sourcetype, index, filter, layout, ignoreExceptions, disableCertificateValidation);
    }
    
   
    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event)
    {
        _eventSender.send(
            event.getLevel().toString(),
            event.getMessage().getFormattedMessage()
        );
    }
}
