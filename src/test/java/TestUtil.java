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
    /**
     *   read splunk host info from .splunkrc file
     */
    public static ServiceArgs getSplunkHostInfo()throws IOException {
        String splunkhostfile = System.getProperty("user.home") + File.separator + ".splunkrc";

        ServiceArgs serviceArgs = new ServiceArgs();

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

        return serviceArgs;
    }

    /**
     *   create http input token
     */
    public static String createHttpinput(ServiceArgs serviceArgs,String httpinputName) throws IOException{
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

<<<<<<< HEAD:src/test/java/HttpLoggerTest.java
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
=======
        return token;
>>>>>>> refactoring tests, add TestUtil class for common methods; add tests for java logging:src/test/java/TestUtil.java
    }

    /**
     *   delete http input token
     */
    public static void deleteHttpinput(ServiceArgs serviceArgs,String httpinputName) throws IOException {
        Service service = Service.connect(serviceArgs);
        service.login();

        //remove a httpinput
        try {
            service.delete("/services/data/inputs/http/" + httpinputName);
        } catch (Exception e){}
    }

    /**
     *   modify the config file with the generated token, and configured splunk host
     */
    public static void updateConfigFile(String configFileName, ServiceArgs serviceArgs, String token) throws  IOException{
        String configFileDir = TestUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String configFilePath = new File(configFileDir, configFileName).getPath();
        List<String> lines = Files.readAllLines(new File(configFileDir, configFileName).toPath(), Charset.defaultCharset());
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%user_defined_httpinput_token%")) {
                lines.set(i, lines.get(i).replace("%user_defined_httpinput_token%", token));
            }

            if (lines.get(i).contains("%host%")) {
                lines.set(i, lines.get(i).replace("%host%", serviceArgs.host));
            }

            if (lines.get(i).contains("%port%")) {
                lines.set(i, lines.get(i).replace("%port%",serviceArgs.port.toString()));
            }

            if (lines.get(i).contains("%scheme%")) {
                lines.set(i, lines.get(i).replace("%scheme%", serviceArgs.scheme));
            }
        }

        FileWriter fw = new FileWriter(configFilePath);
        for (String line : lines) {
            fw.write(line);
            fw.write(System.getProperty("line.separator"));
        }

        fw.flush();
        fw.close();
    }

    public static void verifyOneAndOnlyOneEventSendToSplunk(ServiceArgs serviceArgs, String msg) throws IOException {
        //verify the event is send to splunk and can be searched
        Service service = Service.connect(serviceArgs);
        service.login();

        long startTime = System.currentTimeMillis();
        int eventCount = 0;
        InputStream resultsStream=null;
        ResultsReaderXml resultsReader=null;
        while (System.currentTimeMillis()-startTime < 30 * 1000)/*wait for up to 30s*/ {
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
