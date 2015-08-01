package au.com.dius.pact.consumer;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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

    @Test
    public void allowsLikeMatchersWhenTheArrayIsTheRoot() {
        Date date = new Date(1100110011001L);
        subject = (PactDslJsonArray) PactDslJsonArray.arrayEachLike()
            .date("clearedDate", "mm/dd/yyyy", date)
            .stringType("status", "STATUS")
            .realType("amount", 100.0)
            .closeObject();

        assertThat(subject.getBody().toString(), is(equalTo("[{\"amount\":100,\"clearedDate\":\"06/11/2004\",\"status\":\"STATUS\"}]")));
        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
            "[*].amount", "[*].clearedDate", "[*].status"
        ));
        assertThat(subject.getMatchers().keySet(), is(equalTo(expectedMatchers)));
    }

    @Test
    public void allowsLikeMinMatchersWhenTheArrayIsTheRoot() {
        Date date = new Date(1100110011001L);
        subject = (PactDslJsonArray) PactDslJsonArray.arrayMinLike(1)
            .date("clearedDate", "mm/dd/yyyy", date)
            .stringType("status", "STATUS")
            .realType("amount", 100.0)
            .closeObject();

        assertThat(subject.getBody().toString(), is(equalTo("[{\"amount\":100,\"clearedDate\":\"06/11/2004\",\"status\":\"STATUS\"}]")));
        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
            "", "[*].amount", "[*].clearedDate", "[*].status"
        ));
        assertThat(subject.getMatchers().keySet(), is(equalTo(expectedMatchers)));
    }

    @Test
    public void allowsLikeMaxMatchersWhenTheArrayIsTheRoot() {
        Date date = new Date(1100110011001L);
        subject = (PactDslJsonArray) PactDslJsonArray.arrayMaxLike(10)
            .date("clearedDate", "mm/dd/yyyy", date)
            .stringType("status", "STATUS")
            .realType("amount", 100.0)
            .closeObject();

        assertThat(subject.getBody().toString(), is(equalTo("[{\"amount\":100,\"clearedDate\":\"06/11/2004\",\"status\":\"STATUS\"}]")));
        Set<String> expectedMatchers = new HashSet<String>(Arrays.asList(
            "", "[*].amount", "[*].clearedDate", "[*].status"
        ));
        assertThat(subject.getMatchers().keySet(), is(equalTo(expectedMatchers)));
    }
}
