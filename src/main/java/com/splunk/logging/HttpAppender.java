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
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.entity.StringEntity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Splunk Http Appender.
 */
@Plugin(name = "Http", category = "Core", elementType = "appender", printObject = true)
public final class HttpAppender extends AbstractAppender
{
	private final String _url;
	private final String _token;
	private final String _source;
	private final String _sourcetype;
	private final String _index;
		
	private HttpAppender(final String name, 
			             final String url, 
			             final String token, 
			             final String source, 
			             final String sourcetype, 
			             final String index, 
			             final Filter filter, 
			             final Layout<? extends Serializable> layout, 
			             final boolean ignoreExceptions)
	{
		super(name, filter, layout, ignoreExceptions);
		_url = url;
		_token = token;
		_source = source;
		_sourcetype = sourcetype;
		_index = index;
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
    	
    	return new HttpAppender(name, protocol + "://" + url, token, source, sourcetype, index, filter, layout, ignoreExceptions);    	
    }
    
   
    /**
     * Perform Appender specific appending actions.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event)
    {   
    	try
    	{
    		JSONObject obj = new JSONObject();
    	
    		if (_source != null)
    			obj.put("source", _source);
    	
    		if (_sourcetype != null)
    			obj.put("sourcetype", _sourcetype);
    	
    		if (_index != null)
    			obj.put("index", _index);
    		    		    	
        	String evt = new String(getLayout().toByteArray(event), "UTF-8");    		
    		obj.put("event", evt);
    		    		
    		CloseableHttpClient client = HttpClients.createDefault();    		
    		HttpPost post = new HttpPost(_url);
	    	post.setHeader("Authorization", "Splunk " + _token);
	    	StringEntity entity = new StringEntity(obj.toJSONString(), "utf-8");
	    	entity.setContentType("application/json; charset=utf-8"); 
	    	post.setEntity(entity);

	    	HttpResponse response = client.execute(post);    	
	    	int responseCode = response.getStatusLine().getStatusCode();
	    	
	    	if (responseCode != 200)
	    	{
	    		LOGGER.error("http server responded with error code %d", responseCode);			    		
	    	}
	    	
	    	client.close();
    	}
    	catch(IOException e )
    	{
    		LOGGER.error(e.getMessage());		
    	}
    }    
}



