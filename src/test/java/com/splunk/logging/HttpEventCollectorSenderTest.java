package com.splunk.logging;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class HttpEventCollectorSenderTest {


    final int maxFlushRetries = HttpEventCollectorSender.getMaxFlushRetries();
    HttpEventCollectorSender httpEventCollectorSender;
    final List<Exception> exceptionsThatOccured = new LinkedList<>();

    @Before
    public void setupHttpSender() {
        httpEventCollectorSender = new HttpEventCollectorSender("", "", "", "", 0L, 3, 20000, HttpEventCollectorSender.SendModeSequential, new HashMap<>(), null);

        httpEventCollectorSender = Mockito.spy(httpEventCollectorSender);
        Mockito.doAnswer(invocationOnMock -> {
            throw new NullPointerException("something bad happened");
        }).when(httpEventCollectorSender).postEventsAsync(Mockito.anyList());

        HttpEventCollectorErrorHandler.onError((data, ex) -> exceptionsThatOccured.add(ex));
    }


    @Test
    public void testFlushRetries() {
        httpEventCollectorSender.send("some random message");
        httpEventCollectorSender.send("some random message");
        Mockito.verify(httpEventCollectorSender, Mockito.times(0)).flush();
        httpEventCollectorSender.send("some random message");
        Mockito.verify(httpEventCollectorSender, Mockito.times(1)).flush();

        assertThat(httpEventCollectorSender.getCurrentEventsBatchSize(), Matchers.is(3));
        assertThat(exceptionsThatOccured, Matchers.hasSize(1));

        // generate more until retry limit is hit
        for (int i = 0; i < maxFlushRetries - 1; i++) {
            httpEventCollectorSender.send("some random message");
        }

        assertThat(exceptionsThatOccured, Matchers.hasSize(5));
        assertThat(httpEventCollectorSender.getCurrentEventsBatchSize(), Matchers.is(0));

        Mockito.doNothing().when(httpEventCollectorSender).postEventsAsync(Mockito.anyList());

        for (int i = 0; i < 500; i++) {
            httpEventCollectorSender.send("some random message");
        }

        assertThat(exceptionsThatOccured, Matchers.hasSize(5));

    }
}
