package com.splunk.logging;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Contains unit tests for the {@link HttpEventCollectorThrowableInfo}. 
 */
public class HttpEventCollectorThrowableInfoUnitTest {

	@Test
	public void testThatBuildingFromNullReturnsNull() {
		// When a null is used to build an info.
		final HttpEventCollectorThrowableInfo actual = 
				HttpEventCollectorThrowableInfo.buildFromThrowable(null);
		
		// Then a null should have been returned.
		Assert.assertNull(actual);
	}
	
	@Test
	public void testThatBuildingWithNoCauseReturnsInfo() {
		// Given a throwable with no cause.
		final Throwable throwable = new RuntimeException("message");
		
		// When the info is built.
		final HttpEventCollectorThrowableInfo actual = 
				HttpEventCollectorThrowableInfo.buildFromThrowable(throwable);
		
		// Then the info should have the expected values.
		Assert.assertEquals("message", actual.getMessage());
		Assert.assertEquals("java.lang.RuntimeException", actual.getClassName());
		Assert.assertNull(actual.getCause());
		
		// And the stack trace elements should have been populated.
		List<String> stackTraceElements = actual.getStackTraceElements();
		Assert.assertTrue(stackTraceElements.get(0).contains("testThatBuildingWithNoCauseReturnsInfo"));
	}
	
	@Test
	public void testThatBuildingWithCauseReturnsInfo() {
		// Given a throwable with a cause.
		final Throwable cause = new IllegalArgumentException("cause message");
		final Throwable throwable = new RuntimeException("message", cause);
		
		// When the info is built.
		final HttpEventCollectorThrowableInfo actual = 
				HttpEventCollectorThrowableInfo.buildFromThrowable(throwable);
		
		// Then the info should have the expected values.
		Assert.assertEquals("message", actual.getMessage());
		Assert.assertEquals("java.lang.RuntimeException", actual.getClassName());
		
		// And the stack trace elements should have been populated.
		final List<String> stackTraceElements = actual.getStackTraceElements();
		Assert.assertTrue(stackTraceElements.get(0).contains("testThatBuildingWithCauseReturnsInfo"));
		
		// And the cause should have been populated.
		Assert.assertNotNull(actual.getCause());
		final HttpEventCollectorThrowableInfo causeInfo = actual.getCause();
		
		Assert.assertEquals("cause message", causeInfo.getMessage());
		Assert.assertEquals("java.lang.IllegalArgumentException", causeInfo.getClassName());
		
		// And the common stack trace elements should have been removed - one element
		// because the cause is created on a different line in this method.
		final List<String> causeStackTraceElements = causeInfo.getStackTraceElements();
		Assert.assertEquals(1, causeStackTraceElements.size());
	}
	
	@Test
	public void testThatBuildingWithCauseEliminatesCommonStackFramesInfo() {
		// Given a throwable with a cause.
		final Throwable cause = createInMethod();
		final Throwable throwable = new RuntimeException("message", cause);
		
		// When the info is built.
		final HttpEventCollectorThrowableInfo actual = 
				HttpEventCollectorThrowableInfo.buildFromThrowable(throwable);
		
		// Then the info should have the expected values.
		Assert.assertEquals("message", actual.getMessage());
		Assert.assertEquals("java.lang.RuntimeException", actual.getClassName());
		
		// And the stack trace elements should have been populated.
		final List<String> stackTraceElements = actual.getStackTraceElements();
		Assert.assertTrue(stackTraceElements.get(0).contains("testThatBuildingWithCauseEliminatesCommonStackFramesInfo"));
		
		// And the cause should have been populated.
		Assert.assertNotNull(actual.getCause());
		final HttpEventCollectorThrowableInfo causeInfo = actual.getCause();
		
		// And the common stack trace elements should have been removed - leaving the 
		// two different elements - one because the cause is generated in a different method
		// and one because the cause is generated on a different line in this method.
		final List<String> causeStackTraceElements = causeInfo.getStackTraceElements();
		Assert.assertEquals(2, causeStackTraceElements.size());
	}
	
	@Test
	public void testThatBuildingHandlesACausualLoop() {
		// Given a throwable with itself as a cause.
		final Throwable throwable = new LoopException("message");
		
		// When the info is built.
		final HttpEventCollectorThrowableInfo actual = 
				HttpEventCollectorThrowableInfo.buildFromThrowable(throwable);
		
		// Then the info should have the expected values.
		Assert.assertEquals("message", actual.getMessage());
		Assert.assertEquals(
				"com.splunk.logging.HttpEventCollectorThrowableInfoUnitTest.LoopException", 
				actual.getClassName());
		
		// And the stack trace elements should have been populated.
		final List<String> stackTraceElements = actual.getStackTraceElements();
		Assert.assertTrue(stackTraceElements.get(0).contains("testThatBuildingHandlesACausualLoop"));
		
		// And the cause should be null.
		Assert.assertNull(actual.getCause());
	}
	
	@SuppressWarnings("serial")
	public static final class LoopException extends Exception {
		public LoopException(String message) {
			super(message);
		}
		
		@Override
		public synchronized Throwable getCause() {
			return this;
		}
	}
	
	private Throwable createInMethod() {
		return new RuntimeException("failure"); 
	}
}
