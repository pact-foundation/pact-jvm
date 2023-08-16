package steps.v4

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.StringSource
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.MessageAndMetadata
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import steps.shared.SharedHttpProvider
import steps.shared.VerificationData

import static steps.shared.SharedSteps.configureBody
import static steps.shared.SharedSteps.determineContentType

class MessageProvider {
  SharedHttpProvider sharedProvider
  VerificationData verificationData
  def messages = [:]

  def messageFactory = { String desc ->
    messages[desc]
  }

  MessageProvider(SharedHttpProvider sharedProvider, VerificationData verificationData) {
    this.sharedProvider = sharedProvider
    this.verificationData = verificationData
  }

  static V4Interaction.AsynchronousMessage configureMessage(String name, String fixture) {
    def part = configureBody(fixture, determineContentType(fixture, null))
    def contents = new MessageContents(part.body)
    contents.metadata.putAll(part.headers.collectEntries {
      if (it.value.size() == 0) {
        [it.key, null]
      } else if (it.value.size() == 1) {
        [it.key, it.value.first()]
      } else {
        [it.key, it.value]
      }
    })
    new V4Interaction.AsynchronousMessage(name, [], contents)
  }

  @Given('a provider is started that can generate the {string} message with {string}')
  void a_provider_is_started_that_can_generate_the_message(String name, String fixture) {
    def part = configureBody(fixture, determineContentType(fixture, null))
    def message = new MessageAndMetadata(part.body.value, part.headers.collectEntries {
      if (it.value.size() == 0) {
        [it.key, null]
      } else if (it.value.size() == 1) {
        [it.key, it.value.first()]
      } else {
        [it.key, it.value]
      }
    })
    messages[name] = message

    verificationData.providerInfo = new ProviderInfo('p')
    verificationData.providerInfo.verificationType = PactVerification.RESPONSE_FACTORY
    verificationData.providerInfo.stateChangeTeardown = true
    verificationData.responseFactory = messageFactory
  }

  @Given('a Pact file for {string}:{string} is to be verified, but is marked pending')
  void a_pact_file_for_is_to_be_verified_but_is_marked_pending(String name, String fixture) {
    def message = configureMessage(name, fixture)
    message.pending = true
    Pact pact = new V4Pact(new Consumer('v4-compatibility-suite-c'), new Provider('p'), [message])
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

  @Given('a Pact file for {string}:{string} is to be verified with the following comments:')
  void a_pact_file_for_is_to_be_verified_with_the_following_comments(String name, String fixture, DataTable dataTable) {
    def message = configureMessage(name, fixture)

    for (comment in dataTable.asMaps()) {
      switch (comment['type']) {
        case 'text':
          message.addTextComment(comment['comment'])
          break
        case 'testname':
          message.setTestName(comment['comment'])
          break
      }
    }

    Pact pact = new V4Pact(new Consumer('v4-compatibility-suite-c'), new Provider('p'), [message])
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
}
