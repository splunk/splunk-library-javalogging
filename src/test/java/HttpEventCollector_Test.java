/**
 * @copyright
 *
 * Copyright 2013-2015 Splunk, Inc.
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

import ch.qos.logback.core.joran.spi.JoranException;
import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorEventInfo;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.lang.reflect.*;

import com.splunk.*;
import org.slf4j.*;

public class HttpEventCollector_Test {
    public static void addPath(String s) throws Exception {
        File f = new File(s);
        URI u = f.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> urlClass = URLClassLoader.class;
        Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(urlClassLoader, new Object[]{u.toURL()});
    }

    private static int getEventsCount(String searchQuery) throws IOException, InterruptedException {
        //connect to localhost
        Service service = TestUtil.connectToSplunk();

        // Check the syntax of the query.
        try {
            service.parse(searchQuery, new Args("parse_only", true));
        } catch (HttpException e) {
            System.out.printf("query '%s' is invalid: %s\n", searchQuery, e.getDetail());
            return -1;
        }
        Job job = service.getJobs().create(searchQuery, new Args());
        while (!job.isDone()) {
            Thread.sleep(1000);
            job.refresh();
        }
        Args outputArgs = new Args();
        outputArgs.put("count", 100);
        outputArgs.put("offset", 0);
        outputArgs.put("output_mode", "json");
        InputStream stream = job.getResults(outputArgs);
        ResultsReader resultsReader = new ResultsReaderJson(stream);
        int result = -1;
        try {
            HashMap<String, String> event = resultsReader.getNextEvent();
            if (event != null) {
                if (event.containsKey("count")) {
                    String value = event.get("count");
                    result = Integer.parseInt(value);
                }
            }
        } finally {
            resultsReader.close();
        }
        job.cancel();
        return result;
    }

    private static ServiceArgs serviceArgs;
    private static String httpEventCollectorName = "functionalhttp";

    private static String  setupHttpEventCollector(boolean batching) throws Exception {
        TestUtil.enableHttpEventCollector();
        String token=TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        return token;
    }

    @Test
    public void LogToSplunkViaDifferentLoggers() throws Exception {
        LogToSplunk(false);
    }

    @Test
    public void BatchLogToSplunkViaDifferentLoggers() throws Exception {
        LogToSplunk(true);
    }

    public  static volatile boolean exceptionWasRaised = false;
    @Test
    public void TryToLogToSplunkWithDisabledHttpEventCollector() throws Exception {
        HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
            public void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
                String exceptionInfo = ex.getMessage() + " " + ex.getStackTrace();
                HttpEventCollectorErrorHandler.ServerErrorException serverErrorException =
                        new HttpEventCollectorErrorHandler.ServerErrorException(exceptionInfo);
                System.out.printf("Callback has been called on error\n");
                exceptionWasRaised = true;
            }
        });
        int expectedCounter = 200;
        exceptionWasRaised = false;
        boolean batching = false;
        System.out.printf("\tSetting up http event collector with %s ... ", batching ? "batching" : "no batching");
        TestUtil.enableHttpEventCollector();
        String token=TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        System.out.printf("set\n");

        //modify the config file with the generated token
        String loggerName = "splunkLogger_disabled";
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        if (batching) {
            userInputs.put("user_batch_interval", "200");
            userInputs.put("user_batch_size_count", "500");
            userInputs.put("user_batch_size_bytes", "12");
        }
        userInputs.put("user_logger_name", loggerName);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        TestUtil.disableHttpEventCollector();
        Thread.sleep(5000);

        // HTTP event collector is disabled now, expect exception to be raised and reported
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(loggerName);
        for (int i = 0; i < expectedCounter; i++) {
            LOGGER.info(String.format("javautil message%d", i));
        }
        if (!exceptionWasRaised) {
            Thread.sleep(15000);
        }
        Assert.assertTrue(exceptionWasRaised);
        System.out.printf("PASSED with %d events sent.\n\n", expectedCounter);
    }

    private boolean insertDataWithLoggerAndVerify(String token, String loggerType, int expectedCounter, boolean batching) throws IOException, InterruptedException, JoranException {
        System.out.printf("\tInserting data with logger '%s'... ", loggerType);
        long startTime = System.currentTimeMillis() / 1000;
        Thread.sleep(2000);
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        if (batching) {
            userInputs.put("user_batch_interval", "200");
            userInputs.put("user_batch_size_count", "500");
            userInputs.put("user_batch_size_bytes", "12");
        }

        if (loggerType == "log4j") {
            String loggerName = "splunk.log4jInsertVerify";
            userInputs.put("user_logger_name", loggerName);
            org.apache.logging.log4j.core.LoggerContext context = TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
            org.apache.logging.log4j.Logger LOG4J = context.getLogger(loggerName);
            for (int i = 0; i < expectedCounter; i++) {
                LOG4J.info(String.format("log4j message%d", i));
            }
        }
        if (loggerType == "logback") {
            String loggerName = "logBackLogger";
            userInputs.put("user_logger_name", loggerName);
            userInputs.put("user_defined_httpEventCollector_token", token);
            TestUtil.resetLogbackConfiguration("logback_template.xml", "logback.xml", userInputs);
            org.slf4j.Logger LOGBACK = org.slf4j.LoggerFactory.getLogger(loggerName);
            for (int i = 0; i < expectedCounter; i++) {
                LOGBACK.info(String.format("logback message%d", i));
            }
        }
        if (loggerType == "javautil") {
            String loggerName = batching?"splunkLogger_batching":"splunkLogger_nobatching";
            userInputs.put("user_logger_name", loggerName);
            TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
            java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(loggerName);
            for (int i = 0; i < expectedCounter; i++) {
                LOGGER.info(String.format("javautil message%d", i));
            }
        }
        System.out.printf("Done\n");
        // Wait for indexing to complete
        Thread.sleep(10000);
        String searchQuery = String.format("search %s earliest=%d| stats count", loggerType, startTime);
        waitForIndexingToComplete(searchQuery, expectedCounter);
        Boolean testPassed = true;
        int eventCount = getEventsCount(searchQuery);
        System.out.printf("\tLogger: '%s', expected %d events, actually %d, %s\r\n", loggerType, expectedCounter, eventCount, (eventCount == expectedCounter) ? "Passed." : "Failed.");
        return (eventCount == expectedCounter);
    }

    private void LogToSplunk(boolean batching) throws Exception {
        HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
            public void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
                HttpEventCollectorErrorHandler.ServerErrorException serverErrorException =
                        (HttpEventCollectorErrorHandler.ServerErrorException) ex;
                System.out.printf("ERROR: %s", ex.toString());
                Assert.assertTrue(false);
            }
        });
        int expectedCounter = 2;
        System.out.printf("\tSetting up http event collector with %s ... ", batching ? "batching" : "no batching");
        String token = setupHttpEventCollector(batching);
        System.out.printf("Set\n");
        Boolean testPassed = true;
        testPassed &= insertDataWithLoggerAndVerify(token, "log4j", expectedCounter, batching);
        testPassed &= insertDataWithLoggerAndVerify(token, "logback", expectedCounter, batching);
        testPassed &= insertDataWithLoggerAndVerify(token, "javautil", expectedCounter, batching);
        Assert.assertTrue(testPassed);
        System.out.printf("PASSED.\n\n");
    }

    private void waitForIndexingToComplete(String query, int expectedCounter) throws IOException, InterruptedException {
        int eventCount = getEventsCount(query);
        System.out.printf("\tStarting wait on index. %d events expected with query '%s'.\r\n", expectedCounter, query);
        for (int i = 0; i < 12; i++) {
            if (eventCount == expectedCounter) {
                System.out.printf("\tDone waiting for indexing with %d events. All events were found.\r\n", eventCount);
                return;
            }
            System.out.printf("\tWaiting for indexing, %d events so far\r\n", eventCount);
            Thread.sleep(5000);
            eventCount = getEventsCount(query);
        }
        System.out.printf("\tFailed to wait to find %d events, %d were found instead.\r\n", expectedCounter, eventCount);
    }

    private static class DataSender implements Runnable {
        private String threadName;
        public int eventsGenerated = 0, testDurationInSecs = 300;
        java.util.logging.Logger logger;

        public DataSender(String threadName, int testDurationInSecs, java.util.logging.Logger logger) {
            this.threadName = threadName;
            this.testDurationInSecs = testDurationInSecs;
            this.logger = logger;
        }

        public void run() {
            Date dCurrent = new Date();
            Date dEnd = new Date();
            dEnd.setTime(dCurrent.getTime() + testDurationInSecs * 1000);
            while (dCurrent.before(dEnd)) {
                this.logger.info(String.format("javautil thread: %s, event: %d", this.threadName, eventsGenerated++));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dCurrent = new Date();
            }
        }
    }

    @Test
    public  void ResendDataToSplunk() throws  Exception
    {
        HttpEventCollectorErrorHandler.onError(new HttpEventCollectorErrorHandler.ErrorCallback() {
            public void error(final List<HttpEventCollectorEventInfo> data, final Exception ex) {
                HttpEventCollectorErrorHandler.ServerErrorException serverErrorException =
                        (HttpEventCollectorErrorHandler.ServerErrorException) ex;
                System.out.printf("ERROR: %s", ex.toString());
                Assert.assertTrue(false);
            }
        });
        boolean batching = false;
        System.out.printf("\tSetting up http event collector with %s ... ", batching ? "batching" : "no batching");
        String token = setupHttpEventCollector(batching);
        System.out.printf("HTTP event collector fully set\n");
        Service service = TestUtil.connectToSplunk();
        HashMap<String, String> userInputs = new HashMap<String, String>();
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_retries_on_error", "1000");

        String loggerName = "splunkLogger_resend";
        userInputs.put("user_logger_name", loggerName);
        TestUtil.resetJavaLoggingConfiguration("logging_template.properties", "logging.properties", userInputs);
        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(loggerName);

        // Start sending data and restart splunk in the middle. Expected to have no data loss
        long startTime = System.currentTimeMillis() / 1000;
        Thread.sleep(2000);
        DataSender ds = new DataSender("sendingData", 60, LOGGER);
        Thread tSend = new Thread(ds);
        tSend.start();
        Thread.sleep(10000);
        service.restart();
        tSend.join();
        TestUtil.resetConnection();
        int expectedCounter = ds.eventsGenerated;
        String searchQuery = String.format("search %s earliest=%d| stats count", "javautil", startTime);
        System.out.printf("\tWill search data using this query '%s'. Expected counter is %d\r\n", searchQuery, expectedCounter);
        waitForIndexingToComplete(searchQuery, expectedCounter);
        int eventCount = getEventsCount(searchQuery);
        System.out.printf("\tLogger: '%s', expected %d events, actually %d, %s\r\n", "javautil", expectedCounter, eventCount, (eventCount == expectedCounter) ? "Passed." : "Failed.");
        Assert.assertEquals(eventCount, expectedCounter);
        System.out.printf("PASSED.\n\n");
    }
}
