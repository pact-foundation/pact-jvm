package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author bbarker
 * @since 2/17/16
 */
public class Defect215Test {
  private static final int PORT = 8988;

  private static final String MY_SERVICE = "MY_service";
  private static final String EXPECTED_USER_ID = "abcdefghijklmnop";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json.*";
  private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
  private static final String SOME_SERVICE_USER = "/some-service/user/";
  @Rule
  public PactProviderRule mockProvider = new PactProviderRule(MY_SERVICE, "localhost", PORT, this);

  private String getUser() {
    JSONObject usr = new JSONObject();
    usr.put("username","bbarke");
    usr.put("password","123456");
    usr.put("firstname","Brent");
    usr.put("lastname","Barker");
    usr.put("booleam", "true");

    return usr.toString();
  }

  @Pact(provider = MY_SERVICE, consumer="browser_consumer")
  public RequestResponsePact createFragment(PactDslWithProvider builder) {

    return builder
      .given("An env")
      .uponReceiving("a new user")
        .path("/some-service/users")
        .method("POST")
        .body(getUser())
        .matchHeader(CONTENT_TYPE, APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
      .willRespondWith()
        .status(201)
        .matchHeader("Location", "http(s)?://\\w+:\\d+//some-service/user/\\w{36}$")
      .given("An automation user with id: " + EXPECTED_USER_ID)
      .uponReceiving("existing user lookup")
        .path(SOME_SERVICE_USER + EXPECTED_USER_ID)
        .method("GET")
        .matchHeader("Content-Type", APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
      .willRespondWith()
        .status(200)
        .matchHeader("Content-Type", APPLICATION_JSON, APPLICATION_JSON_CHARSET_UTF_8)
        .body(getUser())
      .toPact();
  }

  @Test
  @PactVerification(MY_SERVICE)
  public void runTest() {
    RestAssured
      .given()
      .port(mockProvider.getPort())
      .contentType(ContentType.JSON)
      .body(getUser())
      .post("/some-service/users")
      .then()
      .statusCode(201)
      .header("location", Matchers.containsString(SOME_SERVICE_USER));

    RestAssured.reset();

    RestAssured
      .given()
      .port(mockProvider.getPort())
      .contentType(ContentType.JSON)
      .get(SOME_SERVICE_USER + EXPECTED_USER_ID)
      .then()
      .statusCode(200);

    RestAssured.reset();
  }
}
