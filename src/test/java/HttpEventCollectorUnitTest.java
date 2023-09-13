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

import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.Log4jTestMiddleware;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.LogManager;

public class HttpEventCollectorUnitTest {
    @Test
    public void log4j_simple() throws Exception {
        HashMap<String, String> userInputs = new HashMap<String, String>();
        String loggerName = "splunk.log4jSimple";
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", "11111111-2222-3333-4444-555555555555");
        userInputs.put("user_middleware", "HttpEventCollectorUnitTestMiddleware");
        userInputs.put("user_batch_size_count", "1");
        userInputs.put("user_batch_size_bytes", "0");
        userInputs.put("user_eventBodySerializer", "BodySerializerDoesNotExistButShouldNotCrashTest");
        userInputs.put("user_eventHeaderSerializer", "HeaderSerializerDoesNotExistButShouldNotCrashTest");
        userInputs.put("user_errorCallback", "ErrorCallbackHeaderSerializerDoesNotExistButShouldNotCrashTest");
        TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        org.apache.logging.log4j.Logger LOG4J = org.apache.logging.log4j.LogManager.getLogger(loggerName);

        // send 3 events
        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(1, events.size());
                Assert.assertEquals(0, events.get(0).getMessage().compareTo("hello log4j"));
                Assert.assertEquals(0, events.get(0).getSeverity().compareTo("INFO"));
            }
        };
        LOG4J.info("hello log4j");
        LOG4J.info("hello log4j");
        LOG4J.info("hello log4j");
        if (HttpEventCollectorUnitTestMiddleware.eventsReceived == 0)
            sleep(15000);
        Assert.assertEquals(3, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void log4j_simple_with_errorCallback() throws Exception {
        HashMap<String, String> userInputs = new HashMap<>();
        String loggerName = "splunk.log4jSimple";
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", "11111111-2222-3333-4444-555555555555");
        userInputs.put("user_middleware", "HttpEventCollectorUnitTestMiddleware");
        userInputs.put("user_batch_size_count", "1");
        userInputs.put("user_batch_size_bytes", "0");
        userInputs.put("user_eventBodySerializer", "BodySerializerDoesNotExistButShouldNotCrashTest");
        userInputs.put("user_eventHeaderSerializer", "HeaderSerializerDoesNotExistButShouldNotCrashTest");
        userInputs.put("user_errorCallback", "com.splunk.logging.util.StandardErrorCallback");
        TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        org.apache.logging.log4j.Logger LOG4J = org.apache.logging.log4j.LogManager.getLogger(loggerName);

        // send 3 events
        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
            }

            @Override
            public HttpEventCollectorUnitTestMiddleware.HttpResponse output() {
                throw new IllegalArgumentException("call failure");
            }
        };
        LOG4J.info("hello log4j");
        Assert.assertEquals(1, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(1, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void log4j_subcomponents() throws Exception {
        final org.apache.logging.log4j.Level[] levels = { org.apache.logging.log4j.Level.WARN,
                org.apache.logging.log4j.Level.INFO, org.apache.logging.log4j.Level.DEBUG };
        final long[]  expectedCounts = { 1, 2, 3 };
        for (int i = 0; i < levels.length; i++) {
            HashMap<String, String> userInputs = new HashMap<>();
            String                  loggerName = "splunk.log4jSubcomponents";
            userInputs.put("user_logger_name", loggerName);
            userInputs.put("user_httpEventCollector_token", "11111111-2222-3333-4444-555555555555");
            userInputs.put("user_middleware", "HttpEventCollectorUnitTestMiddleware");
            userInputs.put("user_batch_size_count", "1");
            userInputs.put("user_batch_size_bytes", "0");
            userInputs.put("user_eventBodySerializer", "DoesNotExistButShouldNotCrashTest");
            userInputs.put("user_eventHeaderSerializer", "DoesNotExistButShouldNotCrashTest");
            userInputs.put("user_eventLevel", levels[i].toString());
            TestUtil.resetLog4j2Configuration("log4j2_subcomponents_template.xml", "log4j2.xml", userInputs);
            org.apache.logging.log4j.Logger LOG4J = org.apache.logging.log4j.LogManager.getLogger(loggerName);

            HttpEventCollectorUnitTestMiddleware.eventsReceived = 0;
            // send 3 events
            LOG4J.warn("hello log4j");
            LOG4J.info("hello log4j");
            LOG4J.debug("hello log4j");
            if (HttpEventCollectorUnitTestMiddleware.eventsReceived == 0)
                sleep(1000);
            Assert.assertEquals(expectedCounts[i], Log4jTestMiddleware.eventsReceived);
        }
    }

    @Test
    public void logback_simple() throws Exception {
        HashMap<String, String> userInputs = new HashMap<String, String>();
        final String loggerName = "splunk.logback";
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", "11111111-2222-3333-4444-555555555555");
        userInputs.put("user_middleware", "HttpEventCollectorUnitTestMiddleware");
        userInputs.put("user_eventBodySerializer", "BodySerializerDoesNotExistButShouldNotCrashTest");
        userInputs.put("user_eventHeaderSerializer", "HeaderSerializerDoesNotExistButShouldNotCrashTest");
        userInputs.put("user_errorCallback", "ErrorCallbackDoesNotExistButShouldNotCrashTest");
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);
        org.slf4j.Logger LOGBACK = org.slf4j.LoggerFactory.getLogger(loggerName);

        // send 3 events
        HttpEventCollectorUnitTestMiddleware.resetCounters();;
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(1, events.size());
                Assert.assertEquals(0, events.get(0).getMessage().compareTo("hello logback"));
                Assert.assertEquals(0, events.get(0).getSeverity().compareTo("ERROR"));
                Assert.assertEquals(0, events.get(0).getLoggerName().compareTo(loggerName));
            }
        };
        LOGBACK.error("hello logback");
        LOGBACK.error("hello logback");
        LOGBACK.error("hello logback");
        Assert.assertEquals(3, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void java_util_logger_simple() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_bytes=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_interval=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.errorCallback=com.splunk.logging.util.StandardErrorCallback\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.eventBodySerializer=BodySerializerDoesNotExistButShouldNotCrashTest\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.eventHeaderSerializer=HeaderSerializerDoesNotExistButShouldNotCrashTest\n"
        );

        // send 3 events
        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(1, events.size());
                Assert.assertEquals(0, events.get(0).getMessage().compareTo("hello java logger"));
                Assert.assertEquals(0, events.get(0).getSeverity().compareTo("WARNING"));
            }
        };
        LOGGER.warning("hello java logger");
        LOGGER.warning("hello java logger");
        LOGGER.warning("hello java logger");
        Assert.assertEquals(3, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void java_util_logger_error_handler() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_bytes=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_interval=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n"
        );

        // mimic server 404
        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public HttpEventCollectorUnitTestMiddleware.HttpResponse output() {
                return new HttpEventCollectorUnitTestMiddleware.HttpResponse(
                        404, "{\"text\":\"error\",\"code\":4}"
                );
            }
        };
        HttpEventCollectorErrorHandler.onError((data, ex) -> {
            HttpEventCollectorErrorHandler.ServerErrorException serverErrorException =
                    (HttpEventCollectorErrorHandler.ServerErrorException) ex;
            Assert.assertEquals(0, serverErrorException.getReply().compareTo("{\"text\":\"error\",\"code\":4}"));
            Assert.assertEquals(4, serverErrorException.getErrorCode());
        });
        LOGGER.info("hello");
        Assert.assertEquals(1, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(1, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void java_util_logger_resend() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_bytes=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_interval=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.retries_on_error=2\n"
        );

        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            int retries = 0;
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(0, events.get(0).getMessage().compareTo("hello"));
                Assert.assertEquals(0, events.get(0).getSeverity().compareTo("INFO"));
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
        Assert.assertEquals(2, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(1, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void java_util_logger_resend_max_retries() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
            "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_count=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_size_bytes=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.batch_interval=0\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n" +
            "com.splunk.logging.HttpEventCollectorLoggingHandler.retries_on_error=2\n"
        );

        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(0, events.get(0).getMessage().compareTo("hello"));
                Assert.assertEquals(0, events.get(0).getSeverity().compareTo("INFO"));
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
        Assert.assertEquals(3, HttpEventCollectorUnitTestMiddleware.eventsReceived);
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

        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(3, events.size());
                Assert.assertEquals(0, events.get(0).getMessage().compareTo("one"));
                Assert.assertEquals(0, events.get(1).getMessage().compareTo("two"));
                Assert.assertEquals(0, events.get(2).getMessage().compareTo("three"));
            }
        };
        LOGGER.info("one");
        LOGGER.info("two");
        LOGGER.info("three");
        Assert.assertEquals(3, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void java_util_logger_batching_default_count() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
                "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n"
        );
        final int DefaultBatchCount = 10;
        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertTrue(events.size() == DefaultBatchCount);
            }
        };
        for (int i = 0; i < DefaultBatchCount * 100; i ++) {
            LOGGER.info("*");
        }
        Assert.assertEquals(DefaultBatchCount * 100, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void java_util_logger_batching_default_size() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
                "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n"
        );
        final int DefaultBatchSize = 10 * 1024;
        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(1, events.size());
            }
        };
        for (int i = 0; i < 10; i ++) {
            LOGGER.info(repeat("x", DefaultBatchSize));
        }
        Assert.assertEquals(10, HttpEventCollectorUnitTestMiddleware.eventsReceived);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    @Test
    public void java_util_logger_batching_default_interval() {
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        readConf(
                "handlers=com.splunk.logging.HttpEventCollectorLoggingHandler\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.url=http://localhost:8088\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.token=TOKEN\n" +
                "com.splunk.logging.HttpEventCollectorLoggingHandler.middleware=HttpEventCollectorUnitTestMiddleware\n"
        );
        final int DefaultInterval = 10000;
        HttpEventCollectorUnitTestMiddleware.resetCounters();
        HttpEventCollectorUnitTestMiddleware.io = new HttpEventCollectorUnitTestMiddleware.IO() {
            @Override
            public void input(List<HttpEventCollectorEventInfo> events) {
                Assert.assertEquals(1, events.size());
            }
        };
        LOGGER.info("=|:-)");
        sleep(DefaultInterval / 2);
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 0);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
        sleep(DefaultInterval);
        Assert.assertTrue(HttpEventCollectorUnitTestMiddleware.eventsReceived == 1);
        Assert.assertEquals(0, HttpEventCollectorUnitTestMiddleware.eventsWithFailures);
    }

    //--------------------------------------------------------------------------
    // utils

    private void readConf(final String conf) {
        try {
            LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(conf.getBytes()));
        } catch (IOException e) {
            Assert.fail();
        }
    }

    private final String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < times ; i ++)
            sb.append(str);
        return sb.toString();
    }

    private void sleep(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
