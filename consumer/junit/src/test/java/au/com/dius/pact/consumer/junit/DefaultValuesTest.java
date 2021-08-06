package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.dsl.PactDslRequestWithoutPath;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

public class DefaultValuesTest {

    private static final String APPLICATION_JSON = "application/json";

    @Rule
    public PactProviderRule provider = new PactProviderRule("DefaultValuesProvider", this);

    @DefaultRequestValues
    public void defaultRequestValues(PactDslRequestWithoutPath request) {
      request.headers("Content-Type", "application/json").method("GET");
    }

    @Pact(consumer = "DefaultValuesConsumer")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
      RequestResponsePact pact = builder.given("status200")
        .uponReceiving("Get object")
        .path("/path")
        .willRespondWith()
        .status(200)
        .uponReceiving("Download")
        .path("/path2")
        .matchQuery("source_filename", "[\\S\\s]+[\\S]+", "filename")
        .willRespondWith()
        .status(200)
        .toPact();

      assertThat(pact.getInteractions().get(0).asSynchronousRequestResponse()
        .getRequest().getHeaders(), hasEntry("Content-Type",
        Collections.singletonList("application/json")));
      assertThat(pact.getInteractions().get(1)
        .asSynchronousRequestResponse().getRequest().getHeaders(), hasEntry("Content-Type",
        Collections.singletonList("application/json")));

      return pact;
    }

    @Test
    @PactVerification
    public void testWithDefaultValues() throws IOException {
        Response response = Request.get(provider.getUrl() + "/path")
                                       .addHeader("Accept", APPLICATION_JSON)
                                       .addHeader("Content-Type", APPLICATION_JSON)
                                       .execute();

        assertThat(response.returnResponse().getCode(), is(200));

        response = Request.get(provider.getUrl() + "/path2?source_filename=test%20file")
          .addHeader("Accept", APPLICATION_JSON)
          .addHeader("Content-Type", APPLICATION_JSON)
          .execute();

        assertThat(response.returnResponse().getCode(), is(200));
    }
}
