package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.Matchers;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

// Issue #1813
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "eachkeylike_provider", pactVersion = PactSpecVersion.V4)
public class EachKeyLikeTest {

  @Pact(consumer="eachkeylike__consumer")
  public V4Pact createFragment(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A request")
      .path("/")
      .method("POST")
      .body(newJsonBody(body ->
        body.object("a", aObj -> {
          aObj.eachKeyMatching(Matchers.regexp("prop\\d+", "prop1"));
          aObj.eachValueMatching("prop1", propObj -> propObj.stringType("value", "x"));
        })).build())
      .willRespondWith()
      .status(200)
      .toPact(V4Pact.class);
  }

  @Test
  void runTest(MockServer mockServer) throws IOException {
    String json = "{\n" +
      "  \"a\": {\n" +
      "    \"prop1\": {\n" +
      "       \"value\": \"x\"\n" +
      "    },\n" +
      "    \"prop2\": {\n" +
      "      \"value\": \"y\"\n" +
      "    }\n" +
      " }\n" +
      "}";
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.post(mockServer.getUrl())
      .body(new StringEntity(json, ContentType.APPLICATION_JSON))
      .execute()
      .returnResponse();
    assertThat(httpResponse.getCode(), is(200));

// This should make the test fail
//    String json2 = "{\n" +
//      "  \"a\": {\n" +
//      "    \"prop1\": {\n" +
//      "       \"value\": \"x\"\n" +
//      "    },\n" +
//      "    \"prop\": {\n" +
//      "      \"value\": \"y\"\n" +
//      "    }\n" +
//      " }\n" +
//      "}";
//    ClassicHttpResponse httpResponse2 = (ClassicHttpResponse) Request.post(mockServer.getUrl())
//      .body(new StringEntity(json2, ContentType.APPLICATION_JSON))
//      .execute()
//      .returnResponse();
//    assertThat(httpResponse2.getCode(), is(500));
  }
}
