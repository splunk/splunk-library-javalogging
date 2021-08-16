package com.splunk.logging;/*
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

import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Logger;

public final class HttpEventCollector_JavaLoggingTest {

    private String httpEventCollectorName = "JavaLoggingTest";
    List<List<HttpEventCollectorEventInfo>> errors = new ArrayList<List<HttpEventCollectorEventInfo>>();
    List<HttpEventCollectorErrorHandler.ServerErrorException> logEx = new ArrayList<HttpEventCollectorErrorHandler.ServerErrorException>();

    /**
     * sending a message via httplogging using java.logging to splunk
     */
    @Test
    public void canSendEventUsingJavaLogging() throws Exception {
        TestUtil.enableHttpEventCollector();

        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "splunkLoggerNoOptions";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_logger_name", loggerName);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging canSendEventUsingJavaLogging}", date.toString());

        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk(jsonMsg);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * sending a message via httplogging using java.logging to splunk
     */
    @Test
    public void canSendEventUsingJavaLoggingWithOptions() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "splunkLoggerWithOptions";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_index", "main");
        userInputs.put("user_source", "splunktest");
        userInputs.put("user_sourcetype", "battlecat");
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging canSendEventUsingJavaLoggingWithOptions}", date.toString());

        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk(jsonMsg);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * sending batched message via http java.logging to splunk
     */
    @Test
    public void sendBatchedEventsUsingJavaLogging() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "splunkBatchLogger";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_interval", "0");
        userInputs.put("user_batch_size_bytes", "0");
        userInputs.put("user_batch_size_count", "0");
        userInputs.put("user_logger_name", loggerName);

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging sendBatchedEventsUsingJavaLogging}", date.toString());

        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk(jsonMsg);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * sending batched message using java.logging with batched_size_count
     */
    @Test
    public void sendBatchedEventsByCount() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        //clean out the events cache by setting send events immediately
        String loggerName = "splunkLoggerCountCleanCache";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_logger_name", loggerName);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging sendBatchedEventsByCount}", new Date().toString());
        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        loggerName = "splunkBatchLoggerCount";
        userInputs.clear();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_interval", "0");
        userInputs.put("user_batch_size_count", "5");
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest_BatchCount");
        userInputs.put("user_sourcetype", "battlecat_BatchCount");

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        logger = Logger.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging1}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 1");
        TestUtil.verifyNoEventSentToSplunk(msgs);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging2}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 2");
        TestUtil.verifyNoEventSentToSplunk(msgs);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging3}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 3");
        TestUtil.verifyNoEventSentToSplunk(msgs);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging4}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        System.out.println("event 4");
        TestUtil.verifyNoEventSentToSplunk(msgs);

        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging5}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }


    /**
     * sending batched message using java.logging with batched_size_bytes
     */
    @Test
    public void sendBatchedEventsByBatchsize() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "splunkBatchLoggerSize";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_interval", "0");
        userInputs.put("user_batch_size_bytes", "500");
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest_BatchSize");
        userInputs.put("user_sourcetype", "battlecat_BatchSize");

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        Logger logger = Logger.getLogger(loggerName);

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
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "errorHandlingInvalidToken";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        Logger logger = Logger.getLogger(loggerName);

        //disable the token so that it becomes invalid
        TestUtil.disableHttpEventCollector(httpEventCollectorName);
        Thread.sleep(5000);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event disabled token }", new Date().toString());
        logger.info(jsonMsg);
        Thread.sleep(2000);

        //delete the token so that it becomes invalid
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        Thread.sleep(5000);
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
        Assert.assertEquals("Invalid token", logEx.get(1).getErrorText());
        Assert.assertEquals(4, logEx.get(1).getErrorCode());


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
                    logEx.add((HttpEventCollectorErrorHandler.ServerErrorException) ex);
                }
            }
        });

        //create a token used for httpEventCollector logging, then make it becomes invalid
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "disabledendpoint";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        Logger logger = Logger.getLogger(loggerName);

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

        // Enable httpEventCollector endpoint
        TestUtil.enableHttpEventCollector();

        if (logEx == null)
            Assert.fail("didn't catch errors");
        Assert.assertEquals(1, errors.size());

        System.out.println(logEx.toString());
        if (!StringUtils.containsAny(logEx.toString(), "Failed to connect to", "Remote host terminated the handshake", "Connection reset"))
            Assert.fail(String.format("Unexpected error message '%s'", logEx.toString()));
    }


    /**
     * sending batched message using java.logging with batched_size_count
     */
    @Test
    public void EventsFlushedAfterCloseLogger() throws Exception {
        String msgs = queueEvents();
//        Thread.sleep(9000);
//        com.splunk.logging.TestUtil.verifyOneAndOnlyOneEventSentToSplunk(msgs);

    }

    private String queueEvents() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        //clean out the events cache by setting send events immediately
        String loggerName = "splunkLoggerqueueEventsCleanCache";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_logger_name", loggerName);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging}", new Date().toString());
        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        loggerName = "splunkBatchLoggerCount";
        userInputs.clear();
        userInputs.put("user_httpEventCollector_token", token);
        //userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count", "5");
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest_BatchCount");
        userInputs.put("user_sourcetype", "battlecat_BatchCount");

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        logger = Logger.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging1}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        return jsonMsg;
    }


    /**
     * verify events are index in correct order of the events were sent
     */
    @Test
    public void eventsIsIndexedInOrderOfSent() throws Exception {
        TestUtil.enableHttpEventCollector();
        String indexName = "httpevents_in_order_jl";
        TestUtil.createIndex(indexName);
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "splunkLoggerMultipleEvents";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_index", indexName);
        userInputs.put("user_send_mode", "sequential");
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);

        Logger logger = Logger.getLogger(loggerName);

        //send multiple events and verify they are indexed in the order of sending
        List<String> msgs = new ArrayList<String>();
        Date date = new Date();
        int totalEventsCount = 1000;
        String prefix = "javalogging multiple events";
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
     * Test sending a JSON and text message with "_json" source type via http logging appender using java util logger
     */
    @Test
    public void canSendJsonEventUsingUtilLoggerWithJsonSourceType() throws Exception {
        canSendJsonEventUsingUtilLoggerWithSourceType("_json");
    }

    /**
     * Test sending a JSON and text message with "battlecat_test" source type via http logging appender using java util logger
     */
    @Test
    public void canSendJsonEventUsingUtilLoggerWithDefaultSourceType() throws Exception {
        canSendJsonEventUsingUtilLoggerWithSourceType("battlecat_test");
    }

    /**
     * sending a message using user-defined EventBodySerializer through java.logging to splunk
     */
    @Test
    public void canSendEventUsingJavaLoggingWithUserEventBodySerializer() throws Exception {
        TestUtil.enableHttpEventCollector();

        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "splunkLoggerBodySerializer";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_eventBodySerializer", "com.splunk.logging.TestEventBodySerializer");

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);

        Date date = new Date();
        String jsonMsg = String.format("EventDate:%s, EventMsg:test event for java logging With User EventBodySerializer", date.toString());

        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk("user-prefix:" + jsonMsg);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    /**
     * sending a message using user-defined EventHeaderSerializer through java.logging to splunk
     */
    @Test
    public void canSendEventUsingJavaLoggingWithUserEventHeaderSerializer() throws Exception {
        TestUtil.enableHttpEventCollector();

        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        String loggerName = "splunkLoggerHeaderSerializer";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_eventHeaderSerializer", "com.splunk.logging.TestEventHeaderSerializer");

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);

        Date date = new Date();
        String jsonMsg = String.format("EventDate:%s, EventMsg:test event for java logging With User EventHeaderSerializer", date.toString());

        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk("source=user_source");

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }

    @SuppressWarnings("unchecked")
    private void canSendJsonEventUsingUtilLoggerWithSourceType(final String sourceType) throws Exception {
        final String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        final String loggerName = "javaUtilLogger" + sourceType;
        // Build User input map
        final HashMap<String, String> userInputs = TestUtil.buildUserInputMap(loggerName, token, sourceType, "json");

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);

        final List<String> msgs = new ArrayList<String>();

        final long timeMillsec = new Date().getTime();

        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("transactionId", new JsonPrimitive("11"));
        jsonObject.add("userId", new JsonPrimitive("21"));
        jsonObject.add("eventTimestamp", new JsonPrimitive(timeMillsec));

        final Logger logger = Logger.getLogger(loggerName);

        // Test with a json event message
        jsonObject.add("severity", new JsonPrimitive("info"));
        final String infoJson = jsonObject.toString();
        logger.info(infoJson);
        msgs.add(infoJson);

        // Test with a text event message
        jsonObject.add("severity", new JsonPrimitive("info"));
        final String infoText = String.format("{EventTimestamp:%s, EventMsg:'this is a text info for java util logger}", timeMillsec);
        logger.info(infoText);
        msgs.add(infoText);

        TestUtil.verifyEventsSentToSplunk(msgs);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }
}
