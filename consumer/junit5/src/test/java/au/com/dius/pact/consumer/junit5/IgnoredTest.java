package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.support.Response;
import au.com.dius.pact.core.support.SimpleHttp;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
class IgnoredTest {

  private static final String EXPECTED_USER_ID = "abcdefghijklmnop";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json.*";
  private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
  private static final String SOME_SERVICE_USER = "/some-service/user/";

  private static String user() {
    return """
      {
        "username": "bbarke",
        "password": "123456",
        "firstname": "Brent",
        "lastname": "Barker",
        "booleam": "true"
      }
      """;
  }

  @Pact(provider = "multitest_provider", consumer= "browser_consumer")
  RequestResponsePact createFragment1(PactDslWithProvider builder) {
    return builder
      .given("An env")
      .uponReceiving("a new user")
        .path("/some-service/users")
        .method("POST")
        .body(user())
        .matchHeader(CONTENT_TYPE, APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
      .willRespondWith()
        .status(201)
        .matchHeader("Location", "http(s)?://\\w+:\\d+//some-service/user/\\w{36}$")
      .given("An automation user with id: " + EXPECTED_USER_ID)
      .uponReceiving("existing user lookup")
        .path(SOME_SERVICE_USER + EXPECTED_USER_ID)
        .method("GET")
      .willRespondWith()
        .status(200)
        .matchHeader("Content-Type", APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
        .body(user())
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createFragment1", pactVersion = PactSpecVersion.V3)
  void runTest1(MockServer mockServer) {
    SimpleHttp http = new SimpleHttp(mockServer.getUrl());

    Response response = http.post("/some-service/users", user(), "application/json");
    assertThat(response.getStatusCode(), is(equalTo(201)));
    assertThat(response.getHeaders().get("location").get(0).contains(SOME_SERVICE_USER), is(true));

    response = http.get(SOME_SERVICE_USER + EXPECTED_USER_ID);
    assertThat(response.getStatusCode(), is(equalTo(200)));
  }

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
  @PactTestFor(pactMethod = "createFragment2", pactVersion = PactSpecVersion.V3)
  @Disabled
  void runTest2(MockServer mockServer) throws IOException {
    String response = Request.put(mockServer.getUrl() + "/numbertest")
      .addHeader("Accept", "application/json")
      .bodyString("{\"name\": \"harry\",\"data\": 1234.0 }", ContentType.APPLICATION_JSON)
      .execute()
      .returnContent()
      .asString();
    assertThat(response, is(equalTo("{\"responsetest\": true, \"name\": \"harry\",\"data\": 1234.0 }")));
  }

  @Pact(provider = "multitest_provider", consumer = "test_consumer")
  RequestResponsePact getUsersFragment(PactDslWithProvider builder) {
    DslPart body = new PactDslJsonArray()
      .maxArrayLike(5)
      .uuid("id")
      .stringType("userName")
      .stringType("email")
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
  @PactTestFor(pactMethod = "getUsersFragment", pactVersion = PactSpecVersion.V3)
  @DisabledIf(value = "runTest3Check")
  void runTest3(MockServer mockServer) throws IOException {
    HttpResponse response = Request.get(mockServer.getUrl() + "/idm/user")
      .execute()
      .returnResponse();
    assertThat(response.getCode(), is(equalTo(200)));
  }

  boolean runTest3Check() { return true; }

  @Pact(provider = "multitest_provider", consumer = "test_consumer")
  @Disabled
  RequestResponsePact getUsersFragment3(PactDslWithProvider builder) {
    return builder
      .uponReceiving("get all users")
      .path("/idm/user")
      .method("GET")
      .willRespondWith()
      .status(404)
      .toPact();
  }
}
