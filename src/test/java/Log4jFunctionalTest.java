import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class Log4jFunctionalTest {
    @Test
    public void log4j2SocketAppenderTest() throws InterruptedException {
        final Util.StringContainer container = Util.readLineFromPort(Util.port, Util.timeoutInMs);

        String helloChina = "Hello, \u4E2D\u570B!";

        Logger logger = LogManager.getLogger("splunk.logger");
        logger.info(helloChina);

        synchronized (container) {
            container.wait(Util.timeoutInMs);
        }

        Assert.assertNotNull(container.value);
        Assert.assertEquals("INFO: " + helloChina, container.value);
    }

}
