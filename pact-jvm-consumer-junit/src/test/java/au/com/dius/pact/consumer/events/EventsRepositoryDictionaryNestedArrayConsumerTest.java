package au.com.dius.pact.consumer.events;

import au.com.dius.pact.consumer.MatcherTestUtils;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.FeatureToggles;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * THIS IS A TEST FOR https://github.com/DiUS/pact-jvm/issues/401
 */
public class EventsRepositoryDictionaryNestedArrayConsumerTest {

  private static final Integer PORT = 8092;

  @Rule
  public PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2("EventsProvider", "localhost", PORT, this);

  @Before
  public void setup() {
    FeatureToggles.toggleFeature("pact.feature.matchers.useMatchValuesMatcher", true);
  }

  @After
  public void cleanup() {
    FeatureToggles.reset();
  }

  @Pact(provider = "EventsProvider", consumer = "EventsConsumerDictionaryNestedArray")
  public RequestResponsePact createPact(PactDslWithProvider builder) {

    DslPart body = new PactDslJsonBody()
      .object("events")
      //key is dynamic (i.e. think dictionary or java map
      //see https://github.com/DiUS/pact-jvm/issues/313
      //see https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-consumer-junit
      .eachKeyMappedToAnArrayLike("ant") //broken, see pact-jvm issue 401
        .stringType("title", "ant")
      //we dont care about other attributes here. neither does pact :-);
      ;

    RequestResponsePact pact = builder
      .given("initialStateForEventsTest")
      .uponReceiving("a request to get events keyed by title")
      .path("/dictionaryNestedArray")
      .headers("Accept", ContentType.APPLICATION_JSON.toString())
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(body)
      .toPact();

    MatcherTestUtils.assertResponseMatcherKeysEqualTo(pact, "body",
      "$.events.*",
      "$.events.*[*].title"
    );

//    assertThat(pact.getInteractions().get(0).getResponse().getMatchingRules().rulesForCategory("body").getMatchingRules(),
//      is(equalTo(new HashMap<>())));

    return pact;
  }

  @Test
  @PactVerification(value = "EventsProvider")
  public void runTest() {
    Map<String, Map<String, List<Event>>> events = new EventsRepository("http://localhost:" + PORT).getEventsMapNestedArray();
    assertThat(events.entrySet(), hasSize(1));
    assertThat(events.get("events").get("ant").get(0).getTitle(), is("ant"));
  }

}
