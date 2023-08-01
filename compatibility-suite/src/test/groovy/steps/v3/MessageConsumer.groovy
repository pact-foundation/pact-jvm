package steps.v3

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.JsonUtils
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.cucumber.datatable.DataTable
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runMessageConsumerTest
import static au.com.dius.pact.core.support.Json.toJson
import static steps.shared.SharedSteps.configureBody
import static steps.shared.SharedSteps.determineContentType
import static steps.v3.Generators.matchTypeOfElement

@SuppressWarnings(['ThrowRuntimeException'])
class MessageConsumer {
  MessagePactBuilder builder
  Pact pact
  List<? extends Interaction> receivedMessages
  PactVerificationResult result
  String scenarioId
  Pact loadedPact

  @Before
  void before(Scenario scenario) {
    scenarioId = scenario.id
  }

  @After
  void after(Scenario scenario) {
    if (!scenario.failed) {
      def dir = "build/compatibility-suite/v3/$scenarioId" as File
      dir.deleteDir()
    }
  }

  @ParameterType('first|second|third')
  @SuppressWarnings(['SpaceAfterOpeningBrace'])
  static Integer numType(String numType) {
    switch (numType) {
      case 'first' -> yield 0
      case 'second'-> yield 1
      case 'third' -> yield 2
      default -> throw new IllegalArgumentException("$numType is not a valid number type")
    }
  }

  @Given('a message integration is being defined for a consumer test')
  void a_message_integration_is_being_defined_for_a_consumer_test() {
    builder = new MessagePactBuilder(PactSpecVersion.V3)
      .consumer('V3-message-consumer')
      .hasPactWith('V3-message-provider')
  }

  @Given('the message payload contains the {string} JSON document')
  void the_message_payload_contains_the_json_document(String fixture) {
    String contents
    if (fixture.endsWith('.json')) {
      File f = new File("pact-compatibility-suite/fixtures/${fixture}")
      contents = f.text
    } else {
      File f = new File("pact-compatibility-suite/fixtures/${fixture}.json")
      contents = f.text
    }
    builder.expectsToReceive('a message')
      .withContent(contents, 'application/json')
  }

  @Given('a message is defined')
  void a_message_is_defined() {
    builder.expectsToReceive('a message')
  }

  @Given('the message configured with the following:')
  void the_message_configured_with_the_following(DataTable dataTable) {
    builder.expectsToReceive('a message')
    def message = builder.messages.last()
    def entry = dataTable.entries().first()

    OptionalBody body = OptionalBody.missing()
    def metadata = message.contents.metadata
    def matchingRules = message.contents.matchingRules
    def generators = new au.com.dius.pact.core.model.generators.Generators()

    if (entry['body']) {
      def part = configureBody(entry['body'], determineContentType(entry['body'], message.contentType.toString()))
      body = part.body
      metadata.putAll(part.headers)
    }

    if (entry['generators']) {
      JsonValue json
      if (entry['generators'].startsWith('JSON:')) {
        json = JsonParser.INSTANCE.parseString(entry['generators'][5..-1])
      } else {
        File contents = new File("pact-compatibility-suite/fixtures/${entry['generators']}")
        contents.withInputStream {
          json = JsonParser.INSTANCE.parseStream(it)
        }
      }
      def g = au.com.dius.pact.core.model.generators.Generators.fromJson(json)
      def category = g.categoryFor(Category.BODY)
      if (category) {
        generators.addGenerators(Category.CONTENT, category)
      }
      category = g.categoryFor(Category.METADATA)
      if (category) {
        generators.addGenerators(Category.METADATA, category)
      }
    }

    if (entry['metadata']) {
      def jsonValue = JsonParser.INSTANCE.parseString(entry['metadata'])
      metadata.putAll(jsonValue.asObject().entries)
    }

    message.contents = new MessageContents(body, metadata, matchingRules, generators)
  }

  @Given('the message contains the following metadata:')
  void the_message_contains_the_following_metadata(DataTable dataTable) {
    builder.withMetadata { mdBuilder ->
      for (entry in dataTable.asMap()) {
        if (entry.value.startsWith('JSON: ')) {
          def json = JsonParser.INSTANCE.parseString(entry.value[5..-1])
          mdBuilder.add(entry.key, json)
        } else {
          mdBuilder.add(entry.key, entry.value)
        }
      }
    }
  }

  @Given('a provider state {string} for the message is specified')
  void a_provider_state_for_the_message_is_specified(String state) {
    builder.given(state)
  }

  @Given('a provider state {string} for the message is specified with the following data:')
  void a_provider_state_for_the_message_is_specified_with_the_following_data(String state, DataTable dataTable) {
    def entry = dataTable.entries()
      .first()
      .collectEntries {
        [it.key, JsonParser.parseString(it.value).unwrap()]
      }
    builder = builder.given(state, entry)
  }

  @When('the message is successfully processed')
  void the_message_is_successfully_processed() {
    pact = builder.toPact()
    result = runMessageConsumerTest(pact, PactSpecVersion.V3) { i, context ->
      receivedMessages = i
      context.pactFolder = "build/compatibility-suite/v3/$scenarioId"
      true
    }
  }

  @Then('the consumer test will have passed')
  void consumer_test_will_have_passed() {
    assert result instanceof PactVerificationResult.Ok
  }

  @Then('the received message payload will contain the {string} JSON document')
  void the_received_message_payload_will_contain_the_json_document(String fixture) {
    File contents = new File("pact-compatibility-suite/fixtures/${fixture}.json")
    assert receivedMessages.first().asMessage().contents.value == contents.bytes
  }

  @Then('the received message content type will be {string}')
  void the_received_message_content_type_will_be(String contentType) {
    assert receivedMessages.first().asMessage().contentType.toString() == contentType
  }

  @Then('a Pact file for the message interaction will have been written')
  void a_pact_file_for_the_message_interaction_will_have_been_written() {
    def dir = "build/compatibility-suite/v3/$scenarioId" as File
    def pactFile = new File(dir, 'V3-message-consumer-V3-message-provider.json')
    assert pactFile.exists()
    def loadPactJson = new JsonSlurper().parse(pactFile)
    assert loadPactJson['metadata']['pactSpecification']['version'] == '3.0.0'
    loadedPact = DefaultPactReader.INSTANCE.loadPact(pactFile)
    assert loadedPact instanceof MessagePact
  }

  @Then('the pact file will contain {int} message interaction(s)')
  void the_pact_file_will_contain_message_interaction(Integer messages) {
    assert loadedPact.asMessagePact().unwrap().messages.size() == messages
  }

  @Then('the {numType} message in the pact file will contain the {string} document')
  void the_first_message_in_the_pact_file_will_contain_the_document(Integer index, String fixture) {
    def message = loadedPact.asMessagePact().unwrap().messages[index]
    File contents = new File("pact-compatibility-suite/fixtures/${fixture}")
    if (fixture.endsWith('.json')) {
      def json = new JsonSlurper().parse(contents)
      assert message.contents.value == JsonOutput.toJson(json).bytes
    } else {
      assert message.contents.value == contents.bytes
    }
  }

  @Then('the {numType} message in the pact file content type will be {string}')
  void the_first_message_in_the_pact_file_content_type_will_be(Integer index, String contentType) {
    def message = loadedPact.asMessagePact().unwrap().messages[index]
    assert message.contentType.toString() == contentType
  }

  @When('the message is NOT successfully processed with a {string} exception')
  void the_message_is_not_successfully_processed_with_a_exception(String error) {
    pact = builder.toPact()
    result = runMessageConsumerTest(pact, PactSpecVersion.V3) { i, context ->
      receivedMessages = i
      context.pactFolder = "build/compatibility-suite/v3/$scenarioId"
      throw new RuntimeException(error)
    }
  }

  @Then('the consumer test will have failed')
  void the_consumer_test_will_have_failed() {
    assert result instanceof PactVerificationResult.Error
  }

  @Then('the consume test error will be {string}')
  void the_consume_test_error_will_be_blah(String error) {
    assert result.error.message == error
  }

  @Then('a Pact file for the message interaction will NOT have been written')
  void a_pact_file_for_the_message_interaction_will_not_have_been_written() {
    def dir = "build/compatibility-suite/v3/$scenarioId" as File
    def pactFile = new File(dir, 'V3-message-consumer-V3-message-provider.json')
    assert !pactFile.exists()
  }

  @Then('the received message metadata will contain {string} == {string}')
  void the_received_message_metadata_will_contain(String key, String value) {
    if (value.startsWith('JSON: ')) {
      def json = JsonParser.INSTANCE.parseString(value[5..-1])
      assert receivedMessages.first().asMessage().metadata[key] == json
    } else {
      assert receivedMessages.first().asMessage().metadata[key] == value
    }
  }

  @Then('the {numType} message in the pact file will contain the message metadata {string} == {string}')
  void the_first_message_in_the_pact_file_will_contain_the_message_metadata(Integer index, String key, String value) {
    def message = loadedPact.asMessagePact().unwrap().messages[index]
    if (value.startsWith('JSON: ')) {
      def json = JsonParser.INSTANCE.parseString(value[5..-1])
      assert message.metadata[key] == Json.INSTANCE.toMap(json)
    } else {
      assert message.metadata[key] == value
    }
  }

  @When('the {numType} message in the pact file will contain {int} provider state(s)')
  void the_first_message_in_the_pact_file_will_contain_provider_states(Integer index, Integer states) {
    def message = loadedPact.asMessagePact().unwrap().messages[index]
    assert message.providerStates.size() == states
  }

  @When('the {numType} message in the Pact file will contain provider state {string}')
  void the_first_message_in_the_pact_file_will_contain_provider_state(Integer index, String stateName) {
    def message = loadedPact.asMessagePact().unwrap().messages[index]
    assert message.providerStates.find { it.name == stateName }
  }

  @Then('the provider state {string} for the message will contain the following parameters:')
  void the_provider_state_for_the_message_will_contain_the_following_parameters(String state, DataTable dataTable) {
    def entry = dataTable.entries().first()['parameters']
    def params = JsonParser.parseString(entry).asObject().entries.collectEntries {
      [it.key, Json.INSTANCE.fromJson(it.value)]
    }
    def message = loadedPact.asMessagePact().unwrap().messages.first()
    def providerState = message.providerStates.find { it.name == state }
    assert providerState.params == params
  }

  @Then('the message contents for {string} will have been replaced with a(n) {string}')
  void the_message_contents_for_will_have_been_replaced_with_an(String path, String valueType) {
    def message = pact.asMessagePact().unwrap().messages.first()
    def originalJson = JsonParser.parseString(message.contents.valueAsString())
    def contents = receivedMessages.first().asMessage().contents
    def generatedJson = JsonParser.parseString(contents.valueAsString())
    def originalElement = JsonUtils.INSTANCE.fetchPath(originalJson, path)
    def element = JsonUtils.INSTANCE.fetchPath(generatedJson, path)
    assert originalElement != element
    matchTypeOfElement(valueType, element)
  }

  @Then('the received message metadata will contain {string} replaced with a(n) {string}')
  void the_received_message_metadata_will_contain_replaced_with_an(String key, String valueType) {
    def message = pact.asMessagePact().unwrap().messages.first()
    def original = message.metadata[key]
    def generated = receivedMessages.first().asMessage().metadata[key]
    assert generated != original
    matchTypeOfElement(valueType, toJson(generated))
  }
}
