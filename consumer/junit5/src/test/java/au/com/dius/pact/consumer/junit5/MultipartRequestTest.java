package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.MultipartBuilder;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "MultipartProvider")
public class MultipartRequestTest {
  @Pact(consumer = "MultipartConsumer")
  public V4Pact pact(PactBuilder builder) {
    return builder
      .expectsToReceiveHttpInteraction("multipart request", interactionBuilder ->
        interactionBuilder
          .withRequest(requestBuilder -> requestBuilder
            .path("/path")
            .method("POST")
            .body(new MultipartBuilder()
              .filePart("file-part", "RAT.JPG", getClass().getResourceAsStream("/RAT.JPG"), "image/jpeg")
              .jsonPart("json-part", LambdaDsl.newJsonBody(body -> body
                .stringMatcher("a", "\\w+", "B")
                .integerType("c", 100)).build())
            )
          )
          .willRespondWith(responseBuilder -> responseBuilder.status(201))
      )
      .toPact();
  }

  @Test
  @PactTestFor
  void testArticles(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.post(mockServer.getUrl() + "/path")
      .body(
        MultipartEntityBuilder.create()
          .addBinaryBody("file-part", getClass().getResourceAsStream("/RAT.JPG"), ContentType.IMAGE_JPEG, "RAT.JPG")
          .addTextBody("json-part", "{\"a\": \"B\", \"c\": 1234}", ContentType.APPLICATION_JSON)
          .build()
      )
      .execute()
      .returnResponse();
    assertThat(httpResponse.getCode(), is(equalTo(201)));
  }
}
