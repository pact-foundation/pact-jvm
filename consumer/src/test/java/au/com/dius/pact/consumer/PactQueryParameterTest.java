package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.BasePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PactQueryParameterTest {
  @Test
  public void testMatchQueryWithSimpleQueryParameter() throws Throwable {
    // Given a pact expecting GET /hello?q=simple created using the .matchQuery(...) method
    String path = "/hello";
    String parameterName = "q";
    String decodedValue = "simple";
    String encodedValue = "simple";
    String encodedFullPath = path + "?" + parameterName + "=" + encodedValue;

    BasePact pact = ConsumerPactBuilder
      .consumer("Some Consumer")
      .hasPactWith("Some Provider")
      .uponReceiving(encodedFullPath)
      .path(path)
      .matchQuery(parameterName, ".+", decodedValue)
      .method("GET")
      .willRespondWith()
      .status(200)
      .toPact();

    // When sending the request, we expect no errors
    verifyRequestMatches(pact, encodedFullPath);
  }

  @Test
  public void testMatchQueryWithComplexQueryParameter() throws Throwable {
    // Given a pact expecting GET /hello?q=query%20containing%20%26%20and%20%3F%20characters
    // created using the .matchQuery(...) method
    String path = "/hello";
    String parameterName = "q";
    String decodedValue = "query containing & and ? characters";
    String encodedValue = "query%20containing%20%26%20and%20%3F%20characters";
    String encodedFullPath = path + "?" + parameterName + "=" + encodedValue;

    BasePact pact = ConsumerPactBuilder
      .consumer("Some Consumer")
      .hasPactWith("Some Provider")
      .uponReceiving(encodedFullPath)
      .path(path)
      .matchQuery(parameterName, ".+", decodedValue)
      .method("GET")
      .willRespondWith()
      .status(200)
      .toPact();

    // When sending the request, we expect no errors
    verifyRequestMatches(pact, encodedFullPath);
  }

  @Test
  public void testEncodedQueryWithSimpleQueryParameter() throws Throwable {
    // Given a pact expecting GET /hello?q=simple, created using the .query(...) method
    String path = "/hello";
    String parameterName = "q";
    String encodedValue = "simple";
    String encodedQuery = parameterName + "=" + encodedValue;
    String encodedFullPath = path + "?" + encodedQuery;

    BasePact pact = ConsumerPactBuilder
      .consumer("Some Consumer")
      .hasPactWith("Some Provider")
      .uponReceiving(encodedFullPath)
      .path(path)
      .encodedQuery(encodedQuery)
      .method("GET")
      .willRespondWith()
      .status(200)
      .toPact();

    // When sending the request, we expect no errors
    verifyRequestMatches(pact, encodedFullPath);
  }

  @Test
  public void testEncodedQueryWithComplexQueryParameter() throws Throwable {
    // Given a pact expecting GET /hello?q=query%20containing%20%26%20and%20%3F%20characters,
    // created using the .query(...) method
    String path = "/hello";
    String parameterName = "q";
    String encodedValue = "query%20containing%20%26%20and%20%3F%20characters";
    String encodedQuery = parameterName + "=" + encodedValue;
    String encodedFullPath = path + "?" + encodedQuery;

    BasePact pact = ConsumerPactBuilder
      .consumer("Some Consumer")
      .hasPactWith("Some Provider")
      .uponReceiving(encodedFullPath)
      .path(path)
      .encodedQuery(encodedQuery)
      .method("GET")
      .willRespondWith()
      .status(200)
      .toPact();

    // When sending the request, we expect no errors
    verifyRequestMatches(pact, encodedFullPath);
  }

  private void verifyRequestMatches(BasePact pact, String fullPath) {
    MockProviderConfig config = MockProviderConfig.createDefault();
    PactVerificationResult result = runConsumerTest(pact, config, (mockServer, context) -> {
      String uri = mockServer.getUrl() + fullPath;

      Request.get(uri).execute().handleResponse(httpResponse -> {
        String content = EntityUtils.toString(httpResponse.getEntity());
        if (httpResponse.getCode() == 500) {
          Map map = new ObjectMapper().readValue(content, Map.class);
          Assert.fail((String) map.get("error"));
        }
        return null;
      });

      return true;
    });
    if (result instanceof PactVerificationResult.Error) {
      Throwable error = ((PactVerificationResult.Error) result).getError();
      if (error instanceof RuntimeException) {
        throw (RuntimeException) error;
      } else {
        throw new RuntimeException(error);
      }
    }
    assertThat(result, is(instanceOf(PactVerificationResult.Ok.class)));
  }
}
