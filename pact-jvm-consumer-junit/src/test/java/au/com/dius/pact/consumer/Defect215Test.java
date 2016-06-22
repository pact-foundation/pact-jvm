package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * @author bbarker
 * @since 2/17/16
 */
public class Defect215Test {
  private static final int PORT = 8988;

  private static final String MY_SERVICE = "MY_service";
  private static final String EXPECTED_USER_ID = "abcdefghijklmnop";
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
  public PactFragment createFragment(PactDslWithProvider builder) {

    return builder
      .given("An env")
      .uponReceiving("a new user")
        .path("/some-service/users")
        .method("POST")
        .body(getUser())
        .matchHeader("Content-Type", "application/json.*", "application/json; charset=UTF-8")
      .willRespondWith()
        .status(201)
        .matchHeader("Location", "http(s)?://\\S+:\\d+//some-service/user/\\S{36}$")
      .given("An automation user with id: " + EXPECTED_USER_ID)
      .uponReceiving("existing user lookup")
        .path("/some-service/user/" + EXPECTED_USER_ID)
        .method("GET")
        .matchHeader("Content-Type", "application/json.*", "application/json; charset=UTF-8")
      .willRespondWith()
        .status(200)
        .matchHeader("Content-Type", "application/json.*", "application/json; charset=UTF-8")
        .body(getUser())
      .toFragment();
  }

  @Test
  @PactVerification(MY_SERVICE)
  public void runTest() {
    RestAssured
      .given()
      .port(mockProvider.getConfig().port())
      .contentType(ContentType.JSON)
      .body(getUser())
      .post("/some-service/users")
      .then()
      .statusCode(201)
      .header("location", Matchers.containsString("/some-service/user/"));

    RestAssured.reset();

    RestAssured
      .given()
      .port(mockProvider.getConfig().port())
      .contentType(ContentType.JSON)
      .get("/some-service/user/" + EXPECTED_USER_ID)
      .then()
      .statusCode(200);

    RestAssured.reset();
  }
}
