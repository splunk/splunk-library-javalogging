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

public class HttpLoggerStressTest {
    private  static  class  GcCaller implements  Runnable
    {
        private int testDurationInSecs = 0;
        public GcCaller(int testDurationInSecs) {
            this.testDurationInSecs = testDurationInSecs;
        }
        public void run() {
            Date dCurrent = new Date();
            Date dEnd = new Date();
            dEnd.setTime(dCurrent.getTime() + testDurationInSecs * 1000);
            while(dCurrent.before(dEnd)) {
                try {
                    Thread.sleep(5000);
                }
                catch (Exception e){}
                //System.gc();
                dCurrent = new Date();
            }
        }
    }
    private static class DataSender implements Runnable {
        private String threadName;
        public int eventsGenerated = 0, testDurationInSecs = 300;
        Logger logger;

        public DataSender(String threadName, int testDurationInSecs) {
            this.threadName = threadName;
            this.testDurationInSecs = testDurationInSecs;
            this.logger = LogManager.getLogger("splunkHttpLogger");
        }

        public void run() {
            Date dCurrent = new Date();
            Date dEnd = new Date();
            dEnd.setTime(dCurrent.getTime() + testDurationInSecs * 1000);
            while(dCurrent.before(dEnd)) {
                this.logger.info(String.format("Thread: %s, event: %d", this.threadName, eventsGenerated++));
                dCurrent = new Date();
            }
        }
    }

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
    private static String httpinputName = "stresshttpinput";

    private static void setupHttpInput() throws Exception {
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

        if (true) // Enable after debugging
        {
            //modify the config file with the generated token
            String configFileDir = HttpLoggerStressTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String configFilePath = new File(configFileDir, "log4j2.xml").getPath();

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
            //fw.write("              batch_interval=\"250\"\r\n");
            //fw.write("              batch_size_count=\"500\"\r\n");
            //fw.write("              batch_size_bytes=\"512\"\r\n");
            fw.write("              source=\"splunktest\" sourcetype=\"battlecat\">\r\n");
            fw.write("            <PatternLayout pattern=\"%m\"/>\r\n");
            fw.write("        </Http>\r\n");
            fw.write("    </Appenders>\r\n");
            fw.write("    <loggers>\r\n");
            fw.write("        <root level=\"debug\"/>\r\n");
            fw.write("        <logger name =\"splunkHttpLogger\" level=\"INFO\">\r\n");
            fw.write("            <appender-ref ref=\"Http\" />\r\n");
            fw.write("        </logger>\r\n");
            fw.write("    </loggers>\r\n");
            fw.write("</Configuration>");

            fw.flush();
            fw.close();
            addPath(configFilePath);
        }
    }

    @Test
    public void canSendEventUsingJavaLogging() throws Exception {
        int numberOfThreads = 50;
        int testDurationInSecs = 600;

        System.out.printf("\tSetting up http inputs ... ");
        setupHttpInput();
        System.out.printf("Inserting data ... ");
        DataSender[] dsList = new DataSender[numberOfThreads];
        Thread[] tList = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            dsList[i] = new DataSender(String.format("Thread%s", i), testDurationInSecs);
            tList[i] = new Thread(dsList[i]);;
        }
        Thread tGc = new Thread(new GcCaller(testDurationInSecs));
        tGc.start();
        for (Thread t : tList)
            t.start();
        for (Thread t : tList)
            t.join();
        tGc.join();
        System.out.printf("Done.\r\n");
        // Wait for indexing to complete
        int eventCount = getEventsCount("search *|stats count");
        for(int i=0; i<3; i++) {
            do {
                System.out.printf("\tWaiting for indexing to complete, %d events so far\r\n", eventCount);
                Thread.sleep(10000);
                int updatedEventCount = getEventsCount("search *|stats count");
                if (updatedEventCount == eventCount)
                    break;
                eventCount = updatedEventCount;
            } while (true);
            System.out.printf("\tCompleted wait for iteration %d\r\n", i);
        }
        Boolean testPassed = true;
        for (int i = 0; i < numberOfThreads; i++) {
            String arguments = String.format("search Thread%d | stats count", i);
            eventCount = getEventsCount(arguments);
            System.out.printf("Thread %d, expected %d events, actually %d, %s\r\n", i, dsList[i].eventsGenerated, eventCount, (eventCount == dsList[i].eventsGenerated)?"passed.":"failed.");
            testPassed &= (eventCount == dsList[i].eventsGenerated);
        }
        System.out.printf("Test %s.\r\n", testPassed ? "PASSED" : "FAILED");
        Assert.assertTrue(testPassed);
    }
}
