package steps.v4

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.dsl.SynchronousMessageInteractionBuilder
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.JsonUtils
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.cucumber.datatable.DataTable
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runV4MessageConsumerTest
import static au.com.dius.pact.core.support.Json.toJson
import static steps.shared.SharedSteps.configureBody
import static steps.shared.SharedSteps.determineContentType
import static steps.shared.SharedSteps.matchTypeOfElement

class SyncMessageConsumer {
  SharedV4PactData v4Data
  SynchronousMessageInteractionBuilder builder
  V4Pact pact
  List<MessageContents> receivedMessages = []
  List<MessageContents> responseMessages = []
  PactVerificationResult result

  SyncMessageConsumer(SharedV4PactData v4Data) {
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

  @Given('a synchronous message interaction is being defined for a consumer test')
  void a_synchronous_message_interaction_is_being_defined_for_a_consumer_test() {
    builder = new SynchronousMessageInteractionBuilder('a message', [], [])
    v4Data.builderCallbacks << {
      builder.build()
    }
  }

  @Given('a key of {string} is specified for the synchronous message interaction')
  void a_key_of_is_specified_for_the_synchronous_message_interaction(String key) {
    builder.key(key)
  }

  @Given('the synchronous message interaction is marked as pending')
  void the_synchronous_message_interaction_is_marked_as_pending() {
    builder.pending(true)
  }

  @Given('a comment {string} is added to the synchronous message interaction')
  void a_comment_is_added_to_the_synchronous_message_interaction(String comment) {
    builder.comment(comment)
  }

  @Given('the message request payload contains the {string} JSON document')
  void the_message_request_payload_contains_the_json_document(String fixture) {
    String contents
    if (fixture.endsWith('.json')) {
      File f = new File("pact-compatibility-suite/fixtures/${fixture}")
      contents = f.text
    } else {
      File f = new File("pact-compatibility-suite/fixtures/${fixture}.json")
      contents = f.text
    }
    builder.withRequest {
      it.withContent(contents, 'application/json')
    }
  }

  @Given('the message response payload contains the {string} document')
  void the_message_response_payload_contains_the_document(String fixture) {
    def part = configureBody(fixture, determineContentType(fixture, null))
    builder.willRespondWith {
      it.contents.contents = part.body
      it.contents.metadata.putAll(part.headers)
      it
    }
  }

  @Given('the message request contains the following metadata:')
  void the_message_request_contains_the_following_metadata(DataTable dataTable) {
    builder.withRequest {
      it.withMetadata { mdBuilder ->
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
  }

  @Given('a provider state {string} for the synchronous message is specified')
  void a_provider_state_for_the_synchronous_message_is_specified(String state) {
    builder.state(state)
  }

  @Given('a provider state {string} for the synchronous message is specified with the following data:')
  void a_provider_state_for_the_synchronous_message_is_specified_with_the_following_data(
    String state,
    DataTable dataTable
  ) {
    def entry = dataTable.entries()
      .first()
      .collectEntries {
        [it.key, JsonParser.parseString(it.value).unwrap()]
      }
    builder = builder.state(state, entry)
  }

  @Given('the message request is configured with the following:')
  void the_message_request_is_configured_with_the_following(DataTable dataTable) {
    def message = builder.interaction
    def entry = dataTable.entries().first()

    OptionalBody body = OptionalBody.missing()
    def metadata = message.request.metadata
    def matchingRules = message.request.matchingRules
    def generators = new au.com.dius.pact.core.model.generators.Generators()

    if (entry['body']) {
      def part = configureBody(entry['body'], determineContentType(entry['body'],
        message.request.contentType.toString()))
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

    message.request = new MessageContents(body, metadata, matchingRules, generators)
  }

  @Given('the message response is configured with the following:')
  void the_message_response_is_configured_with_the_following(DataTable dataTable) {
    def message = builder.interaction
    def entry = dataTable.entries().first()

    OptionalBody body = OptionalBody.missing()
    def metadata = message.request.metadata
    def matchingRules = message.request.matchingRules
    def generators = new au.com.dius.pact.core.model.generators.Generators()

    if (entry['body']) {
      def part = configureBody(entry['body'], determineContentType(entry['body'],
        message.response.find()?.contentType?.toString()))
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

    message.response << new MessageContents(body, metadata, matchingRules, generators)
  }

  @When('the message is successfully processed')
  void the_message_is_successfully_processed() {
    v4Data.builderCallbacks.forEach {
      v4Data.pactBuilder.interactions.add(builder.build())
    }
    pact = v4Data.pactBuilder.toPact()
    result = runV4MessageConsumerTest(pact) { i, context ->
      receivedMessages.addAll(i.collect {
        if (it instanceof V4Interaction.AsynchronousMessage) {
          it.contents
        } else if (it instanceof V4Interaction.SynchronousMessages) {
          it.request
        }
      })
      responseMessages.addAll(i.collectMany {
        if (it instanceof V4Interaction.AsynchronousMessage) {
          [ it.contents ]
        } else if (it instanceof V4Interaction.SynchronousMessages) {
          it.response
        } else {
          []
        }
      })
      context.pactFolder = "build/compatibility-suite/v4/${v4Data.scenarioId}"
      true
    }
  }

  @Then('the received message payload will contain the {string} document')
  void the_received_message_payload_will_contain_the_document(String fixture) {
    def contents = configureBody(fixture, null)
    assert responseMessages.find { it.contents == contents.body } != null
  }

  @Then('the received message content type will be {string}')
  void the_received_message_content_type_will_be(String contentType) {
    assert responseMessages.find { it.contentType.toString() == contentType } != null
  }

  @Then('the consumer test will have passed')
  void the_consumer_test_will_have_passed() {
    assert result instanceof PactVerificationResult.Ok
  }

  @Then('a Pact file for the message interaction will have been written')
  void a_pact_file_for_the_message_interaction_will_have_been_written() {
    def dir = "build/compatibility-suite/v4/${v4Data.scenarioId}" as File
    def pactFile = new File(dir, 'V4 consumer-V4 provider.json')
    assert pactFile.exists()
    def loadPactJson = new JsonSlurper().parse(pactFile)
    assert loadPactJson['metadata']['pactSpecification']['version'] == '4.0'
    def loadedPact = DefaultPactReader.INSTANCE.loadPact(pactFile)
    assert loadedPact instanceof V4Pact
    v4Data.pact = loadedPact
  }

  @Then('the pact file will contain {int} interaction')
  void the_pact_file_will_contain_interaction(Integer num) {
    assert v4Data.pact.interactions.size() == num
  }

  @Then('the first interaction in the pact file will contain the {string} document as the request')
  void the_first_interaction_in_the_pact_file_will_contain_the_document_as_the_request(String fixture) {
    def interaction = v4Data.pact.interactions.first() as V4Interaction.SynchronousMessages
    def contents = configureBody(fixture, determineContentType(fixture, null))
    if (contents.jsonBody()) {
      def json1 = JsonOutput.prettyPrint(contents.body.valueAsString())
      def json2 = JsonOutput.prettyPrint(interaction.request.contents.valueAsString())
      assert json1 == json2
    } else {
      assert interaction.request.contents == contents.body
    }
  }

  @Then('the first interaction in the pact file request content type will be {string}')
  void the_first_interaction_in_the_pact_file_request_content_type_will_be(String contentType) {
    def interaction = v4Data.pact.interactions.first() as V4Interaction.SynchronousMessages
    assert interaction.request.contentType.toString() == contentType
  }

  @Then('the first interaction in the pact file will contain the {string} document as a response')
  void the_first_interaction_in_the_pact_file_will_contain_the_document_as_a_response(String fixture) {
    def interaction = v4Data.pact.interactions.first() as V4Interaction.SynchronousMessages
    def contents = configureBody(fixture, determineContentType(fixture, null))
    if (contents.jsonBody()) {
      def json1 = JsonOutput.prettyPrint(contents.body.valueAsString())
      def json2 = JsonOutput.prettyPrint(interaction.response.first().contents.valueAsString())
      assert json1 == json2
    } else {
      assert interaction.response.first().contents == contents.body
    }
  }

  @Then('the first interaction in the pact file response content type will be {string}')
  void the_first_interaction_in_the_pact_file_response_content_type_will_be(String contentType) {
    def interaction = v4Data.pact.interactions.first() as V4Interaction.SynchronousMessages
    assert interaction.response.first().contentType.toString() == contentType
  }

  @Then('the first interaction in the pact file will contain {int} response messages')
  void the_first_interaction_in_the_pact_file_will_contain_response_messages(Integer num) {
    def interaction = v4Data.pact.interactions.first() as V4Interaction.SynchronousMessages
    assert interaction.response.size() == num
  }

  @Then('the first interaction in the pact file will contain the {string} document as the {numType} response message')
  void the_first_interaction_in_the_pact_file_will_contain_the_document_as_the_first_response_message(
    String fixture,
    Integer index
  ) {
    def contents = configureBody(fixture, determineContentType(fixture, null))
    def interaction = v4Data.pact.interactions.first() as V4Interaction.SynchronousMessages
    if (contents.jsonBody()) {
      def json1 = JsonOutput.prettyPrint(contents.body.valueAsString())
      def json2 = JsonOutput.prettyPrint(interaction.response[index].contents.valueAsString())
      assert json1 == json2
    } else {
      assert interaction.response[index].contents == contents.body
    }
  }

  @Then('the first message in the pact file will contain the request message metadata {string} == {string}')
  void the_first_message_in_the_pact_file_will_contain_the_request_message_metadata(String key, String value) {
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    if (value.startsWith('JSON: ')) {
      def json = JsonParser.INSTANCE.parseString(value[5..-1])
      assert message.request.metadata[key] == json
    } else {
      assert message.request.metadata[key] == value
    }
  }

  @Then('the first message in the pact file will contain {int} provider state(s)')
  void the_first_message_in_the_pact_file_will_contain_provider_states(Integer states) {
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    assert message.providerStates.size() == states
  }

  @Then('the first message in the Pact file will contain provider state {string}')
  void the_first_message_in_the_pact_file_will_contain_provider_state(String state) {
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    assert message.providerStates.find { it.name == state } != null
  }

  @Then('the provider state {string} for the message will contain the following parameters:')
  void the_provider_state_for_the_message_will_contain_the_following_parameters(String state, DataTable dataTable) {
    def entry = dataTable.entries().first()['parameters']
    def params = JsonParser.parseString(entry).asObject().entries.collectEntries {
      [it.key, Json.INSTANCE.fromJson(it.value)]
    }
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    def providerState = message.providerStates.find { it.name == state }
    assert providerState.params == params
  }

  @Then('the message request contents for {string} will have been replaced with a(n) {string}')
  void the_message_contents_for_will_have_been_replaced_with_an(String path, String valueType) {
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    def originalJson = JsonParser.parseString(message.request.contents.valueAsString())
    def contents = receivedMessages.first().contents
    def generatedJson = JsonParser.parseString(contents.valueAsString())
    def originalElement = JsonUtils.INSTANCE.fetchPath(originalJson, path)
    def element = JsonUtils.INSTANCE.fetchPath(generatedJson, path)
    assert originalElement != element
    matchTypeOfElement(valueType, element)
  }

  @Then('the message response contents for {string} will have been replaced with a(n) {string}')
  void the_message_response_contents_for_will_have_been_replaced_with_an(String path, String valueType) {
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    def originalJson = JsonParser.parseString(message.response.first().contents.valueAsString())
    def contents = responseMessages.first().contents
    def generatedJson = JsonParser.parseString(contents.valueAsString())
    def originalElement = JsonUtils.INSTANCE.fetchPath(originalJson, path)
    def element = JsonUtils.INSTANCE.fetchPath(generatedJson, path)
    assert originalElement != element
    matchTypeOfElement(valueType, element)
  }

  @Then('the received message request metadata will contain {string} == {string}')
  void the_received_message_request_metadata_will_contain(String key, String value) {
    if (value.startsWith('JSON: ')) {
      def json = JsonParser.INSTANCE.parseString(value[5..-1])
      assert receivedMessages.first().metadata[key] == json
    } else {
      assert receivedMessages.first().metadata[key] == value
    }
  }

  @Then('the received message request metadata will contain {string} replaced with a(n) {string}')
  void the_received_message_request_metadata_will_contain_replaced_with_an(String key, String valueType) {
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    def original = message.request.metadata[key]
    def generated = receivedMessages.first().metadata[key]
    assert generated != original
    matchTypeOfElement(valueType, toJson(generated))
  }

  @Then('the received message response metadata will contain {string} == {string}')
  void the_received_message_response_metadata_will_contain(String key, String value) {
    if (value.startsWith('JSON: ')) {
      def json = JsonParser.INSTANCE.parseString(value[5..-1])
      assert responseMessages.first().metadata[key] == json
    } else {
      assert responseMessages.first().metadata[key] == value
    }
  }

  @Then('the received message response metadata will contain {string} replaced with a(n) {string}')
  void the_received_message_response_metadata_will_contain_replaced_with_an(String key, String valueType) {
    def message = v4Data.pact.interactions.find { it instanceof V4Interaction.SynchronousMessages }
    def original = message.response.first().metadata[key]
    def generated = receivedMessages.first().metadata[key]
    assert generated != original
    matchTypeOfElement(valueType, toJson(generated))
  }
}
