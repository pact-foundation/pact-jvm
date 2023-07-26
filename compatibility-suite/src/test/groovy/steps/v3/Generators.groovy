package steps.v3

import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.JsonUtils
import au.com.dius.pact.core.model.Response
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
  Response response
  IResponse generatedResponse

  @Given('a request configured with the following generators:')
  void a_request_configured_with_the_following_generators(DataTable dataTable) {
    request = new Request('GET', '/path/one')
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

  @Given('a response configured with the following generators:')
  void a_response_configured_with_the_following_generators(DataTable dataTable) {
    response = new Response()
    def entry = dataTable.entries().first()
    if (entry['body']) {
      configureBody(entry['body'], response)
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
      response.generators = au.com.dius.pact.core.model.generators.Generators.fromJson(json)
    }
  }

  @Given('the generator test mode is set as {string}')
  void the_generator_test_mode_is_set_as(String mode) {
    testMode = mode == 'Consumer' ? GeneratorTestMode.Consumer : GeneratorTestMode.Provider
  }

  @When('the request is prepared for use')
  void the_request_prepared_for_use() {
    generatedRequest = request.generatedRequest(context, testMode)
    originalJson = request.body.present ? JsonParser.INSTANCE.parseString(request.body.valueAsString()) : null
    generatedJson = generatedRequest.body.present ?
      JsonParser.INSTANCE.parseString(generatedRequest.body.valueAsString()) : null
  }

  @When('the response is prepared for use')
  void the_response_is_prepared_for_use() {
    generatedResponse = response.generatedResponse(context, testMode)
    originalJson = response.body.present ? JsonParser.INSTANCE.parseString(response.body.valueAsString()) : null
    generatedJson = generatedResponse.body.present ?
      JsonParser.INSTANCE.parseString(generatedResponse.body.valueAsString()) : null
  }

  @When('the request is prepared for use with a {string} context:')
  void the_request_is_prepared_for_use_with_a_context(String type, DataTable dataTable) {
    context[type] = JsonParser.parseString(dataTable.values().first()).asObject().entries
    generatedRequest = request.generatedRequest(context, testMode)
    originalJson = request.body.present ? JsonParser.INSTANCE.parseString(request.body.valueAsString()) : null
    generatedJson = generatedRequest.body.present ?
      JsonParser.INSTANCE.parseString(generatedRequest.body.valueAsString()) : null
  }

  @Then('the body value for {string} will have been replaced with a {string}')
  void the_body_value_for_will_have_been_replaced_with_a_value(String path, String type) {
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
        assert element.type() == 'Boolean'
      }
      default -> throw new AssertionError("Invalid type: $type")
    }
  }

  @Then('the body value for {string} will have been replaced with {string}')
  void the_body_value_for_will_have_been_replaced_with_value(String path, String value) {
    def originalElement = JsonUtils.INSTANCE.fetchPath(originalJson, path)
    def element = JsonUtils.INSTANCE.fetchPath(generatedJson, path)
    assert originalElement != element
    assert element.type() == 'String'
    assert element.toString() == value
  }

  @Then('the request {string} will be set as {string}')
  void the_request_will_be_set_as(String part, String value) {
    switch (part) {
      case 'path' -> {
        assert generatedRequest.path == value
      }
      default -> throw new AssertionError("Invalid HTTP part: $part")
    }
  }

  @Then('the request {string} will match {string}')
  void the_request_will_match(String part, String regex) {
    switch (part) {
      case 'path' -> {
        assert generatedRequest.path ==~ regex
      }
      case ~/^header.*/ -> {
        def header = (part =~ /\[(.*)]/)[0][1]
        assert generatedRequest.headers[header].every { it ==~ regex  }
      }
      case ~/^queryParameter.*/ -> {
        def name = (part =~ /\[(.*)]/)[0][1]
        assert generatedRequest.query[name].every { it ==~ regex  }
      }
      default -> throw new AssertionError("Invalid HTTP part: $part")
    }
  }

  @Then('the response {string} will not be {string}')
  void the_response_will_not_be(String part, String value) {
    switch (part) {
      case 'status' -> {
        assert generatedResponse.status != value.toInteger()
      }
      default -> throw new AssertionError("Invalid HTTP part: $part")
    }
  }

  @Then('the response {string} will match {string}')
  void the_response_will_match(String part, String regex) {
    switch (part) {
      case 'status' -> {
        assert generatedResponse.status ==~ regex
      }
      case ~/^header.*/ -> {
        def header = (part =~ /\[(.*)]/)[0][1]
        assert generatedResponse.headers[header].every { it ==~ regex  }
      }
      default -> throw new AssertionError("Invalid HTTP part: $part")
    }
  }
}
