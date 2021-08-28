package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.Matchers;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "v4_test_provider")
public class V4PactBuilderTest {

  @Pact(provider="v4_test_provider", consumer="v4_test_consumer")
  public V4Pact httpInteraction(PactBuilder builder) {
    return builder
      .usingLegacyDsl()
      .given("good state")
      .comment("This is a comment")
      .uponReceiving("V4 PactProviderTest test interaction")
      .path("/")
      .method("GET")
      .comment("Another comment")
      .willRespondWith()
      .status(200)
      .body("{\"responsetest\": true, \"version\": \"v3\"}")
      .comment("This is also a comment")
      .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(pactMethod = "httpInteraction")
  void runHttpTest(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl()).execute().returnResponse();
    assertThat(httpResponse.getCode(), is(200));
    assertThat(new String(httpResponse.getEntity().getContent().readAllBytes()),
      is(equalTo("{\"responsetest\": true, \"version\": \"v3\"}")));
  }

  @Pact(consumer = "v4_test_consumer")
  V4Pact messageInteraction(PactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody();
    body.stringValue("testParam1", "value1");
    body.stringValue("testParam2", "value2");

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("destination", Matchers.regexp("\\w+\\d+", "X001"));

    return builder.usingLegacyMessageDsl()
      .given("SomeProviderState")
      .expectsToReceive("a test message")
      .withMetadata(metadata)
      .withContent(body)
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "messageInteraction")
  void runMessageTest(V4Pact pact) {
    V4Interaction.AsynchronousMessage message = (V4Interaction.AsynchronousMessage) pact.getInteractions()
      .stream().filter(i -> i instanceof V4Interaction.AsynchronousMessage).findFirst().get();
    assertThat(message.getContents().getContents().valueAsString(), Is.is("{\"testParam1\":\"value1\",\"testParam2\":\"value2\"}"));
    assertThat(message.getContents().getMetadata(), hasEntry("destination", "X001"));
  }
}
