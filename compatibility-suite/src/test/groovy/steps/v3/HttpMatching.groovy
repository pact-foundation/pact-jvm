package steps.v3

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.RequestMatchResult
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.HeaderParser
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

import static au.com.dius.pact.core.matchers.RequestMatching.requestMismatches
import static io.ktor.http.HttpHeaderValueParserKt.parseHeaderValue
import static steps.shared.SharedSteps.configureBody
import static steps.shared.SharedSteps.determineContentType

@SuppressWarnings('SpaceAfterOpeningBrace')
class HttpMatching {
  Request expectedRequest
  List<Request> receivedRequests = []
  List<RequestMatchResult> results = []

  @Given('an expected request with a(n) {string} header of {string}')
  void an_expected_request_with_a_header_of(String header, String value) {
    expectedRequest = new Request()
    expectedRequest.headers[header] = parseHeaderValue(value).collect { HeaderParser.INSTANCE.hvToString(it) }
  }

  @Given('a request is received with a(n) {string} header of {string}')
  void a_request_is_received_with_a_header_of(String header, String value) {
    receivedRequests << new Request()
    receivedRequests[0].headers[header] = parseHeaderValue(value).collect { HeaderParser.INSTANCE.hvToString(it) }
  }

  @Given('an expected request configured with the following:')
  void an_expected_request_configured_with_the_following(DataTable dataTable) {
    expectedRequest = new Request()
    def entry = dataTable.entries().first()
    if (entry['body']) {
      def part = configureBody(entry['body'], determineContentType(entry['body'], expectedRequest.contentTypeHeader()))
      expectedRequest.body = part.body
      expectedRequest.headers.putAll(part.headers)
    }

    if (entry['matching rules']) {
      JsonValue json
      if (entry['matching rules'].startsWith('JSON:')) {
        json = JsonParser.INSTANCE.parseString(entry['matching rules'][5..-1])
      } else {
        File contents = new File("pact-compatibility-suite/fixtures/${entry['matching rules']}")
        contents.withInputStream {
          json = JsonParser.INSTANCE.parseStream(it)
        }
      }
      expectedRequest.matchingRules = MatchingRulesImpl.fromJson(json)
    }
  }

  @Given('a request is received with the following:')
  void a_request_is_received_with_the_following(DataTable dataTable) {
    receivedRequests << new Request()
    def entry = dataTable.entries().first()
    if (entry['body']) {
      def part = configureBody(entry['body'], determineContentType(entry['body'],
        receivedRequests[0].contentTypeHeader()))
      receivedRequests[0].body = part.body
      receivedRequests[0].headers.putAll(part.headers)
    }
  }

  @Given('the following requests are received:')
  void the_following_requests_are_received(DataTable dataTable) {
    for (entry in dataTable.entries()) {
      def request = new Request()

      if (entry['body']) {
        def body = entry['body']
        if (entry['body'] == 'EMPTY') {
          body = ''
        }
        def part = configureBody(body, determineContentType(body, request.contentTypeHeader()))
        request.body = part.body
        request.headers.putAll(part.headers)
      }
      receivedRequests << request
    }
  }

  @When('the request is compared to the expected one')
  void the_request_is_compared_to_the_expected_one() {
    def pact = new RequestResponsePact(new Provider(this.class.name), new Consumer(this.class.name))
    def interaction = new RequestResponseInteraction('compatibility-suite', [], expectedRequest)
    results << requestMismatches(pact, interaction, receivedRequests[0])
  }

  @When('the requests are compared to the expected one')
  void the_requests_are_compared_to_the_expected_one() {
    def pact = new RequestResponsePact(new Provider(this.class.name), new Consumer(this.class.name))
    def interaction = new RequestResponseInteraction('compatibility-suite', [], expectedRequest)
    results.addAll(receivedRequests.collect { requestMismatches(pact, interaction, it) })
  }

  @Then('the comparison should be OK')
  void the_comparison_should_be_ok() {
    assert results.every { it.mismatches.empty }
  }

  @Then('the comparison should NOT be OK')
  void the_comparison_should_not_be_ok() {
    assert results.any { !it.mismatches.empty }
  }

  @Then('the mismatches will contain a mismatch with error {string} -> {string}')
  void the_mismatches_will_contain_a_mismatch_with_error(String path, String error) {
    assert results.any {
      it.mismatches.find {
        def pathMatches = switch (it) {
          case HeaderMismatch -> it.headerKey == path
          case BodyMismatch -> it.path == path
          default -> false
        }
        def desc = it.description()
        (desc.contains(error) || desc.matches(error)) && pathMatches
      } != null
    }
  }
}
