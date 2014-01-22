import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackFunctionalTest {
    @Test
    public void logbackSocketAppenderTest() throws InterruptedException {
        final Util.StringContainer container = Util.readLineFromPort(Util.port, Util.timeoutInMs);

        String helloChina = "Hello, \u4E2D\u570B!";

        Logger logger = LoggerFactory.getLogger("splunk.logger");
        logger.info(helloChina);

        synchronized (container) {
            container.wait(Util.timeoutInMs);
        }

        Assert.assertNotNull(container.value);
        Assert.assertEquals("INFO: " + helloChina, container.value);
    }
}
