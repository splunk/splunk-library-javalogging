import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        HttpEventCollector_JavaLoggingTest.class,
        HttpEventCollector_Log4j2Test.class,
        HttpEventCollector_LogbackTest.class,
        HttpEventCollector_Test.class
})
public class TestSuiteAcceptanceTests {
    @ClassRule
    public static final TestRuleSplunkContainer TEST_CONTAINER = new TestRuleSplunkContainer();
}
