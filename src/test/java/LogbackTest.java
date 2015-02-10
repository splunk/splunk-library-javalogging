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
import com.splunk.ServiceArgs;
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
        ServiceArgs serviceArgs = TestUtil.getSplunkHostInfo();
        String token = TestUtil.createHttpinput(serviceArgs, httpinputName);
        TestUtil.updateConfigFile("logback_template.xml", "logback.xml",serviceArgs, token);

        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event for Logback Test", date.toString());
        Logger logger=LoggerFactory.getLogger("splunkLogback");
        logger.info(jsonMsg);

        TestUtil.verifyOneAndOnlyOneEventSendToSplunk(serviceArgs,jsonMsg);
        TestUtil.deleteHttpinput(serviceArgs,httpinputName);
    }
}
