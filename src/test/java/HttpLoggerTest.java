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
import org.junit.Assert;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
//import java.util.logging.Logger;
import com.splunk.Event;
import com.splunk.ResultsReaderXml;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import com.splunk.logging.HttpAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class HttpLoggerTest {

    /**
     * Try writing a message via TCP to java.util.logging to validate the example configuration.
     */
    @Test
    public void httpAppenderTest() throws IOException,InterruptedException {
        //user httplogger
        //Logger logger = Logger.getLogger("splunkHttpLogger");
        Logger logger=LogManager.getLogger("splunkHttpLogger");

        logger.info("use httplogger");
        logger.info("new use of httplogger");

        //connect to localhost
        ServiceArgs serviceArgs = new ServiceArgs();
        serviceArgs.setUsername("admin");
        serviceArgs.setPassword("changeme");
        serviceArgs.setHost("localhost");
        serviceArgs.setPort(8089);
        Service service = Service.connect(serviceArgs);
        service.login();

        //do simple search and read result
        String splunkSearchstr="search index=_internal | head 3";
        InputStream resultsStream = service.oneshotSearch(splunkSearchstr);
        ResultsReaderXml resultsReader = new ResultsReaderXml(resultsStream);

        for (Event event : resultsReader) {
            System.out.println("---------------");
            System.out.println(event.getSegmentedRaw());
        }

        //now should be able to read the new info from splunk
        splunkSearchstr="search httplogger*";
        resultsStream = service.oneshotSearch(splunkSearchstr);
        resultsReader = new ResultsReaderXml(resultsStream);

        System.out.println("----- search httplogger result:----------");
        for (Event event : resultsReader) {
            System.out.println(event.getSegmentedRaw());
        }

        System.out.println("====================== Test pass=========================");
    }
}
