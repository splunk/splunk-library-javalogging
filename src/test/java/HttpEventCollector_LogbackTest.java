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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorEventInfo;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public final class HttpEventCollector_LogbackTest {

    private String httpEventCollectorName = "LogbackTest";
    List<List<HttpEventCollectorEventInfo>> errors = new ArrayList<>();
    List<Exception> logEx = new ArrayList<>();

    /**
     * sending a message via httplogging using logback to splunk
     */
    @Test
    public void canSendEventUsingLogback() throws Exception {
        TestUtil.enableHttpEventCollector();
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "logBackLogger";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_defined_httpEventCollector_token", token);
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);

        List<String> msgs = new ArrayList<String>();

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for Logback Test}", date.toString());
        Logger logger = LoggerFactory.getLogger(loggerName);
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test error for Logback Test}", date.toString());
        logger.error(jsonMsg);
        msgs.add(jsonMsg);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test debug for Logback Test}", date.toString());
        logger.debug(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * sending a message via httplogging using logback to splunk
     */
    @Test
    public void canSendExceptionUsingLogback() throws Exception {
        TestUtil.enableHttpEventCollector();
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "logBackLogger";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_defined_httpEventCollector_token", token);
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);

        List<String> msgs = new ArrayList<String>();

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for Logback Test}", date.toString());
        Logger logger = LoggerFactory.getLogger(loggerName);
        logger.error(jsonMsg, new RuntimeException("This is RuntimeException"));
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * Sending a commented string messages via httplogging using logback to Splunk
     *
     * @throws Exception
     */
    @Test
    public void canSendCommentedEventStringUsingLogback() throws Exception {
        TestUtil.enableHttpEventCollector();
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "logBackLoggerWithCommentedEventMessages";
        HashMap<String, String> userInputs = new HashMap<>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_defined_httpEventCollector_token", token);
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);

        List<String> messages = new ArrayList<>();

        Date date = new Date();
        Logger logger = LoggerFactory.getLogger(loggerName);
        String commentedEventMessage = "// This is a single line commented string. Dated: " + date.toString();
        logger.info(commentedEventMessage);
        messages.add(commentedEventMessage);

        commentedEventMessage = "/* This is a document type commented string. */ Dated: " + date.toString();
        logger.error(commentedEventMessage);
        messages.add(commentedEventMessage);

        commentedEventMessage = "## This is a normal commented string. Dated: " + date.toString();
        logger.warn(commentedEventMessage);
        messages.add(commentedEventMessage);

        TestUtil.verifyEventsSentToSplunk(messages);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * sending a message via httplogging using logback to splunk
     */
    @Test
    public void canSendEventUsingLogbackWithOptions() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "logBackLogger";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest");
        userInputs.put("user_sourcetype", "battlecat");
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);

        List<String> msgs = new ArrayList<String>();

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for Logback Test}", date.toString());
        Logger logger = LoggerFactory.getLogger(loggerName);
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test error for Logback Test}", date.toString());
        logger.error(jsonMsg);
        msgs.add(jsonMsg);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test debug for Logback Test}", date.toString());
        logger.debug(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * sending batched message using logback to splunk
     */
    @Test
    public void canSendBatchEventByCount() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "logBackBatchLoggerCount";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        //userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count", "5");
        //userInputs.put("user_batch_size_bytes","500000");
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest_BatchSize");
        userInputs.put("user_sourcetype", "battlecat_BatchSize");

        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);
        Logger logger = LoggerFactory.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for logback 1}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 1");
        TestUtil.verifyNoEventSentToSplunk(msgs);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for logback 2}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 2");
        TestUtil.verifyNoEventSentToSplunk(msgs);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for logback 3}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 3");
        TestUtil.verifyNoEventSentToSplunk(msgs);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for logback 4}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 4");
        TestUtil.verifyNoEventSentToSplunk(msgs);

        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for logback 5}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);

    }

    /**
     * sending batched message using logback to splunk
     */
    @Test
    public void canSendBatchEventBySize() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "logBackBatchLoggerSize";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_size_bytes", "500");
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest_BatchSize");
        userInputs.put("user_sourcetype", "battlecat_BatchSize");

        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);
        Logger logger = LoggerFactory.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();

        int size = 0;
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 1}", new Date().toString());
        size += jsonMsg.length();
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 2}", new Date().toString());
        size += jsonMsg.length();
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 3, adding more msg to exceed the maxsize}", new Date().toString());
        while (size + jsonMsg.length() < 550) {
            jsonMsg = String.format("%saaaaa", jsonMsg);
        }

        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);

    }

    /**
     * error handling
     */
    @Test
    public void errorHandlingInvalidToken() throws Exception {
        errors.clear();
        logEx.clear();
        //define error callback
        HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
            public void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
                synchronized (errors) {
                    errors.add(data);
                    logEx.add((HttpEventCollectorErrorHandler.ServerErrorException) ex);
                }
            }
        });

        //create a token used for httpEventCollector logging, then make it becomes invalid
        httpEventCollectorName = "wrongtoken";
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "wrongToken";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);
        Logger logger = LoggerFactory.getLogger(loggerName);

        //disable the token so that it becomes invalid
        TestUtil.disableHttpEventCollector(httpEventCollectorName);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event disabled token }", new Date().toString());
        logger.info(jsonMsg);

        //delete the token so that it becomes invalid
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event deleted token}", new Date().toString());
        logger.info(jsonMsg);

        //wait for async process to return the error
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 60 * 1000)/*wait for up to 60s*/ {
            if (logEx.size() >= 2)
                break;
            Thread.sleep(1000);
        }

        if (logEx == null)
            Assert.fail("didn't catch errors");


        System.out.println("======print logEx");
        System.out.println(logEx.toString());
        System.out.println("======finish print logEx");
        // in this case expect a valid http reply with a json error message
        HttpEventCollectorErrorHandler.ServerErrorException serverErrorException = (HttpEventCollectorErrorHandler.ServerErrorException) logEx.get(1);
        Assert.assertEquals("Invalid token", serverErrorException.getErrorText());
        Assert.assertEquals(4, serverErrorException.getErrorCode());


        for (List<HttpEventCollectorEventInfo> infos : errors) {
            for (HttpEventCollectorEventInfo info : infos) {
                System.out.println(info.getMessage());
            }
        }
        Assert.assertEquals(2, errors.size());
    }


    /**
     * error handling
     */
    @Test
    public void errorHandlingDisabledHttpEventCollectorEndpoint() throws Exception {
        errors.clear();
        logEx.clear();

        //define error callback
        HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
            public void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
                synchronized (errors) {
                    errors.add(data);
                    logEx.add(ex);
                }
            }
        });

        //create a token used for httpEventCollector logging, then make it becomes invalid
        httpEventCollectorName = "wrongtoken";
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "wrongToken";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);
        Logger logger = LoggerFactory.getLogger(loggerName);


        //disable httpEventCollector endpoint
        TestUtil.disableHttpEventCollector();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event httpEventCollector disabled}", new Date().toString());
        logger.info(jsonMsg);

        //wait for async process to return the error
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 60 * 1000)/*wait for up to 60s*/ {
            if (logEx.size() >= 1)
                break;
            Thread.sleep(1000);
        }

        TestUtil.enableHttpEventCollector();
        
        if (logEx == null)
            Assert.fail("didn't catch errors");
        Assert.assertEquals(1, errors.size());

        System.out.println(logEx.toString());
        if (!StringUtils.containsAny(logEx.toString(), "Failed to connect to", "Remote host terminated the handshake", "Connection reset"))
            Assert.fail(String.format("Unexpected error message '%s'", logEx.toString()));
    }

    /**
     * verify events are index in correct order of the events were sent
     */
    @Test
    public void eventsIsIndexedInOrderOfSent() throws Exception {
        TestUtil.enableHttpEventCollector();
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String indexName = "httpevents_in_order_lb";
        TestUtil.createIndex(indexName);

        String loggerName = "logBackLogger";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_defined_httpinput_token", token);
        userInputs.put("user_index", indexName);
        userInputs.put("user_send_mode", "sequential");
        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);

        Date date = new Date();
        List<String> msgs = new ArrayList<String>();
        Logger logger = LoggerFactory.getLogger(loggerName);

        //send multiple events and verify they are indexed in the order of sending
        int totalEventsCount = 1000;
        String prefix = "logback multiple events";
        for (int i = 0; i < totalEventsCount; i++) {
            String jsonMsg = String.format("%s %s", prefix, i);
            logger.info(jsonMsg);
            msgs.add(jsonMsg);
        }

        TestUtil.verifyEventsSentInOrder(prefix, totalEventsCount, indexName);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        System.out.println("====================== Test pass=========================");
    }
    
    /**
     * Test sending a JSON and text message with "_json" source type via http logging appender using logback
     */
    @Test
    public void canSendJsonEventUsingLogbackWithJsonSourceType() throws Exception {
        canSendJsonEventUsingLogbackWithSourceType("_json");
    }
    
    /**
     * Test sending a JSON and text message with "battlecat_test" source type via http logging appender using logback
     */
    @Test
    public void canSendJsonEventUsingLogbackWithDefaultSourceType() throws Exception {
        canSendJsonEventUsingLogbackWithSourceType("battlecat_test");
    }
    
    @SuppressWarnings("unchecked")
    private void canSendJsonEventUsingLogbackWithSourceType(final String sourceType) throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        final String loggerName = "logBackLogger";

        // Build User input map
        final HashMap<String, String> userInputs = TestUtil.buildUserInputMap(loggerName, token, sourceType, "json");

        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);

        final List<String> msgs = new ArrayList<>();

        final long timeMillsec = new Date().getTime();

        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("transactionId", new JsonPrimitive("11"));
        jsonObject.add("userId", new JsonPrimitive("21"));
        jsonObject.add("eventTimestamp", new JsonPrimitive(timeMillsec));

        final Logger logger = LoggerFactory.getLogger(loggerName);

        // Test with a json event message
        jsonObject.add("severity", new JsonPrimitive("info"));
        final String infoJson = jsonObject.toString();
        logger.info(infoJson);
        msgs.add(infoJson);

        jsonObject.add("severity", new JsonPrimitive("error"));
        final String errorJson = jsonObject.toString();
        logger.error(errorJson);
        msgs.add(errorJson);

        // Test with a text event message
        jsonObject.add("severity", new JsonPrimitive("debug"));
        final String debugText = String.format("{EventTimestamp:%s, EventMsg:'this is a test debug for Logback Test}", timeMillsec);
        logger.debug(debugText);
        msgs.add(debugText);

        TestUtil.verifyEventsSentToSplunk(msgs);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }
}
