package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.support.json.JsonParser;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

// Issue #1783: JSON without a charset must be read and written as UTF-8, whatever the platform default charset is
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "1783_provider", pactVersion = PactSpecVersion.V3)
public class JsonBodyWithoutCharsetTest {

  private static final String NAME = "Привет, café";

  @Pact(consumer = "1783_consumer")
  public RequestResponsePact pact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("a JSON request without a charset")
      .path("/greeting")
      .method("POST")
      .headers("Content-Type", "application/json")
      .body(new PactDslJsonBody().stringValue("name", NAME))
      .willRespondWith()
      .status(200)
      .headers(Map.of("Content-Type", "application/json"))
      .body(new PactDslJsonBody().stringValue("name", NAME))
      .toPact();
  }

  @Test
  void jsonWithoutCharsetIsUtf8(MockServer mockServer) throws Exception {
    byte[] request = ("{\"name\":\"" + NAME + "\"}").getBytes(StandardCharsets.UTF_8);
    ClassicHttpResponse response = (ClassicHttpResponse) Request.post(mockServer.getUrl() + "/greeting")
      .bodyByteArray(request, ContentType.create("application/json"))
      .execute()
      .returnResponse();

    assertThat(response.getCode(), is(200));
    String body = new String(EntityUtils.toByteArray(response.getEntity()), StandardCharsets.UTF_8);
    assertThat(JsonParser.parseString(body).get("name").asString(), is(NAME));
  }
}
