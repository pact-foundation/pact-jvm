package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.RequestResponsePact;

import java.io.IOException;

public class QueryParameterMatchingTest extends ConsumerPactTest {

    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
                .uponReceiving("java test interaction with a query string matcher")
                .path("/some path")
                .method("GET")
                .queryMatchingDate("date", "yyyy-MM-dd", "2011-12-03")
                .queryMatchingDate("date2", "yyyy-MM-dd")
                .queryMatchingTime("time", "HH:mm:ss", "11:12:03")
                .queryMatchingDatetime("datetime", "yyyy-MM-dd HH:mm:ss", "2011-12-03 00:00:00")
                .queryMatchingISODate("isodate", "2011-12-03")
                .queryMatchingISOTime("isotime", "11:12:03")
                .queryMatchingISODatetime("isodatetime", "2011-12-03")
                .willRespondWith()
                .status(200)
                .toPact();
    }

    @Override
    protected String providerName() {
        return "test_provider";
    }

    @Override
    protected String consumerName() {
        return "test_consumer";
    }

    @Override
    protected void runTest(MockServer mockServer, PactTestExecutionContext context) throws IOException {
        new ConsumerClient(mockServer.getUrl()).getAsMap("/some path",
          "date=2011-12-03&date2=2012-09-13&time=10:05:22&datetime=2019-01-23 16:09:33" +
            "&isodate=2011-12-03&isotime=10:05:22&isodatetime=2019-01-23T16:09:33+11:00");
    }
}
