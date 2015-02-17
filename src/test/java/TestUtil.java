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
import org.junit.Assert;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUtil {

    private static final ServiceArgs serviceArgs = new ServiceArgs();
    private static Service service;

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

    private static void connectToSplunk() throws IOException {

        if (service == null) {
            getSplunkHostInfo();

            //get splunk service and login
            service = Service.connect(serviceArgs);
            service.login();
        }
    }


    /**
     * create http input token
     */
    public static String createHttpinput(String httpinputName) throws IOException {
        connectToSplunk();

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

        //get httpinput token
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

        if (token.isEmpty()) {
            Assert.fail("no httpinput token is created");
        }

        return token;
    }

    /**
     * delete http input token
     */
    public static void deleteHttpinput(String httpinputName) throws IOException {
        connectToSplunk();

        //remove a httpinput
        try {
            service.delete("/services/data/inputs/http/" + httpinputName);
        } catch (Exception e) {
        }
    }

    /**
     * modify the config file with the generated token, and configured splunk host,
     * read the template from configFileTemplate, and create the updated configfile to configFile
     */
    public static void updateConfigFile(String configFileTemplate, String configFile, String token) throws IOException {
        getSplunkHostInfo();

        String configFileDir = TestUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        List<String> lines = Files.readAllLines(new File(configFileDir, configFileTemplate).toPath(), Charset.defaultCharset());
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%user_defined_httpinput_token%")) {
                lines.set(i, lines.get(i).replace("%user_defined_httpinput_token%", token));
            }

            if (lines.get(i).contains("%host%")) {
                lines.set(i, lines.get(i).replace("%host%", serviceArgs.host));
            }

            if (lines.get(i).contains("%port%")) {
                lines.set(i, lines.get(i).replace("%port%", serviceArgs.port.toString()));
            }

            if (lines.get(i).contains("%scheme%")) {
                lines.set(i, lines.get(i).replace("%scheme%", serviceArgs.scheme));
            }
        }

        String configFilePath = new File(configFileDir, configFile).getPath();
        FileWriter fw = new FileWriter(configFilePath);
        for (String line : lines) {
            fw.write(line);
            fw.write(System.getProperty("line.separator"));
        }

        fw.flush();
        fw.close();
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
    }
}
