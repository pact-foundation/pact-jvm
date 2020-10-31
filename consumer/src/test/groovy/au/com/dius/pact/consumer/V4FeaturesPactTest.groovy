package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.V4PactFeaturesException
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest
import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runMessageConsumerTest

class V4FeaturesPactTest {

  @Test
  void testFailIfV4FeaturesUsedWithV3Spec() {
    RequestResponsePact pact = ConsumerPactBuilder
      .consumer('Some Consumer')
      .hasPactWith('Some Provider')
      .uponReceiving('a request to say Hello')
        .path('/hello')
        .method('POST')
        .body(new PactDslJsonBody().unorderedArray('items').string('harry'))
      .willRespondWith()
        .status(200)
      .toPact()

    MockProviderConfig config = MockProviderConfig.createDefault()
    PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun<Boolean>() {
      @Override
      Boolean run(MockServer mockServer, PactTestExecutionContext context) {
        new ConsumerClient(mockServer.url).post('/hello', '{"items": ["harry"]}', ContentType.APPLICATION_JSON)
        true
      }
    })

    assert result instanceof PactVerificationResult.Error
    assert result.error instanceof V4PactFeaturesException
  }

  @Test
  void testPassesIfV4FeaturesUsedWithV4Spec() {
    Pact pact = ConsumerPactBuilder
      .consumer('V4 Some Consumer')
      .hasPactWith('V4 Some Provider')
      .uponReceiving('a request to say Hello')
      .path('/hello')
      .method('POST')
      .body(new PactDslJsonBody().unorderedArray('items').string('harry'))
      .willRespondWith()
      .status(200)
      .toPact()

    MockProviderConfig config = MockProviderConfig.createDefault(PactSpecVersion.V4)
    PactVerificationResult result = runConsumerTest(pact, config, new PactTestRun<Boolean>() {
      @Override
      Boolean run(MockServer mockServer, PactTestExecutionContext context) {
        new ConsumerClient(mockServer.url).post('/hello', '{"items": ["harry"]}', ContentType.APPLICATION_JSON)
        true
      }
    })

    assert result instanceof PactVerificationResult.Ok
  }

  @Test
  void testRunMessageConsumerFailsIfV4FeaturesUsedWithV3Spec() {
    PactDslJsonBody content = new PactDslJsonBody()
    content.unorderedArray('items').string('harry')

    Pact pact = MessagePactBuilder
      .consumer('async_ping_consumer')
      .hasPactWith('async_ping_provider')
      .expectsToReceive('a message')
      .withContent(content)
      .toPact()

    PactVerificationResult result = runMessageConsumerTest(pact, PactSpecVersion.V3) { messages, context ->
      true
    }

    assert result instanceof PactVerificationResult.Error
    assert result.error instanceof V4PactFeaturesException
  }

  @Test
  @Disabled('Not implemented')
  void testRunMessageConsumerPassesIfV4FeaturesUsedWithV4Spec() {
    PactDslJsonBody content = new PactDslJsonBody()
    content.unorderedArray('items').string('harry')

    MessagePact pact = MessagePactBuilder
      .consumer('v4_async_ping_consumer')
      .hasPactWith('v4_async_ping_provider')
      .expectsToReceive('a message')
      .withContent(content)
      .toPact()

    PactVerificationResult result = runMessageConsumerTest(pact, PactSpecVersion.V4) { messages, context ->
      true
    }

    assert result instanceof PactVerificationResult.Ok
  }
}
