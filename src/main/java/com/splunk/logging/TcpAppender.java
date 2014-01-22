package com.splunk.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.util.CloseUtil;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.concurrent.*;

public class TcpAppender extends AppenderBase<ILoggingEvent> implements Runnable, SocketConnector.ExceptionHandler {
    public static final int DEFAULT_RECONNECTION_DELAY = 30000; // in ms
    public static final int DEFAULT_QUEUE_SIZE = 0;
    private static final int DEFAULT_ACCEPT_CONNECTION_DELAY = 5000;

    private String host;
    private int port;
    private InetAddress address;

    private Layout<ILoggingEvent> layout;
    private Future<?> task;
    private Future<Socket> connectorTask;

    private int reconnectionDelay = DEFAULT_RECONNECTION_DELAY;
    private int queueSize = DEFAULT_QUEUE_SIZE;
    private int acceptConnectionTimeout = DEFAULT_ACCEPT_CONNECTION_DELAY;

    private BlockingQueue<ILoggingEvent> queue;
    private volatile Socket socket;

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
                SocketConnector connector = new DefaultSocketConnector(address, port, 0, reconnectionDelay);
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
            assert true;    // ok... we'll exit now
        }
        addInfo("shutting down");
    }

    @Override
    public void start() {
        if (started)
            return;

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
            task = getContext().getExecutorService().submit(this);
        }

        super.start();
    }

    @Override
    public void stop() {
        if (!started)
            return;

        CloseUtil.closeQuietly(socket);
        task.cancel(true);
        if(connectorTask != null)
            connectorTask.cancel(true);
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event != null && started)
            queue.offer(event);
    }

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
