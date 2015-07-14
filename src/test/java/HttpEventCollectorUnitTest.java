/**
 * @copyright
 *
 * Copyright 2013-2015 Splunk, Inc.
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.HttpEventCollectorMiddleware;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.LogManager;

public class HttpEventCollectorUnitTest {
    @Test
    public void log4j_simple() {
        org.apache.logging.log4j.Logger LOG4J = org.apache.logging.log4j.LogManager.getLogger("splunk.log4j");
        // send 3 events
        HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertTrue(events.size() == 1);
                Assert.assertTrue(events.get(0).getMessage().compareTo("hello log4j") == 0);
                Assert.assertTrue(events.get(0).getSeverity().compareTo("INFO") == 0);
            }
        };
        LOG4J.info("hello log4j");
        LOG4J.info("hello log4j");
        LOG4J.info("hello log4j");
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 3);
    }

    @Test
    public void logback_simple() {
        org.slf4j.Logger LOGBACK = org.slf4j.LoggerFactory.getLogger("splunk.logback");
        // send 3 events
        HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertTrue(events.size() == 1);
                Assert.assertTrue(events.get(0).getMessage().compareTo("hello logback") == 0);
                Assert.assertTrue(events.get(0).getSeverity().compareTo("ERROR") == 0);
            }
        };
        LOGBACK.error("hello logback");
        LOGBACK.error("hello logback");
        LOGBACK.error("hello logback");
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 3);
    }

    @Test
    public void java_util_logger_simple() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n"
        );

        // send 3 events
        HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertTrue(events.size() == 1);
                Assert.assertTrue(events.get(0).getMessage().compareTo("hello java logger") == 0);
                Assert.assertTrue(events.get(0).getSeverity().compareTo("WARNING") == 0);
            }
        };
        LOGGER.warning("hello java logger");
        LOGGER.warning("hello java logger");
        LOGGER.warning("hello java logger");
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 3);
    }

    @Test
    public void java_util_logger_error_handler() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n"
        );

        // mimic server 404
        HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public HttpEventCollectorUnitTestMiddleware.HttpResponse output() {
                return new HttpEventCollectorUnitTestMiddleware.HttpResponse(
                        404, "{\"text\":\"error\",\"code\":4}"
                );
            }
        };
        HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
            public void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
                HttpEventCollectorErrorHandler.ServerErrorException serverErrorException =
                        (HttpEventCollectorErrorHandler.ServerErrorException) ex;
                Assert.assertTrue(serverErrorException.getReply().compareTo("{\"text\":\"error\",\"code\":4}") == 0);
                Assert.assertTrue(serverErrorException.getErrorCode() == 4);
            }
        });
        LOGGER.info("hello");
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 1);
    }

    @Test
    public void java_util_logger_resend() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.retries_on_error=2\n"
        );

        HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            int retries = 0;
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertTrue(events.get(0).getMessage().compareTo("hello") == 0);
                Assert.assertTrue(events.get(0).getSeverity().compareTo("INFO") == 0);
            }
            @Override
            public HttpEventCollectorUnitTestMiddleware.HttpResponse output() {
                retries++;
                if (retries <= 1) {
                    // mimic server error
                    return new HttpEventCollectorUnitTestMiddleware.HttpResponse(
                            0, ""
                    );
                } else {
                    return new HttpEventCollectorUnitTestMiddleware.HttpResponse();
                }
            }
        };
        LOGGER.info("hello");
        // the system should make 2 retries
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 2);
    }

    @Test
    public void java_util_logger_resend_max_retries() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.retries_on_error=2\n"
        );

        HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertTrue(events.get(0).getMessage().compareTo("hello") == 0);
                Assert.assertTrue(events.get(0).getSeverity().compareTo("INFO") == 0);
            }
            @Override
            public HttpEventCollectorUnitTestMiddleware.HttpResponse output() {
                // mimic server error
                return new HttpEventCollectorUnitTestMiddleware.HttpResponse(
                        0, "{\"text\":\"error\",\"code\":4}");
            }
        };
        HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
            public void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
                // ignore errors
            }
        });
        LOGGER.info("hello");
        // the system should make only 2 retries and stop after that
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 3);
    }

    @Test
    public void java_util_logger_batching() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count=3\n"
        );

        HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertTrue(events.size() == 3);
                Assert.assertTrue(events.get(0).getMessage().compareTo("one") == 0);
                Assert.assertTrue(events.get(1).getMessage().compareTo("two") == 0);
                Assert.assertTrue(events.get(2).getMessage().compareTo("three") == 0);
            }
        };
        LOGGER.info("one");
        LOGGER.info("two");
        LOGGER.info("three");
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 3);
    }

    private void readConf(final String conf) {
        try {
            LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(conf.getBytes()));
        } catch (IOException e) {
            Assert.fail();
        }
    }
}
