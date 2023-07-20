package steps.v3

import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.JsonUtils
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

import static steps.shared.SharedSteps.configureBody

@SuppressWarnings('SpaceAfterOpeningBrace')
class Generators {
  Request request
  IRequest generatedRequest
  Map<String, Object> context = [:]
  GeneratorTestMode testMode = GeneratorTestMode.Provider
  JsonValue originalJson
  JsonValue generatedJson

  @Given('a request configured with the following generators:')
  void a_request_configured_with_the_following_generators(DataTable dataTable) {
    request = new Request()
    def entry = dataTable.entries().first()
    if (entry['body']) {
      configureBody(entry['body'], request)
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
      request.generators = au.com.dius.pact.core.model.generators.Generators.fromJson(json)
    }
  }

  @Given('the generator test mode is set as {string}')
  void the_generator_test_mode_is_set_as(String mode) {
    testMode = mode == 'Consumer' ? GeneratorTestMode.Consumer : GeneratorTestMode.Provider
  }

  @When('the request is prepared for use')
  void the_request_prepared_for_use() {
    generatedRequest = request.generatedRequest(context, testMode)
    originalJson = JsonParser.INSTANCE.parseString(request.body.valueAsString())
    generatedJson = JsonParser.INSTANCE.parseString(generatedRequest.body.valueAsString())
  }

  @When('the request is prepared for use with a {string} context:')
  void the_request_is_prepared_for_use_with_a_context(String type, DataTable dataTable) {
    context[type] = JsonParser.parseString(dataTable.values().first()).asObject().entries
    generatedRequest = request.generatedRequest(context, testMode)
    originalJson = JsonParser.INSTANCE.parseString(request.body.valueAsString())
    generatedJson = JsonParser.INSTANCE.parseString(generatedRequest.body.valueAsString())
  }

  @Then('the value for {string} will have been replaced with a {string}')
  void the_value_for_will_have_been_replaced_with_a_value(String path, String type) {
    def originalElement = JsonUtils.INSTANCE.fetchPath(originalJson, path)
    def element = JsonUtils.INSTANCE.fetchPath(generatedJson, path)
    assert originalElement != element
    switch (type) {
      case 'integer' -> {
        assert element.type() == 'Integer'
        assert element.toString() ==~ /\d+/
      }
      case 'decimal number' -> {
        assert element.type() == 'Decimal'
        assert element.toString() ==~ /\d+\.\d+/
      }
      case 'hexadecimal number' -> {
        assert element.type() == 'String'
        assert element.toString() ==~ /[a-fA-F0-9]+/
      }
      case 'random string' -> {
        assert element.type() == 'String'
      }
      case 'string from the regex' -> {
        assert element.type() == 'String'
        assert element.toString() ==~ /\d{1,8}/
      }
      case 'date' -> {
        assert element.type() == 'String'
        assert element.toString() ==~ /\d{4}-\d{2}-\d{2}/
      }
      case 'time' -> {
        assert element.type() == 'String'
        assert element.toString() ==~ /\d{2}:\d{2}:\d{2}/
      }
      case 'date-time' -> {
        assert element.type() == 'String'
        assert element.toString() ==~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{1,9}/
      }
      case 'UUID' -> {
        assert element.type() == 'String'
        UUID.fromString(element.toString())
      }
      case 'boolean' -> {
        assert element.type() ==~ /True|False/
      }
      default -> throw new AssertionError("Invalid type: $type")
    }
  }

  @Then('the value for {string} will have been replaced with {string}')
  void the_value_for_will_have_been_replaced_with_value(String path, String value) {
    def originalElement = JsonUtils.INSTANCE.fetchPath(originalJson, path)
    def element = JsonUtils.INSTANCE.fetchPath(generatedJson, path)
    assert originalElement != element
    assert element.type() == 'String'
    assert element.toString() == value
  }
}
