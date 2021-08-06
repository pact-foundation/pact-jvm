package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup;
import au.com.dius.pact.core.model.matchingrules.MatchingRules;
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.model.matchingrules.TypeMatcher;
import com.google.common.collect.Sets;
import org.apache.hc.client5.http.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class Defect266Test {

  @Rule
  public PactProviderRule provider = new PactProviderRule("266_provider", this);

  @Pact(provider = "266_provider", consumer = "test_consumer")
  public RequestResponsePact getUsersFragment(PactDslWithProvider builder) {
    DslPart body = new PactDslJsonArray().maxArrayLike(5)
      .uuid("id")
      .stringType("userName")
      .stringType("email")
      .closeObject();
    RequestResponsePact pact = builder
      .given("a user with an id named 'user' exists")
      .uponReceiving("get all users for max")
      .path("/idm/user")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(body)
      .toPact();
    MatchingRules matchingRules = pact.getInteractions().get(0)
      .asSynchronousRequestResponse().getResponse().getMatchingRules();
    Map<String, MatchingRuleGroup> bodyMatchingRules = matchingRules.rulesForCategory("body").getMatchingRules();
    assertThat(bodyMatchingRules.keySet(), is(equalTo(Sets.newHashSet("$[0][*].userName", "$[0][*].id", "$[0]",
      "$[0][*].email"))));
    assertThat(bodyMatchingRules.get("$[0][*].userName").getRules().get(0), is(equalTo(TypeMatcher.INSTANCE)));
    assertThat(bodyMatchingRules.get("$[0][*].id").getRules().get(0),
      is(equalTo(new RegexMatcher("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))));
    assertThat(bodyMatchingRules.get("$[0]").getRules().get(0), is(equalTo(new MaxTypeMatcher(5))));
    assertThat(bodyMatchingRules.get("$[0][*].email").getRules().get(0), is(equalTo(TypeMatcher.INSTANCE)));
    return pact;
  }

  @Pact(provider = "266_provider", consumer = "test_consumer")
  public RequestResponsePact getUsersFragment2(PactDslWithProvider builder) {
    DslPart body = new PactDslJsonArray().minArrayLike(5)
      .uuid("id")
      .stringType("userName")
      .stringType("email")
      .closeObject();
    RequestResponsePact pact = builder
      .given("a user with an id named 'user' exists")
      .uponReceiving("get all users for min")
      .path("/idm/user")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(body)
      .toPact();
    MatchingRules matchingRules = pact.getInteractions().get(0)
      .asSynchronousRequestResponse().getResponse().getMatchingRules();
    Map<String, MatchingRuleGroup> bodyMatchingRules = matchingRules.rulesForCategory("body").getMatchingRules();
    assertThat(bodyMatchingRules.keySet(), is(equalTo(Sets.newHashSet("$[0][*].userName", "$[0][*].id", "$[0]",
      "$[0][*].email"))));
    assertThat(bodyMatchingRules.get("$[0][*].userName").getRules().get(0), is(equalTo(TypeMatcher.INSTANCE)));
    assertThat(bodyMatchingRules.get("$[0][*].id").getRules().get(0),
      is(equalTo(new RegexMatcher("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))));
    assertThat(bodyMatchingRules.get("$[0]").getRules().get(0), is(equalTo(new MinTypeMatcher(5))));
    assertThat(bodyMatchingRules.get("$[0][*].email").getRules().get(0), is(equalTo(TypeMatcher.INSTANCE)));
    return pact;
  }

  @Test
  @PactVerification(value = "266_provider", fragment = "getUsersFragment")
  public void runTest() throws IOException {
    Request.get(provider.getUrl() + "/idm/user").execute().returnContent().asString();
  }

  @Test
  @PactVerification(value = "266_provider", fragment = "getUsersFragment2")
  public void runTest2() throws IOException {
    Request.get(provider.getUrl() + "/idm/user").execute().returnContent().asString();
  }
}
