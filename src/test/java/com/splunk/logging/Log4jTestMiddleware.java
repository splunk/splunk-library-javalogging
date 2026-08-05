package com.splunk.logging;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;

import com.splunk.logging.HttpEventCollectorMiddleware.HttpSenderMiddleware;
import com.splunk.logging.HttpEventCollectorMiddleware.IHttpSender;
import com.splunk.logging.HttpEventCollectorMiddleware.IHttpSenderCallback;

@Plugin(name = "Log4jTestMiddleware", category = "Core", elementType = HttpEventCollectorLog4jAppender.MIDDLEWARE_TYPE)
public class Log4jTestMiddleware extends HttpSenderMiddleware {

    @PluginFactory
    public static Log4jTestMiddleware createMiddleware(@PluginValue("level") String level) {
        return new Log4jTestMiddleware(Level.valueOf(level));
    }

    public static long eventsReceived = 0;
    private Level level;

    private Log4jTestMiddleware(Level level) {
        this.level = level;
    }

    @Override
    public void postEvents(List<HttpEventCollectorEventInfo> events, IHttpSender sender, IHttpSenderCallback callback) {
        eventsReceived += events.stream().filter(e -> Level.valueOf(e.getSeverity()).compareTo(level) <= 0).count();
    }

}
