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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.splunk.logging.HttpInputLoggingErrorHandler;
import com.splunk.logging.HttpInputLoggingEventInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.LogManager;

public class HttpInputLoggerUnitTest {
    private final static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
    private final static org.apache.logging.log4j.Logger LOG4J = org.apache.logging.log4j.LogManager.getLogger("splunk.log4j");
    private final static org.slf4j.Logger LOGBACK = org.slf4j.LoggerFactory.getLogger("splunk.logback");
    private String lastReply = "";
    private HttpInputLoggingEventInfo lastEvent;
    private static final String SuccessReply = "{\"text\":\"Success\",\"code\":0}";
    private static final String ErrorReply = "{\"text\":\"Error\",\"code\":1}";

    private static class TestHttpServerHandler implements HttpHandler {
        private List<JSONObject> jsonObjects = new LinkedList<JSONObject>();

        public void handle(HttpExchange httpExchange) throws IOException {
            byte[] bodyBytes = new byte[httpExchange.getRequestBody().available()];
            httpExchange.getRequestBody().read(
                    bodyBytes, 0, httpExchange.getRequestBody().available());
            String body = new String(bodyBytes);
            System.out.println(body);
            // extract individual json documents and parse
            boolean error = body.contains("fail");
            int bracketBalance = 0;
            int begin = 0, end = 0;
            for (end = 0; end < body.length(); end ++) {
                if (body.charAt(end) == '{') bracketBalance ++;
                else if (body.charAt(end) == '}') bracketBalance --;
                if (bracketBalance == 0) {
                    String json = body.substring(begin, end+1);
                    JSONParser jsonParser = new JSONParser();
                    try {
                        jsonObjects.add(jsonObjects.size(), (JSONObject) jsonParser.parse(json));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Assert.fail();
                    }
                    begin = end;
                }
            }
            // reply
            String response = error ? ErrorReply : SuccessReply;;
            httpExchange.sendResponseHeaders(error ? 500 : 200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        public JSONObject pop() {
            if (jsonObjects.size() == 0)
                return null;
            JSONObject jsonObject = jsonObjects.get(0);
            jsonObjects.remove(jsonObject);
            return jsonObject;
        }
    }

    private static TestHttpServerHandler httpHandler = new TestHttpServerHandler();

    private final String LoggerConf =
        "handlers=com.splunk.logging.HttpInputHandler\n" +
        "com.splunk.logging.HttpInputHandler.url=http://localhost:5555/services/logging\n" +
        "com.splunk.logging.HttpInputHandler.token=22C712B0-E6EE-4355-98DD-2DDE23D968D7\n" +
        "com.splunk.logging.HttpInputHandler.disableCertificateValidation=true";

    public HttpInputLoggerUnitTest() {

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(5555), 0);
            server.createContext("/", httpHandler);
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (Exception e) {
            System.out.print(e);
            e.printStackTrace();
            Assert.fail();
        }

        try {
            LogManager.getLogManager().readConfiguration(
                new ByteArrayInputStream(LoggerConf.getBytes())
            );
        } catch (IOException e) {
            System.out.print(e);
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void simpleLogging() {

        HttpInputLoggingErrorHandler.onError(new HttpInputLoggingErrorHandler.ErrorCallback() {
            public void error(final List<HttpInputLoggingEventInfo> data, final Exception ex) {
                HttpInputLoggingErrorHandler.ServerErrorException serverErrorException =
                        (HttpInputLoggingErrorHandler.ServerErrorException)ex;
                lastReply = serverErrorException.getReply();
                lastEvent = data.get(0);
            }
        });

        // java.util.logger
        LOGGER.info("this is info");
        shortSleep();
        LOGGER.warning("this is warning");

        sleep(); // wait for http server to receive all data
        testEvent(httpHandler.pop(), "INFO", "this is info");
        testEvent(httpHandler.pop(), "WARNING", "this is warning");

        // log4j
        LOG4J.info("this is info");
        shortSleep();
        LOG4J.error("this is error");
        sleep();
        testEvent(httpHandler.pop(), "INFO", "this is info");
        testEvent(httpHandler.pop(), "ERROR", "this is error");

        // logback
        LOGBACK.info("this is info");
        sleep();
        shortSleep();
        LOGBACK.error("this is error");
        sleep(); // wait for http server to receive all data
        testEvent(httpHandler.pop(), "INFO", "this is info");
        testEvent(httpHandler.pop(), "ERROR", "this is error");

        // error detection
        Assert.assertFalse(lastReply.equalsIgnoreCase(ErrorReply));
        LOGGER.info("fail");
        sleep();
        Assert.assertTrue(lastReply.equalsIgnoreCase(ErrorReply));
        Assert.assertTrue(lastEvent.getSeverity().equalsIgnoreCase("INFO"));
        Assert.assertTrue(lastEvent.getMessage().equalsIgnoreCase("fail"));
    }

    private void testEvent(JSONObject json, String severity, String message) {
        Assert.assertNotNull(json);
        JSONObject event = (JSONObject)json.get("event");
        Assert.assertNotNull(event);
        Assert.assertTrue(String.format((String)event.get("severity")).equalsIgnoreCase(severity));
        Assert.assertTrue(((String)event.get("message")).equalsIgnoreCase(message));
    }

    // shot sleep to deliver logging requests in order
    private void shortSleep() {
        try {
            Thread.sleep(100);
        } catch (Exception e) {}
    }

    private void sleep() {
        try {
            Thread.sleep(2000);
        } catch (Exception e) {}
    }
}
