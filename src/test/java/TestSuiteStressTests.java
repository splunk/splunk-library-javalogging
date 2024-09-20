import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        HttpLoggerStressTest.class
})
public class TestSuiteStressTests {
    @ClassRule
    public static final TestRuleSplunkContainer TEST_CONTAINER = new TestRuleSplunkContainer();
}
