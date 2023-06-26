package steps.shared

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HeaderParser
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderResponse
import io.cucumber.datatable.DataTable
import io.cucumber.java.After
import io.cucumber.java.Scenario
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.apache.hc.core5.http.HttpRequest

import static au.com.dius.pact.consumer.MockHttpServerKt.mockServer
import static au.com.dius.pact.core.model.PactReaderKt.queryStringToMap
import static io.ktor.http.HttpHeaderValueParserKt.parseHeaderValue

class MockServerData {
  RequestResponsePact pact
  MockProviderConfig config
  BaseMockServer mockServer
  ProviderResponse response
}

class MockServerSharedSteps {
  CompatibilitySuiteWorld world
  MockServerData mockServerData

  MockServerSharedSteps(CompatibilitySuiteWorld world, MockServerData mockServerData) {
    this.world = world
    this.mockServerData = mockServerData
  }

  @After
  @SuppressWarnings('UnusedMethodParameter')
  void after(Scenario scenario) {
    mockServerData?.mockServer?.stop()
  }

  @When('the mock server is started with interaction {int}')
  void the_mock_server_is_started_with_interaction(Integer num) {
    mockServerData.pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [world.interactions[num - 1] ])
    mockServerData.config = new MockProviderConfig()
    mockServerData.mockServer = mockServer(mockServerData.pact, mockServerData.config)
    mockServerData.mockServer.start()
  }

  @When('request {int} is made to the mock server')
  void request_is_made_to_the_mock_server(Integer num) {
    IProviderInfo providerInfo = new ProviderInfo()
    providerInfo.port = mockServerData.mockServer.port
    def client = new ProviderClient(providerInfo, new HttpClientFactory())
    mockServerData.response = client.makeRequest(world.interactions[num - 1].request)
  }

  @When('request {int} is made to the mock server with the following changes:')
  void request_is_made_to_the_mock_server_with_the_following_changes(Integer num, DataTable dataTable) {
    def request = world.interactions[num - 1].request.copy()
    def entry = dataTable.entries().first()
    if (entry['method']) {
      request.method = entry['method']
    }

    if (entry['path']) {
      request.path = entry['path']
    }

    if (entry['query']) {
      request.query = queryStringToMap(entry['query'])
    }

    if (entry['headers']) {
      request.headers = entry['headers'].split(',').collect {
        it.trim()[1..-2].split(':')
      }.collect {
        [it[0].trim(), parseHeaderValue(it[1].trim()).collect { HeaderParser.INSTANCE.hvToString(it) }]
      }.inject([:]) { acc, e ->
        if (acc.containsKey(e[0])) {
          acc[e[0]] += e[1].flatten()
        } else {
          acc[e[0]] = e[1].flatten()
        }
        acc
      }
    }

    if (entry['body']) {
      println(entry['body'].inspect())
      if (entry['body'].startsWith('JSON:')) {
        request.headers['content-type'] = ['application/json']
        request.body = OptionalBody.body(entry['body'][5..-1].bytes, new ContentType('application/json'))
      } else if (entry['body'].startsWith('XML:')) {
        request.headers['content-type'] = ['application/xml']
        request.body = OptionalBody.body(entry['body'][4..-1].bytes, new ContentType('application/xml'))
      } else {
        String contentType = 'text/plain'
        if (entry['body'].endsWith('.json')) {
          contentType = 'application/json'
        } else if (entry['body'].endsWith('.xml')) {
          contentType = 'application/xml'
        }
        request.headers['content-type'] = [contentType]
        File contents = new File("pact-compatibility-suite/fixtures/${entry['body']}")
        contents.withInputStream {
          request.body = OptionalBody.body(it.readAllBytes(), new ContentType(contentType))
        }
      }
    }

    IProviderInfo providerInfo = new ProviderInfo()
    providerInfo.port = mockServerData.mockServer.port
    if (entry['raw headers']) {
      def headers = entry['raw headers'].split(',').collect {
        it.trim()[1..-2].split(':').collect { it.trim() }
      }
      providerInfo.requestFilter = { HttpRequest req ->
        headers.each {
          req.addHeader(it[0], it[1])
        }
      }
      def client = new ProviderClient(providerInfo, new HttpClientFactory())
      mockServerData.response = client.makeRequest(request)
    } else {
      def client = new ProviderClient(providerInfo, new HttpClientFactory())
      mockServerData.response = client.makeRequest(request)
    }
  }

  @Then('a {int} success response is returned')
  void a_success_response_is_returned(Integer status) {
    assert mockServerData.response.statusCode == status
  }

  @Then('a {int} error response is returned')
  void a_error_response_is_returned(Integer status) {
    assert mockServerData.response.statusCode == status
  }

  @Then('the payload will contain the {string} JSON document')
  void the_payload_will_contain_the_json_document(String name) {
    File contents = new File("pact-compatibility-suite/fixtures/${name}.json")
    assert mockServerData.response.body.value == contents.bytes
  }

  @Then('the content type will be set as {string}')
  void the_content_type_will_be_set_as(String string) {
    assert mockServerData.response.body.contentType.toString() == string
  }

  @Then('the mismatches will contain a {string} mismatch with error {string}')
  @SuppressWarnings('SpaceAfterOpeningBrace')
  void the_mismatches_will_contain_a_mismatch_with_error(String mismatchType, String error) {
    def mismatches = mockServerData.mockServer.mismatchedRequests
      .values()
      .flatten()
      .collectMany {
        switch (it) {
          case PactVerificationResult.Mismatches -> {
            def mismatchResult = it.mismatches.find {
              it instanceof PactVerificationResult.PartialMismatch
            } as PactVerificationResult.PartialMismatch
            mismatchResult?.mismatches?.findAll { it.type() == mismatchType }
          }
          case PactVerificationResult.PartialMismatch -> {
            it.mismatches.findAll { it.type() == mismatchType }
          }
          default -> throw new IllegalArgumentException("$it is not an expected result")
        }
      }
    assert mismatches?.find { it.description() == error } != null
  }
}
