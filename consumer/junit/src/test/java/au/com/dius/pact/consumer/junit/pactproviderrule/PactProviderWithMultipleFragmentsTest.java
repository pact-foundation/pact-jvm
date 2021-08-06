package au.com.dius.pact.consumer.junit.pactproviderrule;

import au.com.dius.pact.consumer.junit.DefaultRequestValues;
import au.com.dius.pact.consumer.junit.DefaultResponseValues;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.consumer.junit.PactVerifications;
import au.com.dius.pact.consumer.dsl.PactDslRequestWithoutPath;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.apache.hc.client5.http.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PactProviderWithMultipleFragmentsTest {

    @Rule
    public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", this);

    @Rule
    public PactProviderRule mockTestProvider2 = new PactProviderRule("test_provider2", this);

    @DefaultRequestValues
    public void defaultRequestValues(PactDslRequestWithoutPath request) {
      Map<String, String> headers = new HashMap<String, String>();
      headers.put("testreqheader", "testreqheadervalue");
      request.headers(headers);
    }

    @DefaultResponseValues
    public void defaultResponseValues(PactDslResponse response) {
      Map<String, String> headers = new HashMap<String, String>();
      headers.put("testresheader", "testresheadervalue");
      response.headers(headers);
    }

    @Pact(consumer="test_consumer", provider = "test_provider")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
        return builder
            .given("good state")
            .uponReceiving("PactProviderTest test interaction")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .uponReceiving("PactProviderTest second test interaction")
                .method("OPTIONS")
                .path("/second")
                .body("")
            .willRespondWith()
                .status(200)
                .body("")
            .toPact();
    }

    @Pact(consumer="test_consumer", provider = "test_provider2")
    public RequestResponsePact createFragment2(PactDslWithProvider builder) {
        return builder
                .given("good state")
                .uponReceiving("PactProviderTest test interaction 2")
                    .path("/")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .body("{\"responsetest\": true, \"name\": \"fred\"}")
                .toPact();
    }

    @Pact(consumer="test_consumer", provider = "test_provider2")
    public RequestResponsePact createFragment3(PactDslWithProvider builder) {
        return builder
          .given("bad state")
          .uponReceiving("PactProviderTest test interaction 3")
          .path("/path/2")
          .method("GET")
          .willRespondWith()
          .status(404)
          .body("{\"error\": \"ID 2 does not exist\"}")
          .toPact();
    }

    @Test
    @PactVerification(value = "test_provider2", fragment = "createFragment2")
    public void runTestWithFragment2() throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        assertEquals(new ConsumerClient(mockTestProvider2.getUrl()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerification(value = "test_provider", fragment = "createFragment")
    public void runTestWithFragment1() throws IOException {
        Assert.assertEquals(new ConsumerClient(mockTestProvider.getUrl()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerifications({
      @PactVerification(value = "test_provider", fragment = "createFragment"),
      @PactVerification(value = "test_provider2", fragment = "createFragment2")
    })
    public void runTestWithBothFragments() throws IOException {
        Assert.assertEquals(new ConsumerClient(mockTestProvider.getUrl()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);

        expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        assertEquals(new ConsumerClient(mockTestProvider2.getUrl()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerifications({
      @PactVerification(value = "test_provider", fragment = "createFragment"),
      @PactVerification(value = "test_provider2", fragment = "createFragment2"),
      @PactVerification(value = "test_provider2", fragment = "createFragment3")
    })
    public void runTestWithAllFragments() throws IOException {
        Assert.assertEquals(new ConsumerClient(mockTestProvider.getUrl()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);

        expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        assertEquals(new ConsumerClient(mockTestProvider2.getUrl()).getAsMap("/", ""), expectedResponse);

        try {
            new ConsumerClient(mockTestProvider2.getUrl()).getAsMap("/path/2", "");
            fail();
        } catch (HttpResponseException ex) {
            assertThat(ex.getStatusCode(), is(404));
        }
    }
}
