package com.splunk.logging;

import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Contains self-contained unit tests for the {@link HttpEventCollectorSender}.
 */
public class HttpEventCollectorSenderUnitTest {
	
	private static final long TIMEOUT = 15000L;

	private AtomicReference<String> container = new AtomicReference<String>();
	
	private LocalTestServer server = new LocalTestServer(null, null);
	
	private HttpRequestHandler myHttpRequestHandler = new HttpRequestHandler() {
		@Override
		public void handle(HttpRequest request, HttpResponse response,
				HttpContext context) throws HttpException, IOException {
			response.setEntity(new StringEntity(""));
			response.setStatusCode(200);
			
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				container.set(EntityUtils.toString(entity));
			}
			
            synchronized (container) {
            	container.notifyAll();
            }
		}
	};

	private String apiUrl;

	private HttpEventCollectorSender sender;

	@Before
	public void setUp() throws Exception {
		server.start();
		server.register("/*", myHttpRequestHandler);

		apiUrl = "http:/" + server.getServiceAddress() + "/api";
		sender = new HttpEventCollectorSender(apiUrl, "", 0, 1, 1, "sequential", new Hashtable<String, String>());
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
	}
	
	@Test
	public void testThatNoThrowableInfoIsSent() throws Throwable {
		// Given a HttpEventCollectorSender that is wired up to send to the fake endpoint.

		// When an event with no throwable is sent.
		this.sender.send("ERROR", "test_message", "test_logger_name", "test_thread_name", null, (Throwable) null, null);
		
		// Then the event should have been serialised and sent to the endpoint.
		synchronized (this.container) {
        	this.container.wait(TIMEOUT);
        }
        
		final JSONObject wrapper = (JSONObject) new JSONParser().parse(container.get());
		final JSONObject event = (JSONObject) wrapper.get("event");
		
		// And the the event should contain the base properties.
		Assert.assertEquals("ERROR", event.get("severity"));
		Assert.assertEquals("test_logger_name", event.get("logger"));
		Assert.assertEquals("test_message", event.get("message"));
		Assert.assertEquals("test_thread_name", event.get("thread"));
		
		// And the event should contain no throwable information.
		final JSONObject throwableObject = (JSONObject) event.get("throwable");
		Assert.assertNull(throwableObject);
	}

	@Test
	public void testThatThrowableInfoIsSent() throws Throwable {
		// Given a HttpEventCollectorSender that is wired up to send to the fake endpoint.

		// When an event with a throwable is sent.
		final Throwable throwable = new RuntimeException("test_exception");
		this.sender.send("ERROR", "test_message", "test_logger_name", "test_thread_name", null, throwable, null);
		
		// Then the event should have been serialised and sent to the endpoint.
		synchronized (this.container) {
        	this.container.wait(TIMEOUT);
        }
        
		final JSONObject wrapper = (JSONObject) new JSONParser().parse(container.get());
		final JSONObject event = (JSONObject) wrapper.get("event");
		
		// And the the event should contain the base properties.
		Assert.assertEquals("ERROR", event.get("severity"));
		Assert.assertEquals("test_logger_name", event.get("logger"));
		Assert.assertEquals("test_message", event.get("message"));
		Assert.assertEquals("test_thread_name", event.get("thread"));
		
		// And the event should contain the throwable information.
		final JSONObject throwableObject = (JSONObject) event.get("throwable");
		Assert.assertEquals("test_exception", throwableObject.get("throwable_message"));
		Assert.assertEquals("java.lang.RuntimeException", throwableObject.get("throwable_class"));
		
		// And no cause should have been sent.
		final JSONObject causeObject = (JSONObject) throwableObject.get("cause");
		Assert.assertNull(causeObject);
	}

	@Test
	public void testThatThrowableCauseInfoIsSent() throws Throwable {
		// Given a HttpEventCollectorSender that is wired up to send to the fake endpoint.

		// When an event with a throwable that is caused by another throwable is sent.
		final Throwable cause = new IllegalStateException("cause message");
		final Throwable throwable = new RuntimeException("test_exception", cause);
		this.sender.send("ERROR", "test_message", "test_logger_name", "test_thread_name", null, throwable, null);
		
		// Then the event should have been serialised and sent to the endpoint.
		synchronized (this.container) {
        	this.container.wait(TIMEOUT);
        }
        
		final JSONObject wrapper = (JSONObject) new JSONParser().parse(container.get());
		final JSONObject event = (JSONObject) wrapper.get("event");
				
		// And the event should contain the throwable information.
		final JSONObject throwableObject = (JSONObject) event.get("throwable");
		Assert.assertEquals("test_exception", throwableObject.get("throwable_message"));
		Assert.assertEquals("java.lang.RuntimeException", throwableObject.get("throwable_class"));
		
		// And the cause should have been serialised as well.
		final JSONObject causeObject = (JSONObject) throwableObject.get("cause");
		Assert.assertEquals("cause message", causeObject.get("throwable_message"));
		Assert.assertEquals("java.lang.IllegalStateException", causeObject.get("throwable_class"));
	}
	
}
