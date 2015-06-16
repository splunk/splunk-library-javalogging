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

import com.splunk.logging.HttpInputLoggingErrorHandler;
import com.splunk.logging.HttpInputLoggingEventInfo;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Test;
import org.apache.logging.log4j.Logger;

public final class HttpInput_Log4j2Test {
    private String httpinputName = "Log4j2Test";
    List<List<HttpInputLoggingEventInfo>> errors = new ArrayList<List<HttpInputLoggingEventInfo>>();
    List<HttpInputLoggingErrorHandler.ServerErrorException> logEx = new ArrayList<HttpInputLoggingErrorHandler.ServerErrorException>();

    /**
     * sending a message via httplogging using log4j2 to splunk
     */
    @Test
    public void canSendEventUsingLog4j2() throws Exception, IOException, InterruptedException {
        TestUtil.enableHttpinput();
        String token = TestUtil.createHttpinput(httpinputName);
        String loggerName = "splunkLogger4j2";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpinput_token", token);
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

        TestUtil.deleteHttpinput(httpinputName);
        System.out.println("====================== Test pass=========================");
    }


    /**
     * sending a message via httplogging using log4j2 to splunk and set index, source and sourcetype
     */
    @Test
    public void canSendEventUsingLog4j2WithOptions() throws Exception, IOException, InterruptedException {

        String token = TestUtil.createHttpinput(httpinputName);
        String loggerName = "splunkLogger4j2";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpinput_token", token);
        userInputs.put("user_index", "main");
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

        TestUtil.deleteHttpinput(httpinputName);
        System.out.println("====================== Test pass=========================");
    }

    /**
     * sending a message via httplogging using java.logging with batched_size_count
     */
    @Test
    public void sendBatchedEventsByCount() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        //clean out the events cache by setting send events immediately
        String loggerName = "splunkLoggerCountCleanCache";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpinput_token", token);
        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging}", new Date().toString());
        Logger logger = context.getLogger(loggerName);
        logger.info(jsonMsg);

        loggerName = "splunkBatchLoggerCount";
        userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpinput_token", token);
        //userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count", "5");
        userInputs.put("user_logger_name", loggerName);
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

        TestUtil.deleteHttpinput(httpinputName);
    }


    /**
     * sending a message via httplogging using java.logging with batched_size_bytes
     */
    @Test
    public void sendBatchedEventsByBatchsize() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);
        String loggerName = "splunkLoggerBatchSize";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpinput_token", token);
        userInputs.put("user_batch_size_bytes", "500");
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
        TestUtil.deleteHttpinput(httpinputName);
    }

    /**
     * error handling
     */
    @Test
    public void errorHandlingInvalidToken() throws Exception {
        errors.clear();
        logEx.clear();
        //define error callback
        HttpInputLoggingErrorHandler.onError(new HttpInputLoggingErrorHandler.ErrorCallback() {
            public void error(final List<HttpInputLoggingEventInfo> data, final Exception ex) {
                synchronized (errors) {
                    errors.add(data);
                    logEx.add((HttpInputLoggingErrorHandler.ServerErrorException) ex);
                }
            }
        });

        //create a token used for httpinput logging, then make it becomes invalid
        httpinputName = "wrongtoken";
        String token = TestUtil.createHttpinput(httpinputName);
        String loggerName = "wrongToken";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpinput_token", token);
        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        Logger logger = context.getLogger(loggerName);

        //disable the token so that it becomes invalid
        TestUtil.disableHttpinput(httpinputName);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event disabled token }", new Date().toString());
        logger.info(jsonMsg);

        //delete the token so that it becomes invalid
        TestUtil.deleteHttpinput(httpinputName);
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
        System.out.println("======finsih print logEx");
        Assert.assertEquals("Invalid token", logEx.get(1).getErrorText());
        Assert.assertEquals(4, logEx.get(1).getErrorCode());


        for (List<HttpInputLoggingEventInfo> infos : errors) {
            for (HttpInputLoggingEventInfo info : infos) {
                System.out.println(info.getMessage());
            }
        }
        Assert.assertEquals(2, errors.size());
    }


    /**
     * error handling
     */
    @Test
    public void errorHandlingDisabledHttpinputEndpoint() throws Exception {
        errors.clear();
        logEx.clear();

        //define error callback
        HttpInputLoggingErrorHandler.onError(new HttpInputLoggingErrorHandler.ErrorCallback() {
            public void error(final List<HttpInputLoggingEventInfo> data, final Exception ex) {
                synchronized (errors) {
                    errors.add(data);
                    logEx.add((HttpInputLoggingErrorHandler.ServerErrorException) ex);
                }
            }
        });

        //create a token used for httpinput logging, then make it becomes invalid
        httpinputName = "wrongtoken";
        String token = TestUtil.createHttpinput(httpinputName);
        String loggerName = "wrongToken";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpinput_token", token);
        LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
        Logger logger = context.getLogger(loggerName);


        //disable httpinput endpoint
        TestUtil.disableHttpinput();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event httpinput disabled}", new Date().toString());
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

        System.out.println(logEx.toString());
        Assert.assertNotNull(logEx.get(0).getErrorCode());
        Assert.assertNotNull(logEx.get(0).getErrorText());

        for (List<HttpInputLoggingEventInfo> infos : errors) {
            for (HttpInputLoggingEventInfo info : infos) {
                System.out.println(info.getMessage());
            }
        }

        Assert.assertTrue(errors.size() >= 1);

    }
}
