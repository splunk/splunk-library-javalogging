import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Util {
    public static final int port = 15000;
    public static final int timeoutInMs = 500;

    public static class StringContainer {
        public String value = null;
    }

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
