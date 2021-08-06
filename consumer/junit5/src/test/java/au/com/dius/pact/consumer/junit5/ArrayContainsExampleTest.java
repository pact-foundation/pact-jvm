package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import groovy.json.JsonSlurper;
import org.apache.hc.client5.http.fluent.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static au.com.dius.pact.consumer.dsl.DslPart.regex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "Siren Order Service", pactVersion = PactSpecVersion.V3)
public class ArrayContainsExampleTest {
  @Pact(consumer = "Order Processor")
  public RequestResponsePact articles(PactDslWithProvider builder) {
    final DslPart body = new PactDslJsonBody()
      .array("class")
        .stringValue("entity")
      .closeArray()
      .eachLike("entities")
        .array("class")
          .stringValue("entity")
        .closeArray()
        .array("rel")
          .stringValue("item")
        .closeArray()
        .object("properties")
          .integerType("id", 1234)
        .closeObject()
        .array("links")
          .object()
            .array("rel")
              .stringValue("self")
            .closeArray()
            .matchUrl("href", "http://localhost:9000", "orders", regex("\\d+", "1234"))
          .closeObject()
        .closeArray()
        .arrayContaining("actions")
          .object()
            .stringValue("name", "update")
            .stringValue("method", "PUT")
            .matchUrl("href", "http://localhost:9000", "orders", regex("\\d+", "1234"))
          .closeObject()
          .object()
            .stringValue("name", "delete")
            .stringValue("method", "DELETE")
            .matchUrl("href", "http://localhost:9000", "orders", regex("\\d+", "1234"))
          .closeObject()
        .closeArray()
      .closeArray()
      .array("links")
        .object()
          .array("rel")
            .stringValue("self")
          .closeArray()
          .matchUrl("href", "http://localhost:9000", "orders")
        .closeObject()
      .closeArray();

    return builder.uponReceiving("get all orders")
      .path("/orders")
      .method("GET")
      .willRespondWith()
      .status(200)
      .headers(Map.of("Content-Type", "application/vnd.siren+json"))
      .body(body)
      .toPact();
  }

  @Test
  @PactTestFor
  void testArticles(MockServer mockServer) throws IOException {
    final String response = Request.get(mockServer.getUrl() + "/orders")
      .addHeader("Accept", "application/vnd.siren+json")
      .execute()
      .returnContent()
      .asString(Charset.defaultCharset());

    final Map<String, Object> jsonResponse = (Map<String, Object>) new JsonSlurper().parseText(response);

    assertThat(jsonResponse.keySet(), is(equalTo(Set.of("class", "entities", "links"))));
    List entities = (List) jsonResponse.get("entities");
    assertThat(entities.size(), is(1));
    Map<String, Object> entity = (Map<String, Object>) entities.get(0);
    assertThat(entity.keySet(), is(equalTo(Set.of("rel", "links", "class", "actions", "properties"))));
    List<Map<String, Object>> actions = (List<Map<String, Object>>) entity.get("actions");
    assertThat(actions.size(), is(2));
    for (Map<String, Object> action: actions) {
      assertThat(action.keySet(), is(equalTo(Set.of("method", "name", "href"))));
    }
  }
}
