import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class SplunkTestWatcher extends TestWatcher {

    @Override
    protected void starting(Description description) {
        System.out.println("Starting test: " + description.getMethodName());
    }

    @Override
    protected void succeeded(Description description) {
        System.out.println("====================== Test pass=========================");
    }

    @Override
    protected void finished(Description description) {
        // GitHub pipeline is not including output from last test failure
        System.out.flush();
    }
}
