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

import java.io.*;
import java.util.*;

import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorEventInfo;

import org.apache.logging.log4j.core.LoggerContext;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.apache.logging.log4j.Logger;

public final class HttpEventCollector_Log4j2Test {
    private String httpEventCollectorName = "Log4j2Test";
    List<List<HttpEventCollectorEventInfo>> errors = new ArrayList<List<HttpEventCollectorEventInfo>>();
    List<HttpEventCollectorErrorHandler.ServerErrorException> logEx = new ArrayList<HttpEventCollectorErrorHandler.ServerErrorException>();

    /**
     * sending a message via httplogging using log4j2 to splunk
     */
    @Test
    public void canSendEventUsingLog4j2() throws Exception, IOException, InterruptedException {
        TestUtil.enableHttpEventCollector();
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "splunkLogger4j2";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        org.apache.logging.log4j.core.LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        //use httplogger
        List<String> msgs = new ArrayList<String>();

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for log4j2}", date.toString());

        Logger logger = context.getLogger(loggerName);
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test error for log4j2}", date.toString());
        logger.error(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        System.out.println("====================== Test pass=========================");
    }


    /**
     * sending a message via httplogging using log4j2 to splunk and set index, source and sourcetype
     */
    @Test
    public void canSendEventUsingLog4j2WithOptions() throws Exception, IOException, InterruptedException {

        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "splunkLogger4j2WithOptions";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_index", "main");
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest");
        userInputs.put("user_sourcetype", "battlecat");

        org.apache.logging.log4j.core.LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        //use httplogger
        List<String> msgs = new ArrayList<String>();

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for log4j2}", date.toString());

        Logger logger = context.getLogger(loggerName);
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test error for log4j2}", date.toString());
        logger.error(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        System.out.println("====================== Test pass=========================");
    }

    /**
     * sending a message via httplogging using java.logging with batched_size_count
     */
    @Test
    public void sendBatchedEventsByCount() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        //clean out the events cache by setting send events immediately
        String loggerName = "splunkLoggerCountCleanCache";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging}", new Date().toString());
        Logger logger = context.getLogger(loggerName);
        logger.info(jsonMsg);

        loggerName = "splunkBatchLoggerCount";
        userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count", "5");
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest_BatchCount");
        userInputs.put("user_sourcetype", "battlecat_BatchCount");

        context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        logger = context.getLogger(loggerName);

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
     * sending a message via httplogging using java.logging with batched_size_bytes
     */
    @Test
    public void sendBatchedEventsByBatchsize() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "splunkLoggerBatchSize";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_size_bytes", "500");
        userInputs.put("user_batch_interval", "20000");
        userInputs.put("user_host", "host.example.com");
        userInputs.put("user_source", "splunktest_BatchSize");
        userInputs.put("user_sourcetype", "battlecat_BatchSize");

        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        Logger logger = context.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();
        int size = 0;
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for log4j size 1}", new Date().toString());
        logger.info(jsonMsg);
        size += jsonMsg.length();
        msgs.add(jsonMsg);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for log4j size 2}", new Date().toString());
        size += jsonMsg.length();
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for log4j size 3, adding more msg to exceed the maxsize}", new Date().toString());
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
        String loggerName = "errorHandlingInvalidTokenLog4j";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        Logger logger = context.getLogger(loggerName);

        //disable the token so that it becomes invalid
        TestUtil.disableHttpEventCollector(httpEventCollectorName);
        Thread.sleep(5000);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event disabled token }", new Date().toString());
        logger.info(jsonMsg);
        Thread.sleep(5000);

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

        for (List<HttpEventCollectorEventInfo> infos : errors) {
            for (HttpEventCollectorEventInfo info : infos) {
                System.out.println(info.getMessage());
            }
        }

        System.out.println("======print logEx");
        System.out.println(logEx.toString());
        System.out.println("======finish print logEx");
        Assert.assertEquals("Invalid token", logEx.get(1).getErrorText());
        Assert.assertEquals(4, logEx.get(1).getErrorCode());


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
        httpEventCollectorName = "wrongtoken";
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "errorHandlingDisabledHttpEventCollectorEndpoint";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        Logger logger = context.getLogger(loggerName);


        //disable httpEventCollector endpoint
        TestUtil.disableHttpEventCollector();
        Thread.sleep(1000);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event httpEventCollector disabled}", new Date().toString());
        logger.info(jsonMsg);

        //wait for async process to return the error
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 60 * 1000)/*wait for up to 60s*/ {
            if (logEx.size() >= 1)
                break;
            Thread.sleep(1000);
        }

        if (logEx == null)
            Assert.fail("didn't catch errors");
        Assert.assertTrue(errors.size() >= 1);

        System.out.println(logEx.toString());
        if (!(logEx.toString().contains("Connection refused") || logEx.toString().contains("Connection closed")))
            Assert.fail(String.format("Unexpected error message '%s'", logEx.toString()));
    }

    /**
     * verify events are index in correct order of the events were sent
     */
    @Test
    public void eventsIsIndexedInOrderOfSent() throws Exception {
        TestUtil.enableHttpEventCollector();

        String indexName="httpevents_in_order_l42";
        TestUtil.createIndex(indexName);
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "splunkLogger4j2OrderOfSent";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_index", indexName);
        userInputs.put("user_send_mode", "sequential");

        org.apache.logging.log4j.core.LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);

        //send multiple events and verify they are indexed in the order of sending
        List<String> msgs = new ArrayList<String>();
        Date date = new Date();
        int totalEventsCount = 1000;
        Logger logger = context.getLogger(loggerName);
        String prefix="log4j2 multiple events";
        for (int i = 0; i < totalEventsCount; i++) {
            String jsonMsg = String.format("%s %s", prefix,i);
            logger.info(jsonMsg);
            msgs.add(jsonMsg);
        }

        TestUtil.verifyEventsSentInOrder(prefix,totalEventsCount,indexName);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        System.out.println("====================== Test pass=========================");
    }
    
    /**
     * Test sending a JSON and text message with "_json" source type via http logging appender using log4j 2.x logger
     */
    @Test
    public void canSendJsonEventUsingUtilLoggerWithJsonSourceType() throws Exception {
        canSendJsonEventUsingUtilLoggerWithSourceType("_json");
    }
    
    /**
     * Test sending a JSON and text message with "battlecat_test" source type via http logging appender using log4j 2.x logger
     */
    @Test
    public void canSendJsonEventUsingUtilLoggerWithDefaultSourceType() throws Exception {
        canSendJsonEventUsingUtilLoggerWithSourceType("battlecat_test");
    }
    
    @SuppressWarnings("unchecked")
    private void canSendJsonEventUsingUtilLoggerWithSourceType(final String sourceType) throws Exception {
        final String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);

        final String loggerName = "splunkLog4j2";
        
        // Build User input map
        final HashMap<String, String> userInputs = TestUtil.buildUserInputMap(loggerName, token, sourceType, "json");
        
        final LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        
        final Logger logger = context.getLogger(loggerName);

        final List<String> msgs = new ArrayList<String>();

        final long timeMillsec = new Date().getTime();

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("transactionId", "11");
        jsonObject.put("userId", "21");
        jsonObject.put("eventTimestap", timeMillsec);

        // Test with a json event message
        jsonObject.put("severity", "info");
        final String infoJson = jsonObject.toString();
        logger.info(infoJson);
        msgs.add(infoJson);


        // Test with a text event message
        final String infoText = String.format("{EventTimestamp:%s, EventMsg:'this is a text info for log4j2 logger}", timeMillsec);
        logger.info(infoText);
        msgs.add(infoText);

        TestUtil.verifyEventsSentToSplunk(msgs);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
    }
}
