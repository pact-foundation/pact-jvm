package steps.v3

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.StringSource
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.MessageAndMetadata
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import steps.shared.VerificationData

import static steps.shared.SharedSteps.configureBody
import static steps.shared.SharedSteps.determineContentType

class MessageProvider {
  VerificationData verificationData
  def messages = [:]

  def messageFactory = { String desc ->
    messages[desc]
  }

  MessageProvider(VerificationData verificationData) {
    this.verificationData = verificationData
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

  @Given('a Pact file for {string}:{string} is to be verified')
  void a_pact_file_for_is_to_be_verified(String name, String fixture) {
    Message message = configureMessage(name, fixture)
    Pact pact = new MessagePact(new Provider('p'),
      new Consumer('v3-compatibility-suite-c'), [message])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V3)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  static Message configureMessage(String name, String fixture) {
    def part = configureBody(fixture, determineContentType(fixture, null))
    def message = new Message(name, [], part.body)
    message.metadata.putAll(part.headers.collectEntries {
      if (it.value.size() == 0) {
        [it.key, null]
      } else if (it.value.size() == 1) {
        [it.key, it.value.first()]
      } else {
        [it.key, it.value]
      }
    })
    message
  }

  @Given('a Pact file for {string}:{string} is to be verified with provider state {string}')
  void a_pact_file_for_is_to_be_verified_with_provider_state(String name, String fixture, String state) {
    Message message = configureMessage(name, fixture)
    message.providerStates << new ProviderState(state)
    Pact pact = new MessagePact(new Provider('p'),
      new Consumer('v3-compatibility-suite-c'), [message])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V3)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  @Given('a provider is started that can generate the {string} message with {string} and the following metadata:')
  void a_provider_is_started_that_can_generate_the_message_with_the_following_metadata(
    String name,
    String fixture,
    DataTable dataTable
  ) {
    def part = configureBody(fixture, determineContentType(fixture, null))

    def entries = part.headers.collectEntries {
      if (it.value.size() == 0) {
        [it.key, null]
      } else if (it.value.size() == 1) {
        [it.key, it.value.first()]
      } else {
        [it.key, it.value]
      }
    }

    for (entry in dataTable.asMap()) {
      if (entry.value.startsWith('JSON: ')) {
        def json = JsonParser.INSTANCE.parseString(entry.value[5..-1])
        entries[entry.key] = Json.INSTANCE.fromJson(json)
      } else {
        entries[entry.key] = entry.value
      }
    }

    def message = new MessageAndMetadata(part.body.value, entries)
    messages[name] = message

    verificationData.providerInfo = new ProviderInfo('p')
    verificationData.providerInfo.verificationType = PactVerification.RESPONSE_FACTORY
    verificationData.providerInfo.stateChangeTeardown = true
    verificationData.responseFactory = messageFactory
  }

  @Given('a Pact file for {string}:{string} is to be verified with the following metadata:')
  void a_pact_file_for_is_to_be_verified_with_the_following_metadata(
    String name,
    String fixture,
    DataTable dataTable
  ) {
    Message message = configureMessage(name, fixture)

    for (entry in dataTable.asMap()) {
      if (entry.value.startsWith('JSON: ')) {
        def json = JsonParser.INSTANCE.parseString(entry.value[5..-1])
        message.metadata[entry.key] = Json.INSTANCE.fromJson(json)
      } else {
        message.metadata[entry.key] = entry.value
      }
    }

    Pact pact = new MessagePact(new Provider('p'),
      new Consumer('v3-compatibility-suite-c'), [message])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V3)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  @Given('a Pact file for {string} is to be verified with the following:')
  void a_pact_file_for_is_to_be_verified_with_the_following(String name, DataTable dataTable) {
    Message message = new Message(name)

    for (row in dataTable.asLists()) {
      switch (row[0]) {
        case 'body': {
          message = configureMessage(name, row[1])
          break
        }
        case 'matching rules': {
          JsonValue json
          if (row[1].startsWith('JSON:')) {
            json = JsonParser.INSTANCE.parseString(row[1][5..-1])
          } else {
            File contents = new File("pact-compatibility-suite/fixtures/${row[1]}")
            contents.withInputStream {
              json = JsonParser.INSTANCE.parseStream(it)
            }
          }
          message.matchingRules = MatchingRulesImpl.fromJson(json)
          break
        }
        case 'metadata': {
          row[1].split(';').collect {
            it.trim().split('=')
          }.forEach {
            if (it[1].startsWith('JSON: ')) {
              def json = JsonParser.INSTANCE.parseString(it[1][5..-1])
              message.metadata[it[0]] = Json.INSTANCE.fromJson(json)
            } else {
              message.metadata[it[0]] = it[1]
            }
          }
          break
        }
      }
    }

    Pact pact = new MessagePact(new Provider('p'),
      new Consumer('v3-compatibility-suite-c'), [message])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V3)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }
}
