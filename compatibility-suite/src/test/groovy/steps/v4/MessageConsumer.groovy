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

  @Given('a key of {string} is specified for the message interaction')
  void a_key_of_is_specified_for_the_message_interaction(String key) {
    builder.key(key)
  }

  @Given('the message interaction is marked as pending')
  void the_message_interaction_is_marked_as_pending() {
    builder.pending(true)
  }

  @Given('a comment {string} is added to the message interaction')
  void a_comment_is_added_to_the_message_interaction(String comment) {
    builder.comment(comment)
  }
}
