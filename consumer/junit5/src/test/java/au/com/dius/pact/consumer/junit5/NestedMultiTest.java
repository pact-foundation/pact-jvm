package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import groovy.json.JsonOutput;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "multitest_provider", pactVersion = PactSpecVersion.V3)
@PactDirectory("build/pacts/multi-test")
class NestedMultiTest {

  static final String EXPECTED_USER_ID = "abcdefghijklmnop";
  static final String CONTENT_TYPE = "Content-Type";
  static final String APPLICATION_JSON = "application/json.*";
  static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
  static final String SOME_SERVICE_USER = "/some-service/user/";

  static Map<String, String> user() {
    return Map.of("username", "bbarke",
      "password", "123456",
      "firstname", "Brent",
      "lastname", "Barker",
      "boolean", "true"
    );
  }

  @Nested
  class Test1 {

    @Pact(provider = "multitest_provider", consumer = "browser_consumer")
    RequestResponsePact createFragment1(PactDslWithProvider builder) {
      return builder
        .given("An env")
        .uponReceiving("a new user")
          .path("/some-service/users")
          .method("POST")
          .body(JsonOutput.toJson(user()))
          .matchHeader(CONTENT_TYPE, APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
        .willRespondWith()
          .status(201)
          .matchHeader("Location", "http(s)?://\\w+:\\d+//some-service/user/\\w{36}$",
            "http://localhost:8080/some-service/user/" + EXPECTED_USER_ID)
        .given("An automation user with id: " + EXPECTED_USER_ID)
        .uponReceiving("existing user lookup")
          .path(SOME_SERVICE_USER + EXPECTED_USER_ID)
          .method("GET")
        .willRespondWith()
          .status(200)
          .matchHeader("Content-Type", APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
          .body(JsonOutput.toJson(user()))
        .toPact();
    }

    @Test
    void runTest1(MockServer mockServer) throws IOException {
      ClassicHttpResponse postResponse = (ClassicHttpResponse) Request.post(mockServer.getUrl() + "/some-service/users")
        .bodyString(JsonOutput.toJson(user()), ContentType.APPLICATION_JSON)
        .execute().returnResponse();

      assertThat(postResponse.getCode(), is(equalTo(201)));
      assertThat(postResponse.getFirstHeader("Location").getValue(),
        is(equalTo("http://localhost:8080/some-service/user/abcdefghijklmnop")));


      ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl() + SOME_SERVICE_USER + EXPECTED_USER_ID)
        .execute().returnResponse();
      assertThat(httpResponse.getCode(), is(equalTo(200)));
    }
  }

  @Nested
  class Test2 {
    @Pact(provider= "multitest_provider", consumer= "test_consumer")
    RequestResponsePact createFragment2(PactDslWithProvider builder) {
      return builder
        .given("test state")
        .uponReceiving("A request with double precision number")
          .path("/numbertest")
          .method("PUT")
          .body("{\"name\": \"harry\",\"data\": 1234.0 }", "application/json")
        .willRespondWith()
          .status(200)
          .body("{\"responsetest\": true, \"name\": \"harry\",\"data\": 1234.0 }", "application/json")
        .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createFragment2")
    void runTest2(MockServer mockServer) throws IOException {
      assert Request.put(mockServer.getUrl() + "/numbertest")
        .addHeader("Accept", "application/json")
        .bodyString("{\"name\": \"harry\",\"data\": 1234.0 }", ContentType.APPLICATION_JSON)
        .execute().returnContent().asString().equals("{\"responsetest\": true, \"name\": \"harry\",\"data\": 1234.0 }");
    }

    @Pact(provider = "multitest_provider", consumer = "test_consumer")
    RequestResponsePact getUsersFragment(PactDslWithProvider builder) {
      DslPart body = PactDslJsonArray.arrayMaxLike(5)
        .uuid("id", "7b374cc6-d644-11eb-a613-4ffac1365f0e")
        .stringType("userName", "Bob")
        .stringType("email", "bob@bobville")
        .closeObject();
      return builder
        .given("a user with an id named 'user' exists")
        .uponReceiving("get all users for max")
          .path("/idm/user")
          .method("GET")
        .willRespondWith()
          .status(200)
          .body(body)
        .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getUsersFragment")
    void runTest3(MockServer mockServer) throws IOException {
      assertThat(Request.get(mockServer.getUrl() + "/idm/user").execute().returnContent().asString(),
        is("[{\"email\":\"bob@bobville\",\"id\":\"7b374cc6-d644-11eb-a613-4ffac1365f0e\",\"userName\":\"Bob\"}]"));
    }
  }
}
