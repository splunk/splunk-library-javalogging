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

import com.splunk.*;

public class HttpLoggerStressTest {
    private static class DataSender implements Runnable {
        private String threadName;
        public int eventsGenerated = 0, testDurationInSecs = 300;
        Logger logger;

        public DataSender(String threadName, int testDurationInSecs) {
            this.threadName = threadName;
            this.testDurationInSecs = testDurationInSecs;
            this.logger = LogManager.getLogger("splunkStressHttpLogger");
        }

        public void run() {
            Date dCurrent = new Date();
            Date dEnd = new Date();
            dEnd.setTime(dCurrent.getTime() + testDurationInSecs * 1000);
            while(dCurrent.before(dEnd)) {
                this.logger.info(String.format("Thread: %s, event: %d", this.threadName, eventsGenerated++));
                dCurrent = new Date();
                if(eventsGenerated % 1000 == 0) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (Exception e) {
                    }
                }
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
        Service service = TestUtil.connectToSplunk();
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
    private static String httpEventCollectorName = "stressHttpEventCollector";

    private static void setupHttpEventCollector() throws Exception {
        String token = TestUtil.createHttpEventCollectorToken(httpEventCollectorName);
        String loggerName = "splunkStressHttpLogger";
        HashMap<String, String> userInputs = new HashMap<>();
        userInputs.put("user_logger_name", loggerName);
        userInputs.put("user_httpEventCollector_token", token);
        userInputs.put("user_batch_size_count", "1");
        TestUtil.resetLog4j2Configuration("log4j2_template.xml", "log4j2.xml", userInputs);
    }

    @Test
    public void canSendEventUsingJavaLogging() throws Exception {
        long startTime = System.currentTimeMillis()/1000;
        Thread.sleep(2000);
        int numberOfThreads = 1;
        int testDurationInSecs = 60;

        System.out.printf("\tSetting up http event collector ... ");
        setupHttpEventCollector();
        System.out.printf("Inserting data ... ");
        DataSender[] dsList = new DataSender[numberOfThreads];
        Thread[] tList = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            dsList[i] = new DataSender(String.format("Thread%s", i), testDurationInSecs);
            tList[i] = new Thread(dsList[i]);
        }
        for (Thread t : tList)
            t.start();
        for (Thread t : tList)
            t.join();
        int eventsGenerated = 0;
        for(int i=0;i<numberOfThreads;i++)
            eventsGenerated += dsList[i].eventsGenerated;
        System.out.printf("Done. %d Events were generated\r\n", eventsGenerated);
        // Wait for indexing to complete
        String query = String.format("search earliest=%d| stats count", startTime);
        int eventCount = getEventsCount(query);
        if(eventCount == eventsGenerated) {
            System.out.printf("Looks like all events were indexed.\r\n");
        }
        else {
            System.out.printf("Waiting for events indexing using query '%s'\r\n", query);
            for (int i = 0; i < 18 && eventCount != eventsGenerated; i++) {
                do {
                    System.out.printf("\tWaiting for indexing to complete, %d events so far\r\n", eventCount);
                    Thread.sleep(10000);
                    int updatedEventCount = getEventsCount(query);
                    if (updatedEventCount == eventCount)
                        break;
                    eventCount = updatedEventCount;
                } while (true);
                System.out.printf("\tCompleted wait for iteration %d\r\n", i);
            }
        }
        boolean testPassed = true;
        for (int i = 0; i < numberOfThreads; i++) {
            String arguments = String.format("search Thread%d earliest=%d| stats count", i, startTime);
            eventCount = getEventsCount(arguments);
            System.out.printf("Thread %d, expected %d events, actually %d, %s\r\n", i, dsList[i].eventsGenerated, eventCount, (eventCount == dsList[i].eventsGenerated)?"passed.":"failed.");
            testPassed &= (eventCount == dsList[i].eventsGenerated);
        }
        System.out.printf("Test %s.\r\n", testPassed ? "PASSED" : "FAILED");
        Assert.assertTrue(testPassed);
    }
}
