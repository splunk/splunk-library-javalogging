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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Util {
    // Port that we are going to use for testing.
    public static final int port = 15000;

    // Timeout for all TCP connection and reading waits.
    public static final int timeoutInMs = 500;

    // A container that we can put a string in and use wait/notify on.
    public static class StringContainer {
        public String value = null;
    }

    /**
     * Asynchronously read a line from a TCP port or time out.
     *
     * This method immediately returns a StringContainer object with its value set to <tt>null</tt>.
     * It then listens on TCP port <tt>port</tt>. If a line arrives on that port within <tt>timeoutInMs</tt>
     * milliseconds, its sets that line (minus the terminating newline) as the value of the returned
     * StringContainer and calls notifyAll on the StringContainer. If no line as arrived after the timeout
     * expires, it calls notifyAll but leaves the value <tt>null</tt>.
     *
     * A user of the method should call it something like this:
     *
     *     StringContainer container = readLineFromPort(10000, 500);
     *
     *     // Write something to port 10000
     *
     *     synchronized (container) { container.wait(); }
     *     if (container.value != null) {
     *         // Do something with the read line
     *     } else {
     *         // Timed out
     *     }
     *
     * @param port Port to listen on.
     * @param timeoutInMs How many milliseconds to wait for a line.
     * @return a StringContainer to wait on.
     */
    public static StringContainer readLineFromPort(final int port, final int timeoutInMs) {
        final StringContainer container = new StringContainer();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                Socket socket = null;

                try {
                    serverSocket = new ServerSocket(port);
                    serverSocket.setSoTimeout(timeoutInMs);
                    socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    container.value = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (serverSocket != null) try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    synchronized (container) {
                        container.notifyAll();
                    }
                }
            }
        }).start();

        return container;
    }
}
