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

import com.splunk.*;
import java.io.*;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Log4j2Test {
    private String httpinputName = "Log4j2Test";

    /**
     * sending a message via httplogging using log4j2 to splunk
     */
    @Test
    public void canSendEventUsingLog4j2() throws Exception, IOException, InterruptedException {

        ServiceArgs serviceArgs = TestUtil.getSplunkHostInfo();
        String token = TestUtil.createHttpinput(serviceArgs, httpinputName);
        TestUtil.updateConfigFile("log4j2_template.xml","log4j2.xml", serviceArgs, token);

        //use httplogger
        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for log4j2", date.toString());
        Logger logger = LogManager.getLogger("splunkHttpLogger");
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSendToSplunk(serviceArgs, jsonMsg);
        TestUtil.deleteHttpinput(serviceArgs, httpinputName);
        System.out.println("====================== Test pass=========================");
    }
}
