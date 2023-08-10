package steps.v4

import au.com.dius.pact.consumer.dsl.HttpInteractionBuilder
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given

class HttpConsumer {
  HttpInteractionBuilder httpBuilder
  SharedV4PactData v4Data

  HttpConsumer(SharedV4PactData v4Data) {
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

  @Given('an HTTP interaction is being defined for a consumer test')
  void an_http_interaction_is_being_defined_for_a_consumer_test() {
    httpBuilder = new HttpInteractionBuilder('HTTP interaction', [], [])
    v4Data.builderCallbacks << {
      httpBuilder.build()
    }
  }

  @Given('a key of {string} is specified for the HTTP interaction')
  void a_key_of_is_specified(String key) {
    httpBuilder.key(key)
  }

  @Given('the HTTP interaction is marked as pending')
  void the_interaction_is_marked_as_pending() {
    httpBuilder.pending(true)
  }

  @Given('a comment {string} is added to the HTTP interaction')
  void a_comment_is_added(String value) {
    httpBuilder.comment(value)
  }
}
