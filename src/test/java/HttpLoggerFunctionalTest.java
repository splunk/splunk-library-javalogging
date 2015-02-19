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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.lang.reflect.*;

import com.splunk.logging.*;
import com.splunk.*;

public class HttpLoggerFunctionalTest {
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
        serviceArgs = new ServiceArgs();
        serviceArgs.setUsername("admin");
        serviceArgs.setPassword("changeme");
        serviceArgs.setHost("127.0.0.1");
        serviceArgs.setPort(8089);
        serviceArgs.setScheme("https");
        Service service = Service.connect(serviceArgs);
        service.login();

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
    private static String httpinputName = "functionalhttp";

    private static void setupHttpInput(boolean batching) throws Exception {
        //connect to localhost
        serviceArgs = new ServiceArgs();
        serviceArgs.setUsername("admin");
        serviceArgs.setPassword("changeme");
        serviceArgs.setHost("127.0.0.1");
        serviceArgs.setPort(8089);
        serviceArgs.setScheme("https");
        Service service = Service.connect(serviceArgs);
        service.login();

        //enable logging endpoint
        Map args = new HashMap();
        args.put("disabled", 0);
        service.post("/servicesNS/admin/search/data/inputs/token/http/http", args);

        //create a httpinput
        args = new HashMap();
        args.put("name", httpinputName);
        args.put("description", "test http input");

        try {
            service.delete("/services/data/inputs/token/http/" + httpinputName);
        } catch (Exception e) {
        }

        service.post("/services/data/inputs/token/http", args);

        args = new HashMap();
        ResponseMessage response = service.get("/services/data/inputs/token/http/" + httpinputName, args);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getContent(), "UTF-8"));
        String token = "";
        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            if (line.contains("name=\"token\"")) {
                token = line.split(">")[1];
                token = token.split("<")[0];
                break;
            }
        }
        reader.close();

        //modify the config file with the generated token
        String configFileDir = HttpLoggerStressTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        CreateLog4j2ConfigFile(token, new File(configFileDir, "log4j2.xml").getPath(), batching);
        CreateLogBackConfigFile(token, new File(configFileDir, "logback.xml").getPath(), batching);
        CreateJavaUtilLog(token, batching);
    }
    private static void CreateJavaUtilLog(String token, boolean batching) throws Exception {
        String LoggerConf =
                "handlers=com.splunk.logging.HttpInputHandler\n" +
                "com.splunk.logging.HttpInputHandler.url=https://127.0.0.1:8089/services/receivers/token\n" +
                String.format("com.splunk.logging.HttpInputHandler.token=%s\n", token) +
                "com.splunk.logging.HttpInputHandler.disableCertificateValidation=true\n";
        if(batching) {
            LoggerConf +=
                    "batch_interval=\"1000\"\n" +
                    "batch_size_count=\"500\"\n" +
                    "batch_size_bytes=\"512\"\n";
        }
        try {
            java.util.logging.LogManager.getLogManager().readConfiguration(
                    new ByteArrayInputStream(LoggerConf.getBytes())
            );
        } catch (IOException e) {
            System.out.print(e);
            e.printStackTrace();
            Assert.fail();
        }

    }
    private static void CreateLogBackConfigFile(String token, String configFilePath, boolean batching) throws Exception {
        FileWriter fw = new FileWriter(configFilePath);
        fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        fw.write("<configuration>\r\n");
        fw.write("    <appender name=\"http\" class=\"com.splunk.logging.HttpLogbackAppender\">\r\n");
        fw.write("        <url>https://127.0.0.1:8089/services/receivers/token</url>\r\n");
        fw.write(String.format("        <token>%s</token>\r\n", token));
        fw.write("        <disableCertificateValidation>true</disableCertificateValidation>\r\n");
        if(batching){
            fw.write("        <batch_interval>1000</batch_interval>\r\n");
            fw.write("        <batch_size_count>500</batch_size_count>\r\n");
            fw.write("        <batch_size_bytes>512</batch_size_bytes>\r\n");
        }
        fw.write("        <source>splunktest</source>\r\n");
        fw.write("        <sourcetype>battlecat</sourcetype>\r\n");
        fw.write("        <layout class=\"ch.qos.logback.classic.PatternLayout\">\r\n");
        fw.write("            <pattern>%msg</pattern>\r\n");
        fw.write("        </layout>\r\n");
        fw.write("    </appender>\r\n");
        fw.write("\r\n");
        fw.write("    <logger name=\"splunk.logback\" additivity=\"false\" level=\"INFO\">\r\n");
        fw.write("        <appender-ref ref=\"http\"/>\r\n");
        fw.write("    </logger>\r\n");
        fw.write("\r\n");
        fw.write("    <root level=\"INFO\">\r\n");
        fw.write("        <appender-ref ref=\"http\"/>\r\n");
        fw.write("    </root>\r\n");
        fw.write("</configuration>\r\n");

        fw.flush();
        fw.close();
        addPath(configFilePath);

    }
    private static void CreateLog4j2ConfigFile(String token, String configFilePath, boolean batching) throws Exception {
        FileWriter fw = new FileWriter(configFilePath);
        fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        fw.write("<Configuration status=\"info\" name=\"example\" packages=\"com.splunk.logging\">\r\n");
        fw.write("    <!--need the \"token=...\" line to be a single line for parsing purpose-->\r\n");
        fw.write("    <Appenders>\r\n");
        fw.write("        <Http name=\"Http\"\r\n");
        fw.write("              url=\"https://127.0.0.1:8089/services/receivers/token\"\r\n");
        fw.write("              index=\"\"\r\n");
        fw.write(String.format("              token=\"%s\"\r\n", token));
        fw.write("              disableCertificateValidation=\"true\"\r\n");
        if(batching) {
            fw.write("              batch_interval=\"1000\"\r\n");
            fw.write("              batch_size_count=\"500\"\r\n");
            fw.write("              batch_size_bytes=\"512\"\r\n");
        }
        fw.write("              source=\"splunktest\" sourcetype=\"battlecat\">\r\n");
        fw.write("            <PatternLayout pattern=\"%m\"/>\r\n");
        fw.write("        </Http>\r\n");
        fw.write("    </Appenders>\r\n");
        fw.write("    <loggers>\r\n");
        fw.write("        <root level=\"debug\"/>\r\n");
        fw.write("        <logger name =\"splunk.log4j\" level=\"INFO\">\r\n");
        fw.write("            <appender-ref ref=\"Http\" />\r\n");
        fw.write("        </logger>\r\n");
        fw.write("    </loggers>\r\n");
        fw.write("</Configuration>");

        fw.flush();
        fw.close();
        addPath(configFilePath);
    }

    @Test
    public void LogToSplunkViaDifferentLoggers() throws Exception {
        LogToSplunk(false);
    }

    @Test
    public void BatchLogToSplunkViaDifferentLoggers() throws Exception {
        LogToSplunk(true);
    }

    private void LogToSplunk(boolean batching) throws  Exception {
        long startTime = System.currentTimeMillis()/1000;
        int expectedCounter = 10;
        Thread.sleep(2000);
        System.out.printf("\tSetting up http inputs ... ");
        setupHttpInput(batching);
        System.out.printf("Inserting data ... ");

        org.apache.logging.log4j.Logger LOG4J = org.apache.logging.log4j.LogManager.getLogger("splunk.log4j");
        for(int i=0; i <expectedCounter; i++) {
            LOG4J.info(String.format("log4j message%d", i));
        }

        org.slf4j.Logger LOGBACK = org.slf4j.LoggerFactory.getLogger("splunk.logback");
        for(int i=0; i <expectedCounter; i++) {
            LOGBACK.info(String.format("logback message%d", i));
        }

        java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
        for(int i=0; i <expectedCounter; i++) {
            LOGGER.info(String.format("javautil message%d", i));
        }
        System.out.printf("Done.\r\n");

        // Wait for indexing to complete
        if(batching)
            Thread.sleep(5000);
        int eventCount = getEventsCount(String.format("search earliest=%d| stats count", startTime));
        for(int i=0; i<4; i++) {
            do {
                System.out.printf("\tWaiting for indexing to complete, %d events so far\r\n", eventCount);
                Thread.sleep(5000);
                int updatedEventCount = getEventsCount(String.format("search earliest=%d| stats count", startTime));
                if (updatedEventCount == eventCount)
                    break;
                eventCount = updatedEventCount;
            } while (true);
            System.out.printf("\tCompleted wait for iteration %d\r\n", i);
        }
        Boolean testPassed = true;
        for (String type : new String [] {"log4j", "logback", "javautil"}) {
            String arguments = String.format("search %s earliest=%d| stats count", type, startTime);
            eventCount = getEventsCount(arguments);
            System.out.printf("Logger: '%s', expected %d events, actually %d, %s\r\n", type, expectedCounter, eventCount, (eventCount == expectedCounter)?"passed.":"failed.");
            testPassed &= (eventCount == expectedCounter);
        }
        System.out.printf("Test %s.\r\n", testPassed ? "PASSED" : "FAILED");
        Assert.assertTrue(testPassed);

    }
}
