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
        //userInputs.put("scheme", "http"); //uncomment this line if local splunk uses http vs https
        userInputs.put("user_middleware", "HttpEventCollectorMiddleware");
        
        org.apache.logging.log4j.core.LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        
        //use httplogger
        List<String> msgs = new ArrayList<String>();
        msgs.add(String.format("{EventDate:%s, EventMsg:'this is a test msg for log4j2'}", new Date().toString()));
        msgs.add(String.format("{EventDate:%s, EventMsg:'this is a test error for log4j2'}", new Date().toString()));
        TestUtil.deleteMessages(msgs);
        TestUtil.verifyNoEventSentToSplunk(msgs);
        
        Logger logger = context.getLogger(loggerName);
        logger.info(msgs.get(0));
        logger.error(msgs.get(1));

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        TestUtil.deleteMessages(msgs);
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
        userInputs.put("user_index", "default");
        userInputs.put("user_source", "splunktest");
        userInputs.put("user_sourcetype", "json_no_timestamp");
        //userInputs.put("scheme", "http"); //uncomment this line if local splunk uses http vs https
        userInputs.put("user_middleware", "HttpEventCollectorMiddleware");
        userInputs.put("user_batch_size_count", "1");
        userInputs.put("user_batch_size_bytes", "0");
        userInputs.put("user_patternLayout", "{EventDate:&quot;%d&quot;, Event:{EventSource:&quot;%C&quot;, EventMsg:&quot;%m&quot;}}");

        List<String> msgs = new ArrayList<String>();
        msgs.add("this is a test msg for log4j2");
        msgs.add("this is a test error for log4j2");
        TestUtil.deleteMessages(msgs);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        org.apache.logging.log4j.core.LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        //use httplogger

        Logger logger = context.getLogger(loggerName);
        logger.info(msgs.get(0));
        logger.error(msgs.get(1));

        TestUtil.verifyEventsSentToSplunkMatchingMsgPattern(msgs, ".+\\{EventDate:.+, Event:\\{EventSource:.+"+this.getClass().getSimpleName()+".+, EventMsg:.+this is a test (msg|error) for log4j2.+\\}\\}.+");
        

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        TestUtil.deleteMessages(msgs);
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
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test error for log4j2'}", new Date().toString());
        Logger logger = context.getLogger(loggerName);
        logger.info(jsonMsg);

        loggerName = "splunkBatchLoggerCount";
        userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        //userInputs.put("scheme", "http"); //uncomment this line if local splunk uses http vs https
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count", "5");
        userInputs.put("user_middleware", "HttpEventCollectorMiddleware");
        userInputs.put("user_patternLayout", "%m");


        context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        logger = context.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();
        for (int i=0; i < 4; i++){
        	jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test error for log4j2'}", new Date().toString());
            logger.info(jsonMsg);
            msgs.add(jsonMsg);
            System.out.println("event "+(i+1));
            TestUtil.verifyNoEventSentToSplunk(msgs);
        }
        
        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test error for log4j2'}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        TestUtil.deleteMessages(msgs);
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
        //userInputs.put("scheme", "http"); //uncomment this line if local splunk uses http vs https
        userInputs.put("user_batch_interval", "20000");
        userInputs.put("user_source", "splunktest_BatchSize");
        userInputs.put("user_sourcetype", "battlecat_BatchSize");
        userInputs.put("user_middleware", "HttpEventCollectorMiddleware");
        userInputs.put("user_patternLayout", "%m");

        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        Logger logger = context.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();
        int size = 0;
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for log4j size 1'}", new Date().toString());
        logger.info(jsonMsg);
        size += jsonMsg.length();
        msgs.add(jsonMsg);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for log4j size 2'}", new Date().toString());
        size += jsonMsg.length();
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for log4j size 3, adding more msg to exceed the maxsize'}", new Date().toString());
        while (size + jsonMsg.length() < 550) {
            jsonMsg = String.format("%saaaaa", jsonMsg);
        }

        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);
        TestUtil.deleteHttpEventCollectorToken(httpEventCollectorName);
        TestUtil.deleteMessages(msgs);
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
        //userInputs.put("scheme", "http"); //uncomment this line if local splunk uses http vs https
        userInputs.put("user_middleware", "HttpEventCollectorMiddleware");
        userInputs.put("user_batch_size_count", "1");
        userInputs.put("user_batch_size_bytes", "0");
        userInputs.put("user_patternLayout", "%m");
        
        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        Logger logger = context.getLogger(loggerName);

        //disable the token so that it becomes invalid
        TestUtil.disableHttpEventCollector(httpEventCollectorName);
        Thread.sleep(5000);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event disabled token'}", new Date().toString());
         
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
        if(!logEx.toString().contains("Connection refused"))
            Assert.fail(String.format("Unexpected error message '%s'", logEx.toString()));
    }

    /**
     * verify events are indexed in correct order of the events were sent
     */
    @Test
    public void eventsIndexedInOrderOfSent() throws Exception {
        TestUtil.enableHttpEventCollector();

        String indexName="httpevents_in_order";
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
        int totalEventsCount = 100;
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
}
