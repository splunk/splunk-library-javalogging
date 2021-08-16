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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class Log4jFunctionalTest {

    /**
     * Try writing a message via TCP to log4j 2 to validate the example configuration.
     */
    @Test
    public void log4j2SocketAppenderTest() throws InterruptedException {
        final Util.StringContainer container = Util.readLineFromPort(Util.port, Util.timeoutInMs);

        String helloChina = "Hello, \u4E2D\u570B!";

        Logger logger = LogManager.getLogger("splunk.logger");
        logger.info(helloChina);

        synchronized (container) {
            container.wait(Util.timeoutInMs);
        }

        Assert.assertNotNull(container.value);
        Assert.assertEquals("INFO: " + helloChina, container.value);
    }

}
