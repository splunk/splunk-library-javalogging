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
        TestUtil.updateConfigFile("logback_template.xml", "logback.xml", token);

        List<String> msgs = new ArrayList<String>();

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for Logback Test}", date.toString());
        Logger logger = LoggerFactory.getLogger("splunkLogback");
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
}
