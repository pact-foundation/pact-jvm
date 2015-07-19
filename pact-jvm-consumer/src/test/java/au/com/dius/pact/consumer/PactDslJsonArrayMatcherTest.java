package au.com.dius.pact.consumer;

import org.junit.Before;
import org.junit.Test;

public class PactDslJsonArrayMatcherTest {

    private PactDslJsonArray subject;

    @Before
    public void setup() {
        subject = new PactDslJsonArray();
    }

    @Test(expected = InvalidMatcherException.class)
    public void stringMatcherThrowsExceptionIfTheExampleDoesNotMatchThePattern() {
        subject.stringMatcher("[a-z]+", "dfhdsjf87fdjh");
    }

    @Test(expected = InvalidMatcherException.class)
    public void hexMatcherThrowsExceptionIfTheExampleIsNotAHexadecimalValue() {
        subject.hexValue("dfhdsjf87fdjh");
    }

    @Test(expected = InvalidMatcherException.class)
    public void guidMatcherThrowsExceptionIfTheExampleIsNotAGuid() {
        subject.guid("dfhdsjf87fdjh");
    }

}
