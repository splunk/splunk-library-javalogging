import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class JULFunctionalTest {

    @Test
    public void socketAppenderTest() throws InterruptedException {
        final Util.StringContainer container = Util.readLineFromPort(Util.port, Util.timeoutInMs);

        String helloChina = "Hello, \u4E2D\u570B!";

        Logger logger = Logger.getLogger("splunk.logging");
        logger.info(helloChina);

        synchronized (container) {
            container.wait(Util.timeoutInMs);
        }

        Assert.assertNotNull(container.value);
        Assert.assertEquals("INFO: " + helloChina, container.value);
    }
}
