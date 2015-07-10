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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.splunk.*;
import com.splunk.logging.HttpEventCollectorMiddleware;
import org.junit.Assert;
import org.slf4j.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestUtil {

    private static final ServiceArgs serviceArgs = new ServiceArgs();
    private static Service service;
    private static final String httpInputTokenEndpointPath = "/services/data/inputs/http";

    /**
     * read splunk host info from .splunkrc file
     */
    private static void getSplunkHostInfo() throws IOException {

        if (serviceArgs.isEmpty()) {
            //set default value
            serviceArgs.setUsername("admin");
            serviceArgs.setPassword("changeme");
            serviceArgs.setHost("localhost");
            serviceArgs.setPort(8089);
            serviceArgs.setScheme("https");

            //update serviceArgs with customer splunk host info
            String splunkhostfile = System.getProperty("user.home") + File.separator + ".splunkrc";
            List<String> lines = Files.readAllLines(new File(splunkhostfile).toPath(), Charset.defaultCharset());
            for (String line : lines) {
                if (line.toLowerCase().contains("host=")) {
                    serviceArgs.setHost(line.split("=")[1]);
                }
                if (line.toLowerCase().contains("admin=")) {
                    serviceArgs.setUsername(line.split("=")[1]);
                }
                if (line.toLowerCase().contains("password=")) {
                    serviceArgs.setPassword(line.split("=")[1]);
                }
                if (line.toLowerCase().contains("scheme=")) {
                    serviceArgs.setScheme(line.split("=")[1]);
                }
                if (line.toLowerCase().contains("port=")) {
                    serviceArgs.setPort(Integer.parseInt(line.split("=")[1]));
                }
            }
        }
    }

    public static void resetConnection() {
        service = null;
    }

    public static Service connectToSplunk() throws IOException {

        if (service == null) {
            getSplunkHostInfo();

            //get splunk service and login
            service = Service.connect(serviceArgs);
            service.login();
        }

        return service;
    }

    public static void createIndex(String indexName) throws Exception {
        connectToSplunk();
        IndexCollection indexes = service.getIndexes();
        if (indexes.containsKey(indexName)) {
            Index index = indexes.get(indexName);
            indexes.remove(indexName);
        }

        indexes.create(indexName);
    }


    /**
     * create http event collector token
     */
    public static String createHttpinput(String httpinputName) throws Exception {
        connectToSplunk();

        //enable logging endpoint
        TestUtil.enableHttpinput();

        //create a httpinput
        Map args = new HashMap();
        args.put("name", httpinputName);
        args.put("description", "test http event collector");

        deleteHttpinput(httpinputName);

        ResponseMessage msg = service.post(httpInputTokenEndpointPath, args);
        assert msg.getStatus() == 201;

        //get httpinput token
        args = new HashMap();
        ResponseMessage response = service.get(httpInputTokenEndpointPath + "/" + httpinputName, args);
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

        if (token.isEmpty()) {
            Assert.fail("no httpinput token is created");
        }

        return token;
    }

    /**
     * delete http event collector token
     */
    public static void deleteHttpinput(String httpinputName) throws Exception {
        connectToSplunk();
        try {
            ResponseMessage response = service.get(httpInputTokenEndpointPath + "/" + httpinputName);
            if (response.getStatus() == 200) {
                response = service.delete(httpInputTokenEndpointPath + "/" + httpinputName);
                assert response.getStatus() == 200;
            }
        } catch (com.splunk.HttpException e) {
            if (e.getStatus() != 404)
                throw e;
        }
    }

    /**
     * disable http event collector feature
     */
    public static void disableHttpinput() throws IOException {
        connectToSplunk();

        //disable logging endpoint
        Map args = new HashMap();
        args.put("disabled", 1);
        ResponseMessage response = service.post("/servicesNS/admin/search/data/inputs/http/http", args);
        assert response.getStatus() == 200;
    }

    /**
     * enable http event collector feature
     */
    public static void enableHttpinput() throws IOException {
        connectToSplunk();

        //enable logging endpoint
        Map args = new HashMap();
        args.put("disabled", 0);
        ResponseMessage response = service.post("/servicesNS/admin/search/data/inputs/http/http", args);
        assert response.getStatus() == 200;

    }

    /**
     * disable http event collector token
     */
    public static void disableHttpinput(String httpinputName) throws IOException {
        connectToSplunk();

        Map args = new HashMap();
        args.put("disabled", 1);

        ResponseMessage response = service.post(httpInputTokenEndpointPath + "/" + httpinputName, args);
        assert response.getStatus() == 200;
    }

    /**
     * modify the config file with the generated token, and configured splunk host,
     * read the template from configFileTemplate, and create the updated configfile to configFile
     */
    public static String updateConfigFile(String configFileTemplate, String configFile, HashMap<String, String> userInputs) throws IOException {
        getSplunkHostInfo();

        String configFileDir = TestUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        List<String> lines = Files.readAllLines(new File(configFileDir, configFileTemplate).toPath(), Charset.defaultCharset());
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%host%")) {
                lines.set(i, lines.get(i).replace("%host%", serviceArgs.host));
            }
            if (lines.get(i).contains("%port%")) {
                lines.set(i, lines.get(i).replace("%port%", serviceArgs.port.toString()));
            }

            if (lines.get(i).contains("%scheme%")) {
                lines.set(i, lines.get(i).replace("%scheme%", serviceArgs.scheme));
            }

            String match = FindUserInputConfiguration(lines.get(i));
            if (!match.isEmpty()) {
                if (userInputs.keySet().contains(match))
                    lines.set(i, lines.get(i).replace("%" + match + "%", userInputs.get(match)));
                else
                    lines.set(i, "");
            }
        }

        String configFilePath = new File(configFileDir, configFile).getPath();
        FileWriter fw = new FileWriter(configFilePath);
        for (String line : lines) {
            if (!line.isEmpty()) {
                fw.write(line);
                fw.write(System.getProperty("line.separator"));
            }
        }

        fw.flush();
        fw.close();

        return configFilePath;
    }

    private static String FindUserInputConfiguration(String line) {
        String pattern = "%.*%";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(line);
        if (m.find()) {
            return m.group(0).substring(1, m.group(0).length() - 1);
        } else
            return "";
    }

    /*
        create log4j2.xml and force log4j2 context manager to reload the configurations, return context and using this context to retrieve logger instead of using LogManager
    */
    public static org.apache.logging.log4j.core.LoggerContext resetLog4j2Configuration(String configFileTemplate, String configFile, HashMap<String, String> userInputs) throws IOException, JoranException {
        HttpEventCollectorMiddleware.setMiddleware(null);
        String configFilePath = updateConfigFile(configFileTemplate, configFile, userInputs);
        org.apache.logging.log4j.core.LoggerContext context = new org.apache.logging.log4j.core.LoggerContext("new");
        context.reconfigure();
        context.updateLoggers();
        return context;
    }

    /*
    create logback.xml and force logback manager to reload the configurations
     */
    public static void resetLogbackConfiguration(String configFileTemplate, String configFile, HashMap<String, String> userInputs) throws IOException, JoranException {
        String configFilePath = updateConfigFile(configFileTemplate, configFile, userInputs);

        //force the Logback factory to reload the configuration file
        JoranConfigurator jc = new JoranConfigurator();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        jc.setContext(context);
        context.reset();
        HttpEventCollectorMiddleware.setMiddleware(null);
        jc.doConfigure(configFilePath);
    }

    /*
    create logging.property and force java logging  manager to reload the configurations
    */
    public static void resetJavaLoggingConfiguration(String configFileTemplate, String configFile, HashMap<String, String> userInputs) throws IOException, JoranException {
        String configFilePath = updateConfigFile(configFileTemplate, configFile, userInputs);
        FileInputStream configFileStream = new FileInputStream(configFilePath);
        LogManager.getLogManager().readConfiguration(configFileStream);
        configFileStream.close();
    }

    public static void verifyOneAndOnlyOneEventSentToSplunk(String msg) throws IOException {
        connectToSplunk();

        long startTime = System.currentTimeMillis();
        int eventCount = 0;
        InputStream resultsStream = null;
        ResultsReaderXml resultsReader = null;
        while (System.currentTimeMillis() - startTime < 30 * 1000)/*wait for up to 30s*/ {
            resultsStream = service.oneshotSearch("search " + msg);
            resultsReader = new ResultsReaderXml(resultsStream);

            //verify has one and only one record return
            for (Event event : resultsReader) {
                eventCount++;
                System.out.println("---------------");
                System.out.println(event.getSegmentedRaw());
            }

            if (eventCount > 0)
                break;
        }

        resultsReader.close();
        resultsStream.close();

        Assert.assertTrue(eventCount == 1);
    }

    public static void verifyNoEventSentToSplunk(List<String> msgs) throws IOException {
        connectToSplunk();
        String searchstr = org.apache.commons.lang3.StringUtils.join(msgs, "\" OR \"");
        searchstr = "\"" + searchstr + "\"";

        long startTime = System.currentTimeMillis();
        int eventCount = 0;
        InputStream resultsStream = null;
        ResultsReaderXml resultsReader = null;
        while (System.currentTimeMillis() - startTime < 10 * 1000)/*wait for up to 30s*/ {
            resultsStream = service.oneshotSearch("search " + searchstr);
            resultsReader = new ResultsReaderXml(resultsStream);

            //verify has one and only one record return
            for (Event event : resultsReader) {
                eventCount++;
                System.out.println("------verify no events---------");
                System.out.println(event.getSegmentedRaw());
            }

            if (eventCount > 0)
                break;
        }

        resultsReader.close();
        resultsStream.close();

        Assert.assertTrue(eventCount == 0);
    }

    /*
    verify each of the message in msgs appeared and appeared only once in splunk
     */
    public static void verifyEventsSentToSplunk(List<String> msgs) throws IOException {
        connectToSplunk();

        for (String msg : msgs) {
            long startTime = System.currentTimeMillis();
            int eventCount = 0;
            InputStream resultsStream = null;
            ResultsReaderXml resultsReader = null;
            while (System.currentTimeMillis() - startTime < 30 * 1000)/*wait for up to 30s*/ {
                resultsStream = service.oneshotSearch("search " + msg);
                resultsReader = new ResultsReaderXml(resultsStream);

                //verify has one and only one record return
                for (Event event : resultsReader) {
                    eventCount++;
                    System.out.println("------verify has events---------");
                    System.out.println(event.getSegmentedRaw());
                }

                if (eventCount > 0)
                    break;
            }

            resultsReader.close();
            resultsStream.close();

            Assert.assertTrue(eventCount == 1);
        }
    }

    public static void verifyEventsSentInOrder(String prefix, int totalEventsCount, String index) throws IOException {
        connectToSplunk();

        long startTime = System.currentTimeMillis();
        InputStream resultsStream = null;
        ResultsReaderXml resultsReader = null;
        List<String> results = new ArrayList<String>();
        while (System.currentTimeMillis() - startTime < 100 * 1000)/*wait for up to 30s*/ {
            results.clear();
            String searchstr = "search index=" + index;
            resultsStream = service.oneshotSearch(searchstr, new Args("count", 0));
            resultsReader = new ResultsReaderXml(resultsStream);

            for (Event event : resultsReader) {
                results.add(event.getSegmentedRaw());
            }

            if (results.size() == totalEventsCount)
                break;
        }

        resultsReader.close();
        resultsStream.close();

        assert (results.size() == totalEventsCount) : String.format("expect: %d, actual: %d", totalEventsCount, results.size());

        //verify events record is in correct order
        for (int i = 0; i < totalEventsCount; i++) {
            String expect = String.format("%s %s", prefix, totalEventsCount - 1 - i);
            assert results.get(i).contains(expect) :
                    String.format("expect: %s, actual: %s", expect, results.get(i));
        }
    }
}
