package steps.v4

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.StringSource
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.VerificationResult
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import steps.shared.CompatibilitySuiteWorld
import steps.shared.SharedHttpProvider
import steps.shared.VerificationData

class HttpProvider {
  CompatibilitySuiteWorld world
  SharedHttpProvider sharedProvider
  VerificationData verificationData

  HttpProvider(CompatibilitySuiteWorld world, SharedHttpProvider sharedProvider, VerificationData verificationData) {
    this.world = world
    this.sharedProvider = sharedProvider
    this.verificationData = verificationData
  }

  @Given('a Pact file for interaction {int} is to be verified, but is marked pending')
  void a_pact_file_for_interaction_is_to_be_verified_but_is_marked_pending(Integer index) {
    def interaction = world.interactions[index - 1].asV4Interaction()
    interaction.pending = true
    V4Pact pact = new V4Pact(
      new Consumer('v3-compatibility-suite-c'),
      new Provider('p'),
      [ interaction ]
    )
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V4)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  @Given('a Pact file for interaction {int} is to be verified with the following comments:')
  void a_pact_file_for_interaction_is_to_be_verified_with_the_following_comments(Integer index, DataTable dataTable) {
    def interaction = world.interactions[index - 1].asV4Interaction()

    for (comment in dataTable.asMaps()) {
      switch (comment['type']) {
        case 'text':
          interaction.addTextComment(comment['comment'])
          break
        case 'testname':
          interaction.setTestName(comment['comment'])
          break
      }
    }

    V4Pact pact = new V4Pact(
      new Consumer('v3-compatibility-suite-c'),
      new Provider('p'),
      [ interaction ]
    )
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V4)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  @Then('there will be a pending {string} error')
  void there_will_be_a_pending_error(String error) {
    assert verificationData.verificationResults.any {
      it instanceof VerificationResult.Failed && it.pending && it.description == error
    }
  }

  @Then('the comment {string} will have been printed to the console')
  void the_comment_will_have_been_printed_to_the_console(String comment) {
    def comments = verificationData.verifier.reporters.first().events.find {
      it.comments
    }
    assert comments && comments.comments.text.values.any { it == comment }
  }

  @Then('the {string} will displayed as the original test name')
  void the_will_displayed_as_the_original_test_name(String name) {
    def comments = verificationData.verifier.reporters.first().events.find {
      it.comments
    }
    assert comments && comments.comments.testname == name
  }
}
