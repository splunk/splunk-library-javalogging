import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class SplunkCimLogEventFunctionalTest {
    ClassLoader baseClassLoader;

    @Before
    public void setup() {
        baseClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @After
    public void teardown() {
        Thread.currentThread().setContextClassLoader(baseClassLoader);
    }

    @Test
    public void logbackSocketAppenderTest() throws MalformedURLException {
        Logger logger = LoggerFactory.getLogger("splunk.logger");
        logger.info("Hi!");
    }


}
