
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TestRuleSplunkContainer implements TestRule {
    // Allow choosing image version from CI
    private static String dockerImageName() {
        String version = System.getenv().getOrDefault("USE_IMAGE_VERSION", "latest");
        return String.join(
                ":",
                "splunk/splunk",
                version);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        try (
                GenericContainer<?> splunk = new FixedHostPortGenericContainer<>(dockerImageName())
                        .withFixedExposedPort(8000, 8000)
                        .withFixedExposedPort(5555, 5555)
                        .withFixedExposedPort(8088, 8088)
                        .withFixedExposedPort(8089, 8089)
                        .withFixedExposedPort(15000, 15000)
                        .withFixedExposedPort(10667, 10667)
                        .withFixedExposedPort(10668, 10668, InternetProtocol.UDP)
                        .withExposedPorts(9997)
                        .withEnv("SPLUNK_START_ARGS", "--accept-license")
                        .withEnv("SPLUNK_PASSWORD", "changed!")
                        .withStartupTimeout(Duration.ofMinutes(2L))
                        .waitingFor(Wait.forListeningPorts(9997)) // must wait on an unfixed port
        ) {
            return wrapTestCase(base, splunk);
        }
    }

    private Statement wrapTestCase(final Statement base, final GenericContainer<?> container) {
        return new Statement() {
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<>();

                try {
                    // Pre-Test
                    container.start();
                    base.evaluate();
                } catch (Throwable preError) {
                    errors.add(preError);
                } finally {
                    // Post-Test
                    try {
                        container.stop();
                    } catch (Throwable postError) {
                        errors.add(postError);
                    }
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }
}
