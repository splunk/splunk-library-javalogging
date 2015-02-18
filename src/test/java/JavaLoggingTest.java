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

import java.util.*;

import org.junit.Assert;
import org.junit.Test;
import java.util.logging.Logger;

public final class JavaLoggingTest {

    private String httpinputName = "JavaLoggingTest";

    /**
     * sending a message via httplogging using log4j2 to splunk
     */
    @Test
    public void canSendEventUsingJavaLogging() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        String loggerName="splunkLogger";
        HashMap<String,String> userInputs=new HashMap<String,String>();
        userInputs.put("user_httpinput_token",token);
        userInputs.put("user_logger_name",loggerName);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging}", date.toString());

        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk(jsonMsg);

        TestUtil.deleteHttpinput(httpinputName);
    }

    /**
     * sending batched message via httplogging to splunk
     */
    @Test
    public void sendBatchedEventsUsingJavaLogging() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        String loggerName="splunkBatchLogger";
        HashMap<String,String> userInputs=new HashMap<String,String>();
        userInputs.put("user_httpinput_token",token);
        userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_bytes","0");
        userInputs.put("user_batch_size_count","0");
        userInputs.put("user_logger_name",loggerName);

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging}", date.toString());

        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk(jsonMsg);

        TestUtil.deleteHttpinput(httpinputName);
    }

    /**
     * sending batched message using java.logging with batched_size_count
     */
    @Test
    public void sendBatchedEventsByCount() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        //clean out the events cache by setting send events immediately
        String loggerName="splunkLoggerCountCleanCache";
        HashMap<String,String> userInputs=new HashMap<String,String>();
        userInputs.put("user_httpinput_token",token);
        userInputs.put("user_logger_name",loggerName);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging}", new Date().toString());
        Logger logger = Logger.getLogger(loggerName);
        logger.info(jsonMsg);

        loggerName="splunkBatchLoggerCount";
        userInputs.clear();
        userInputs.put("user_httpinput_token",token);
        //userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count","5");
        userInputs.put("user_logger_name",loggerName);
        userInputs.put("user_source","splunktest_BatchCount");
        userInputs.put("user_sourcetype","battlecat_BatchCount");

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

        TestUtil.deleteHttpinput(httpinputName);
    }


    /**
     * sending batched message using java.logging with batched_size_bytes
     */
    @Test
    public void sendBatchedEventsByBatchsize() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        String loggerName="splunkBatchLoggerSize";
        HashMap<String,String> userInputs=new HashMap<String,String>();
        userInputs.put("user_httpinput_token",token);
        //userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_bytes","500");
        userInputs.put("user_logger_name",loggerName);
        userInputs.put("user_source","splunktest_BatchSize");
        userInputs.put("user_sourcetype","battlecat_BatchSize");

        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        Logger logger = Logger.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();

        int size=0;
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 1}", new Date().toString());
        size +=jsonMsg.length();
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 2}", new Date().toString());
        size +=jsonMsg.length();
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 3, adding more msg to exceed the maxsize}", new Date().toString());
        while(size+jsonMsg.length()<550){
            jsonMsg=String.format("%saaaaa",jsonMsg);
        }
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpinput(httpinputName);
    }

}
