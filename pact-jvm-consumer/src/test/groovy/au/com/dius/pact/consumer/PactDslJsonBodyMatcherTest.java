package au.com.dius.pact.consumer;

import org.junit.Before;
import org.junit.Test;

public class PactDslJsonBodyMatcherTest {

    private PactDslJsonBody subject;

    @Before
    public void setup() {
        subject = new PactDslJsonBody();
    }

    @Test(expected = InvalidMatcherException.class)
    public void stringMatcherThrowsExceptionIfTheExampleDoesNotMatchThePattern() {
        subject.stringMatcher("name", "[a-z]+", "dfhdsjf87fdjh");
    }

    @Test(expected = InvalidMatcherException.class)
    public void hexMatcherThrowsExceptionIfTheExampleIsNotAHexadecimalValue() {
        subject.hexValue("name", "dfhdsjf87fdjh");
    }

    @Test(expected = InvalidMatcherException.class)
    public void guidMatcherThrowsExceptionIfTheExampleIsNotAGuid() {
        subject.guid("name", "dfhdsjf87fdjh");
    }

}
