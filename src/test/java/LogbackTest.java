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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogbackTest {

    private String httpinputName = "LogbackTest";

    /**
     * sending a message via httplogging using logback to splunk
     */
    @Test
    public void canSendEventUsingLogback() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        String loggerName="logBackLogger";
        HashMap<String,String> userInputs=new HashMap<String,String>();
        userInputs.put("user_logger_name",loggerName);
        userInputs.put("user_httpinput_token",token);
        userInputs.put("user_defined_httpinput_token",token);
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
        TestUtil.deleteHttpinput(httpinputName);
    }

    /**
     * sending batched message using logback to splunk
     */
    @Test
    public void canSendBatchEventByCount() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        String loggerName="logBackBatchLoggerCount";
        HashMap<String,String> userInputs=new HashMap<String,String>();
        userInputs.put("user_httpinput_token",token);
        //userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count","5");
        //userInputs.put("user_batch_size_bytes","500000");
        userInputs.put("user_logger_name",loggerName);
        userInputs.put("user_source","splunktest_BatchSize");
        userInputs.put("user_sourcetype","battlecat_BatchSize");

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

        TestUtil.deleteHttpinput(httpinputName);

    }

    /**
     * sending batched message using logback to splunk
     */
    @Test
    public void canSendBatchEventBySize() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);

        String loggerName="logBackBatchLoggerSize";
        HashMap<String,String> userInputs=new HashMap<String,String>();
        userInputs.put("user_httpinput_token",token);
        userInputs.put("user_batch_interval","0");
        userInputs.put("user_batch_size_count","10000");
        userInputs.put("user_batch_size_bytes","500");
        userInputs.put("user_logger_name",loggerName);
        userInputs.put("user_source","splunktest_BatchSize");
        userInputs.put("user_sourcetype","battlecat_BatchSize");

        TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);
        Logger logger = LoggerFactory.getLogger(loggerName);

        List<String> msgs = new ArrayList<String>();

        String jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 1}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);
        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 2}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        Thread.sleep(6000);
        TestUtil.verifyNoEventSentToSplunk(msgs);

        jsonMsg = String.format("{EventDate:%s, EventMsg:'test event for java logging size 3, adding more msg to exceed the maxsize aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa}", new Date().toString());
        logger.info(jsonMsg);
        msgs.add(jsonMsg);

        TestUtil.verifyEventsSentToSplunk(msgs);

        TestUtil.deleteHttpinput(httpinputName);

    }
}
