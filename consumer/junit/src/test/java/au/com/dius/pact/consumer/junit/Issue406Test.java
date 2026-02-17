package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.support.Either;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.management.UnixOperatingSystemMXBean;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class Issue406Test {
  private static final String CONSUMER_NAME = "example-consumer";
  private static final String PROVIDER_NAME = "example-provider";

  private UserProxy userProxy;

  static class User {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String email;

    public User() { }

    public Integer getUserId() {
      return userId;
    }

    public User setUserId(Integer userId) {
      this.userId = userId;
      return this;
    }

    public String getFirstName() {
      return firstName;
    }

    public User setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public String getLastName() {
      return lastName;
    }

    public User setLastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public String getEmail() {
      return email;
    }

    public User setEmail(String email) {
      this.email = email;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      User user = (User) o;
      return Objects.equals(userId, user.userId) && Objects.equals(firstName, user.firstName) &&
        Objects.equals(lastName, user.lastName) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, firstName, lastName, email);
    }
  }

  static enum ApplicationExceptionReason {
    NOT_FOUND, IO_ERROR, UNKNOWN

  }

  static class ApplicationException extends Exception {
    private ApplicationExceptionReason reason;

    public ApplicationException(String message, ApplicationExceptionReason reason) {
      super(message);
      this.reason = reason;
    }

    public ApplicationExceptionReason getReason() {
      return reason;
    }
  }

  static class UserProxy {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Either<ApplicationException, User> get(int accountId, int userId) {
      try {
        HttpResponse httpResponse = Request.get("http://localhost:8081/" + accountId + "/users/" + userId)
          .execute().returnResponse();
        if (httpResponse.getCode() == 404) {
          return Either.a(new ApplicationException("Failed to execute request", ApplicationExceptionReason.NOT_FOUND));
        } else if (httpResponse.getCode() >= 500) {
          return Either.a(new ApplicationException("Server error", ApplicationExceptionReason.UNKNOWN));
        } else {
          HttpEntityContainer entityContainer = (HttpEntityContainer) httpResponse;
          return Either.b(objectMapper.readValue(entityContainer.getEntity().getContent(), User.class));
        }
      } catch (IOException e) {
        return Either.a(new ApplicationException("Failed to execute request", ApplicationExceptionReason.IO_ERROR));
      }
    }
  }

  private static void outputOpenFileCount(String prefix) {
    try {
      OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
      if (os instanceof UnixOperatingSystemMXBean) {
        System.out.println(prefix + ": Number of open fd: " + ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
      }
    } catch (Exception e) {
      System.out.println("Failed get the open file count");
      e.printStackTrace();
    }
  }

  @Rule
  public PactProviderRule provider = new PactProviderRule(PROVIDER_NAME, "localhost", 8081, this);

  @BeforeClass
  public static void beforeAll() {
    outputOpenFileCount("beforeAll");
  }

  @Before
  public void beforeEach() {
    userProxy = new UserProxy();
    outputOpenFileCount("beforeEach");
  }

  @After
  public void afterEach() {
    outputOpenFileCount("afterEach");
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getUser(PactDslWithProvider builder) {
    Map<String, String> responseHeaders = new HashMap<>();
    responseHeaders.put("Content-Type", "application/json;charset=UTF-8");

    String responseBody = new JSONObject()
      .put("userId", 2)
      .put("firstName", "Bob")
      .put("lastName", "Smith")
      .put("email", "bsmith@domain.com")
      .toString();

    return builder
      .given("there is one user in the database")
      .uponReceiving("a request for user 2 on account 1")
      .path("/1/users/2")
      .method("GET")
      .willRespondWith()
      .status(200)
      .headers(responseHeaders)
      .body(responseBody)
      .toPact();
  }

  @Test
  @PactVerification(fragment = "getUser")
  public void shouldGetAUser() {
    Either<ApplicationException, User> result = userProxy.get(1, 2);

    assertThat("result is success", result.isB(), is(true));
    User expectedUser = new User()
      .setUserId(2)
      .setFirstName("Bob")
      .setLastName("Smith")
      .setEmail("bsmith@domain.com");
    assertThat(result.unwrapB("Expected a user"), is(equalTo(expectedUser)));
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getMissingUser(PactDslWithProvider builder) {
    return builder
      .given("there is one user in the database")
      .uponReceiving("a request for user 3 on account 1")
      .path("/1/users/3")
      .method("GET")
      .willRespondWith()
      .status(404)
      .toPact();
  }

  @Test
  @PactVerification(fragment = "getMissingUser")
  public void shouldReturnANotFoundApplicationExceptionWhenTheUserIsNotFound() {
    Either<ApplicationException, User> result = userProxy.get(1, 3);
    assertThat("result is failure", result.isA(), is(true));
    assertThat(result.unwrapA("Expected an error").getReason(), is(ApplicationExceptionReason.NOT_FOUND));
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getMissingAccount(PactDslWithProvider builder) {
    return builder
      .given("there is one user in the database")
      .uponReceiving("a request for user 1 on account 2")
      .path("/2/users/1")
      .method("GET")
      .willRespondWith()
      .status(404)
      .toPact();
  }

  @Test
  @PactVerification(fragment = "getMissingAccount")
  public void shouldReturnANotFoundApplicationExceptionWhenTheAccountIsNotFound() {
    Either<ApplicationException, User> result = userProxy.get(2, 1);
    assertThat("result is failure", result.isA(), is(true));
    assertThat(result.unwrapA("Expected an error").getReason(), is(ApplicationExceptionReason.NOT_FOUND));
  }

  @Pact(consumer = CONSUMER_NAME)
  public RequestResponsePact getServerError(PactDslWithProvider builder) {
    return builder
      .given("the user service is down")
      .uponReceiving("a request for a user that results in a server error")
      .path("/1/users/4")
      .method("GET")
      .willRespondWith()
      .status(500)
      .toPact();
  }

  @Test
  @PactVerification(fragment = "getServerError")
  public void shouldReturnAnUnknownApplicationExceptionWhenAServerErrorOccurs() {
    Either<ApplicationException, User> result = userProxy.get(1, 4);
    assertThat("result is failure", result.isA(), is(true));
    assertThat(result.unwrapA("Expected an error").getReason(), is(ApplicationExceptionReason.UNKNOWN));
  }
}
