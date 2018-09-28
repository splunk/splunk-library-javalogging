package com.splunk.logging;
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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.util.CloseUtil;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.concurrent.*;

/**
 * Logback Appender which writes its events to a TCP port.
 *
 * This class is based on the logic of Logback's SocketAppender, but does not try to serialize Java
 * objects for deserialization and logging elsewhere.
 */
public class TcpAppender extends AppenderBase<ILoggingEvent> implements Runnable, SocketConnector.ExceptionHandler {
    private static final int DEFAULT_RECONNECTION_DELAY = 30000; // in ms
    private static final int DEFAULT_QUEUE_SIZE = 0;
    private static final int DEFAULT_ACCEPT_CONNECTION_DELAY = 5000;

    private String host;
    private int port;
    private InetAddress address;

    private Layout<ILoggingEvent> layout;
    private ExecutorService executor;
    private Future<Socket> connectorTask;

    private int reconnectionDelay = DEFAULT_RECONNECTION_DELAY;
    private int queueSize = DEFAULT_QUEUE_SIZE;
    private int acceptConnectionTimeout = DEFAULT_ACCEPT_CONNECTION_DELAY;

    private BlockingQueue<ILoggingEvent> queue;

    // The socket will be modified by the another thread (in SocketConnector) which
    // handles reconnection of dropped connections.
    private volatile Socket socket;

    // The appender is created by Logback calling a superclass constructor with no arguments.
    // Then it calls setters (and the setters defined by the class define what arguments are
    // understood. Once all the fields have been set, Logback calls start(), When shutting down,
    // Logback calls stop().
    //
    // start() queues the appender as a Runnable, so run() eventually gets invoked to do the
    // actual work. run() opens a port using Logback utilities that reconnect when a connection
    // is lost, and then block on a queue of events, writing them to TCP as soon as they
    // become available.
    //
    // The append method, which Logback logging calls invoke, pushes events to that queue and nothing else.

    @Override
    public void connectionFailed(SocketConnector socketConnector, Exception e) {
        if (e instanceof InterruptedException) {
            addInfo("connector interrupted");
        } else if (e instanceof ConnectException) {
            addInfo(host + ":" + port + " connection refused");
        } else {
            addInfo(host + ":" + port + " " + e);
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                SocketConnector connector = initSocketConnector();
                connector.setExceptionHandler(this);
                connector.setSocketFactory(SocketFactory.getDefault());

                try {
                    connectorTask = getContext().getExecutorService().submit(connector);
                } catch (RejectedExecutionException e) {
                    connectorTask = null;
                    break;
                }

                try {
                    socket = connectorTask.get();
                    connectorTask = null;
                } catch (ExecutionException e) {
                    socket = null;
                    break;
                }

                try {
                    socket.setSoTimeout(acceptConnectionTimeout);
                    OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());
                    socket.setSoTimeout(0);

                    addInfo(host + ":" + port + " connection established");

                    while (true) {
                        ILoggingEvent event = queue.take();
                        String formatted = layout.doLayout(event);
                        writer.write(formatted);
                        writer.flush();
                    }
                } catch (SocketException e) {
                    addInfo(host + ":" + port + " connection failed: " + e);
                } catch (IOException e) {
                    CloseUtil.closeQuietly(socket);
                    socket = null;
                    addInfo(host + ":" + port + " connection closed");
                }
            }
        } catch (InterruptedException ex) {
            // Exiting.
        }
        addInfo("exiting");
    }

    private SocketConnector initSocketConnector() {

        DefaultSocketConnector connector = null;

        try {
            connector = getDefaultSocketConnectorConstr().newInstance(address, port, 0, reconnectionDelay);
        }

        catch (InvocationTargetException e) {
            throwRuntimeException(e);
        }
        catch (InstantiationException e) {
            throwRuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throwRuntimeException(e);
        }

        return connector;
    }

    private void throwRuntimeException(Exception e) throws RuntimeException {
        throw new RuntimeException("Could not invoke DefaultSocketConnector constructor, check your Logback version.", e);
    }

    private Constructor<DefaultSocketConnector> getDefaultSocketConnectorConstr() {

        try {
            return getLogback_1_1_version();
        }
        catch (NoSuchMethodException ex) {
            // do nothing - just go on
        }

        try {
            return getLogback_1_0_version();
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("No known DefaultSocketConnector implementation available. Check your Logback version.", e);
        }

    }

    private Constructor<DefaultSocketConnector> getLogback_1_0_version() throws NoSuchMethodException {
        return DefaultSocketConnector.class.getConstructor(InetAddress.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
    }

    private Constructor<DefaultSocketConnector> getLogback_1_1_version() throws NoSuchMethodException {
        return DefaultSocketConnector.class.getConstructor(InetAddress.class, Integer.TYPE, Long.TYPE, Long.TYPE);
    }

    @Override
    public void start() {
        if (started) {
            return;
        }

        boolean errorPresent = false;

        // Handle options
        if (port <= 0) {
            errorPresent = true;
            addError("No port was configured for appender"
                    + name
                    + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_port");
        }

        if (host == null) {
            errorPresent = true;
            addError("No remote host was configured for appender"
                    + name
                    + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_host");
        }

        if (queueSize < 0) {
            errorPresent = true;
            addError("Queue size must be non-negative");
        }

        if (this.layout == null) {
            addError("No layout set for the appender named [" + name + "].");
            errorPresent = true;
        }

        if (!errorPresent) {
            try {
                address = InetAddress.getByName(host);
            } catch (UnknownHostException ex) {
                addError("unknown host: " + host);
                errorPresent = true;
            }
        }


        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            addError("Unknown host: " + host);
            errorPresent = true;
        }

        // Dispatch this instance of the appender.
        if (!errorPresent) {
            queue = queueSize <= 0 ? new SynchronousQueue<ILoggingEvent>() : new ArrayBlockingQueue<ILoggingEvent>(queueSize);
            ThreadFactory factory = new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "splunk-tcp-appender");
                    t.setDaemon(true);
                    return t;
                }
            };
            executor = Executors.newSingleThreadExecutor(factory);
            executor.execute(this);
        }

        super.start();
    }

    @Override
    public void stop() {
        if (!started)
            return;

        CloseUtil.closeQuietly(socket);
        if (executor != null) {
            executor.shutdownNow();
        }
        if (connectorTask != null) {
            connectorTask.cancel(true);
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Get runtime information now, rather than when
        // the event is actually logged, so that it has
        // the right thread and environment information.
        event.prepareForDeferredProcessing();
        event.getCallerData();

        // Append to the queue to be logged.
        if (event != null && started)
            queue.offer(event);
    }

    // The setters are peculiar here. They are used by Logback (via reflection) to set
    // the parameters of the appender, but they should never be called except by
    // Logback before start() is called.
    public void setRemoteHost(String host) { this.host = host; }
    public String getRemoteHost() { return this.host; }

    public void setPort(int port) { this.port = port; }
    public int getPort() { return this.port; }

    public void setReconnectionDelay(int reconnectionDelay) { this.reconnectionDelay = reconnectionDelay; }
    public int getReconnectionDelay() { return this.reconnectionDelay; }

    public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
    public int getQueueSize() { return this.queueSize; }

    public void setLayout(Layout<ILoggingEvent> layout) { this.layout = layout; }
    public Layout<ILoggingEvent> getLayout() { return this.layout; }
}
