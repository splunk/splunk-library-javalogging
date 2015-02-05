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
import java.util.logging.Logger;
import com.splunk.Event;
import com.splunk.ResultsReaderXml;
import com.splunk.Service;
import com.splunk.ServiceArgs;

public class HttpLoggerTest {

    private ServiceArgs serviceArgs;
    private String httpinputName = "newhttpinput";

    /**
     *   read splunk host info from .splunkrc file
     */
    private void getSplunkHostInfo()throws IOException {
        String splunkhostfile = System.getProperty("user.home") + File.separator + ".splunkrc";
        serviceArgs = new ServiceArgs();

        serviceArgs.setUsername("admin");
        serviceArgs.setPassword("changeme");
        serviceArgs.setHost("localhost");
        serviceArgs.setPort(8089);
        serviceArgs.setScheme("https");

        List<String> lines = Files.readAllLines(new File(splunkhostfile).toPath(), Charset.defaultCharset());

        for (String line : lines) {
            if (line.toLowerCase().contains("host")) {
                serviceArgs.setHost(line.split("=")[1]);
            }
            if (line.toLowerCase().contains("admin")) {
                serviceArgs.setUsername(line.split("=")[1]);
            }
            if (line.toLowerCase().contains("password")) {
                serviceArgs.setPassword(line.split("=")[1]);
            }
            if (line.toLowerCase().contains("scheme")) {
                serviceArgs.setScheme(line.split("=")[1]);
            }
            if (line.toLowerCase().contains("port")) {
                serviceArgs.setPort(Integer.parseInt(line.split("=")[1]));
            }
        }
    }

    /**
     *   modify the config file with the generated token, and configured splunk host
     */
    private void updateConfigFile(String token) throws  IOException{
        String configFileDir = HttpLoggerTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String configFilePath = new File(configFileDir, "log4j2.xml").getPath();
        List<String> lines = Files.readAllLines(new File(configFileDir, "log4j2.xml").toPath(), Charset.defaultCharset());
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%user_defined_httpinput_token%")) {
                lines.set(i, lines.get(i).replace("%user_defined_httpinput_token%", token));
            }

            if (lines.get(i).contains("%host:port%")) {
                lines.set(i, lines.get(i).replace("%host:port%", String.format("%s:%d", this.serviceArgs.host, this.serviceArgs.port)));
            }

            if (lines.get(i).contains("%scheme%")) {
                lines.set(i, lines.get(i).replace("%scheme%", this.serviceArgs.scheme));
            }
        }

        FileWriter fw = new FileWriter(configFilePath);
        for (String line : lines) {
            fw.write(line);
        }

        fw.flush();
        fw.close();
    }

    private void setup() throws IOException{
        this.getSplunkHostInfo();

        //connect to splunk server
        Service service = Service.connect(serviceArgs);
        service.login();

        //enable logging endpoint
        Map args = new HashMap();
        args.put("disabled", 0);
        service.post("/servicesNS/admin/search/data/inputs/http/http", args);

        //create a httpinput
        args = new HashMap();
        args.put("name", httpinputName);
        args.put("description", "test http input");

        try {
            service.delete("/services/data/inputs/http/" + httpinputName);
        } catch (Exception e){}

        service.post("/services/data/inputs/http", args);

        //get httpinput token
        args = new HashMap();
        ResponseMessage response = service.get("/services/data/inputs/http/" + httpinputName, args);
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

<<<<<<< HEAD
<<<<<<< HEAD
        //user httplogger
        Logger logger = Logger.getLogger("splunk.httplogger");
        logger.info("use httplogger");
        logger.info("new use of httplogger");

        //now should be able to read the new info from splunk
        splunkSearchstr="search httplogger*";
        resultsStream = service.oneshotSearch(splunkSearchstr);
        resultsReader = new ResultsReaderXml(resultsStream);
=======
        //modify the config file with the generated token
        String configFileDir = HttpLoggerTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String configFilePath = new File(configFileDir, "log4j2.xml").getPath();
        List<String> lines = Files.readAllLines(new File(configFileDir, "log4j2.xml").toPath(), Charset.defaultCharset());
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%user_defined_httpinput_token%")) {
                lines.set(i, lines.get(i).replace("%user_defined_httpinput_token%", token));
                break;
            }
        }
>>>>>>> Add test to automatically update configure file using generated httpinput token

        FileWriter fw = new FileWriter(configFilePath);
        for (String line : lines) {
            fw.write(line);
        }
        fw.flush();
        fw.close();
=======
        this.updateConfigFile(token);
>>>>>>> read splunk host from .splunkrc; update the log4j2.xml
    }

    private void teardown() throws IOException {
        Service service = Service.connect(serviceArgs);
        service.login();

        //remove a httpinput
        try {
            service.delete("/services/data/inputs/http/" + httpinputName);
        } catch (Exception e){}
    }

    /**
     * sending a message via httplogging to splunk
     */
    @Test
    public void httpAppenderTest() throws Exception, IOException, InterruptedException {
        this.setup();

        //use httplogger
        Date date = new Date();
        String jsonMsg = String.format("{EventDate:%s, EventMsg:'this is a test event", date.toString());
        Logger logger = LogManager.getLogger("splunkHttpLogger");
        logger.info(jsonMsg);

        //verify the event is send to splunk and can be searched
        Service service = Service.connect(serviceArgs);
        service.login();
        long startTime = System.currentTimeMillis();
        int eventCount = 0;

        InputStream resultsStream=null;
        ResultsReaderXml resultsReader=null;
        while (System.currentTimeMillis()-startTime < 30 * 1000)/*wait for up to 30s*/ {
            resultsStream = service.oneshotSearch("search " + jsonMsg);
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
        this.teardown();
        System.out.println("====================== Test pass=========================");
    }
}
