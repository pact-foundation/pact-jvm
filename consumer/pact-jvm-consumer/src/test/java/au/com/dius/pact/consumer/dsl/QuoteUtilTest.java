package au.com.dius.pact.consumer.dsl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuoteUtilTest {

    @Test
    public void testSingleQuotes() {
        final String converted = QuoteUtil.convert("{'name': 'Alex'}");
        assertEquals("{\"name\": \"Alex\"}", converted);
    }

    @Test
    public void testSkipDoubleQuotes() {
        final String converted = QuoteUtil.convert("{\"name\": \"Alex\"}");
        assertEquals("{\"name\": \"Alex\"}", converted);
    }

}
