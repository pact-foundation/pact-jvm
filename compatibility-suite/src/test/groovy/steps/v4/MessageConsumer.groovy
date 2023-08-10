package steps.v4

import au.com.dius.pact.consumer.dsl.MessageInteractionBuilder
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given

class MessageConsumer {
  SharedV4PactData v4Data
  MessageInteractionBuilder builder

  MessageConsumer(SharedV4PactData v4Data) {
    this.v4Data = v4Data
  }

  @Before
  void before(Scenario scenario) {
    v4Data.scenarioId = scenario.id
  }

  @After
  void after(Scenario scenario) {
    if (!scenario.failed) {
      def dir = "build/compatibility-suite/v4/${v4Data.scenarioId}" as File
      dir.deleteDir()
    }
  }

  @Given('a message interaction is being defined for a consumer test')
  void a_message_interaction_is_being_defined_for_a_consumer_test() {
    builder = new MessageInteractionBuilder('a message', [], [])
    v4Data.builderCallbacks << {
      builder.build()
    }
  }
}
