package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.matchingrules.MatchingRule;
import au.com.dius.pact.model.matchingrules.MatchingRules;
import au.com.dius.pact.model.matchingrules.MaxTypeMatcher;
import au.com.dius.pact.model.matchingrules.MinTypeMatcher;
import au.com.dius.pact.model.matchingrules.RegexMatcher;
import au.com.dius.pact.model.matchingrules.TypeMatcher;
import com.google.common.collect.Sets;
import groovy.json.JsonSlurper;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class Defect266Test {

  @Rule
  public PactProviderRule provider = new PactProviderRule("266_provider", "localhost", 8080, this);

  @Pact(provider = "266_provider", consumer = "test_consumer")
  public PactFragment getUsersFragment(PactDslWithProvider builder) {
    DslPart body = new PactDslJsonArray().maxArrayLike(5)
      .uuid("id")
      .stringType("userName")
      .stringType("email")
      .closeObject();
    PactFragment pactFragment = builder
      .given("a user with an id named 'user' exists")
      .uponReceiving("get all users for max")
      .path("/idm/user")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(body)
      .toFragment();
    MatchingRules matchingRules = pactFragment.interactions().head().getResponse().getMatchingRules();
    Map<String, List<MatchingRule>> bodyMatchingRules = matchingRules.rulesForCategory("body").getMatchingRules();
    assertThat(bodyMatchingRules.keySet(), is(equalTo((Set<String>) Sets.newHashSet("$[0][*].userName", "$[0][*].id", "$[0]",
      "$[0][*].email"))));
    assertThat(bodyMatchingRules.get("$[0][*].userName").get(0), is(equalTo((MatchingRule) new TypeMatcher())));
    assertThat(bodyMatchingRules.get("$[0][*].id").get(0),
      is(equalTo((MatchingRule) new RegexMatcher("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))));
    assertThat(bodyMatchingRules.get("$[0]").get(0), is(equalTo((MatchingRule) new MaxTypeMatcher(5))));
    assertThat(bodyMatchingRules.get("$[0][*].email").get(0), is(equalTo((MatchingRule) new TypeMatcher())));
    return pactFragment;
  }

  @Pact(provider = "266_provider", consumer = "test_consumer")
  public PactFragment getUsersFragment2(PactDslWithProvider builder) {
    DslPart body = new PactDslJsonArray().minArrayLike(5)
      .uuid("id")
      .stringType("userName")
      .stringType("email")
      .closeObject();
    PactFragment pactFragment = builder
      .given("a user with an id named 'user' exists")
      .uponReceiving("get all users for min")
      .path("/idm/user")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(body)
      .toFragment();
    MatchingRules matchingRules = pactFragment.interactions().head().getResponse().getMatchingRules();
    Map<String, List<MatchingRule>> bodyMatchingRules = matchingRules.rulesForCategory("body").getMatchingRules();
    assertThat(bodyMatchingRules.keySet(), is(equalTo((Set<String>) Sets.newHashSet("$[0][*].userName", "$[0][*].id", "$[0]",
      "$[0][*].email"))));
    assertThat(bodyMatchingRules.get("$[0][*].userName").get(0), is(equalTo((MatchingRule) new TypeMatcher())));
    assertThat(bodyMatchingRules.get("$[0][*].id").get(0),
      is(equalTo((MatchingRule) new RegexMatcher("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))));
    assertThat(bodyMatchingRules.get("$[0]").get(0), is(equalTo((MatchingRule) new MinTypeMatcher(5))));
    assertThat(bodyMatchingRules.get("$[0][*].email").get(0), is(equalTo((MatchingRule) new TypeMatcher())));
    return pactFragment;
  }

  @Test
  @PactVerification(value = "266_provider", fragment = "getUsersFragment")
  public void runTest() throws IOException {
    Request.Get("http://localhost:8080/idm/user").execute().returnContent().asString();
  }

  @Test
  @PactVerification(value = "266_provider", fragment = "getUsersFragment2")
  public void runTest2() throws IOException {
    Request.Get("http://localhost:8080/idm/user").execute().returnContent().asString();
  }
}
