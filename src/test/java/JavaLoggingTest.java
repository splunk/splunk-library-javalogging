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

import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Logger;
import java.util.logging.LogManager;

public final class JavaLoggingTest {

    private String httpinputName = "JavaLoggingTest";

    /**
     * sending a message via httplogging using log4j2 to splunk
     */
    @Test
    public void canSendEventUsingJavaLogging() throws Exception {
        String token = TestUtil.createHttpinput(httpinputName);
        TestUtil.updateConfigFile("logging_template.properties", "logging.properties", token);

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for java logging}", date.toString());
        FileInputStream configFile = new FileInputStream(TestUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "/logging.properties");
        LogManager.getLogManager().readConfiguration(configFile);

        Logger logger = Logger.getLogger("splunkLogger");
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSentToSplunk(jsonMsg);

        TestUtil.deleteHttpinput(httpinputName);
    }
}
