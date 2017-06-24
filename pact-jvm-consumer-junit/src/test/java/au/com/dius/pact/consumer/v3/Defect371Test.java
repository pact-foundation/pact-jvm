package au.com.dius.pact.consumer.v3;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.MessagePactProviderRule;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import groovy.json.JsonSlurper;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Defect371Test {

    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule("provider1", this);

    @Rule
    public MessagePactProviderRule mockProvider2 = new MessagePactProviderRule("provider2", this);

    private byte[] currentMessage;

    public void setMessage(byte[] messageContents) {
    currentMessage = messageContents;
  }

    @Pact(provider = "provider1", consumer = "Defect371")
    public MessagePact createPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("testParam1", "value1");
        body.stringValue("testParam2", "value2");

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("contentType", "application/json");

        return builder.given("SomeProviderState")
          .expectsToReceive("a test message")
          .withMetadata(metadata)
          .withContent(body)
          .toPact();
    }

    @Pact(provider = "provider2", consumer = "Defect371")
    public MessagePact createPact2(MessagePactBuilder builder) {
      PactDslJsonBody body = new PactDslJsonBody();
      body.stringValue("testParam1", "value3");
      body.stringValue("testParam2", "value4");

      Map<String, String> metadata = new HashMap<String, String>();
      metadata.put("contentType", "application/json");

      return builder.given("Some Other Provider State")
        .expectsToReceive("a test message")
        .withMetadata(metadata)
        .withContent(body)
        .toPact();
    }

    @Test
    @PactVerification(value = "provider1", fragment = "createPact")
    public void test() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put("testParam1", "value1");
        expected.put("testParam2", "value2");
        assertThat((Map<String, String>) new JsonSlurper().parse(currentMessage), is(expected));
    }

    @Test
    @PactVerification(value = "provider2", fragment = "createPact2")
    public void test2() throws Exception {
      Map<String, String> expected = new HashMap<>();
      expected.put("testParam1", "value3");
      expected.put("testParam2", "value4");
      assertThat((Map<String, String>) new JsonSlurper().parse(currentMessage), is(expected));
    }
}
