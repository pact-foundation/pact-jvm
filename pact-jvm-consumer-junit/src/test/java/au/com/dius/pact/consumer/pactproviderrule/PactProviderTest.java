package au.com.dius.pact.consumer.pactproviderrule;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.Request;
import au.com.dius.pact.model.RequestResponsePact;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PactProviderTest {

    @Rule
    public TestFailureProviderRule mockTestProvider = new TestFailureProviderRule("test_provider", this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .given("good state")
            .uponReceiving("PactProviderTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .uponReceiving("PactProviderTest second test interaction")
                .method("OPTIONS")
                .headers(headers)
                .path("/second")
                .body("")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("")
            .toPact();
    }

    @Test
    @PactVerification(value = "test_provider")
    public void runTest() throws IOException {
        Assert.assertEquals(new ConsumerClient(mockTestProvider.getUrl()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerification("test_provider")
    public void runTestWithUserCodeFailure() throws IOException {
      mockTestProvider.validateResultWith((result, t) -> {
        assertThat(t, is(instanceOf(AssertionError.class)));
        assertThat(t.getMessage(), is("Pact Test function failed with an exception: expected:" +
          "<{responsetest=true, name=harry}> but was:<{responsetest=true, name=fred}>"));
        assertThat(result, is(instanceOf(PactVerificationResult.Error.class)));
        PactVerificationResult.Error error = (PactVerificationResult.Error) result;
        assertThat(error.getMockServerState(), is(instanceOf(PactVerificationResult.Ok.INSTANCE.getClass())));
        assertThat(error.getError(), is(instanceOf(AssertionError.class)));
      });
      Assert.assertEquals(new ConsumerClient(mockTestProvider.getUrl()).options("/second"), 200);
      Map expectedResponse = new HashMap();
      expectedResponse.put("responsetest", true);
      expectedResponse.put("name", "fred");
      assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerification(value = "test_provider")
    public void runTestWithPactError() throws IOException {
      mockTestProvider.validateResultWith((result, t) -> {
          assertThat(t, is(instanceOf(AssertionError.class)));
          assertThat(t.getMessage(), startsWith("The following requests were not received:\n" +
            "\tmethod: GET\n" +
            "\tpath: /"));
          assertThat(result, is(instanceOf(PactVerificationResult.ExpectedButNotReceived.class)));
          PactVerificationResult.ExpectedButNotReceived error = (PactVerificationResult.ExpectedButNotReceived) result;
          assertThat(error.getExpectedRequests(), hasSize(1));
          Request request = error.getExpectedRequests().get(0);
          assertThat(request.getPath(), is("/"));
      });
      Assert.assertEquals(new ConsumerClient(mockTestProvider.getUrl()).options("/second"), 200);
    }
}
