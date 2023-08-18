package steps.v1

import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.QueryMismatch
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import io.cucumber.java.Scenario
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import steps.shared.CompatibilitySuiteWorld
import steps.shared.MockServerData

import static au.com.dius.pact.consumer.MockHttpServerKt.mockServer
import static au.com.dius.pact.core.model.PactReaderKt.queryStringToMap

@SuppressWarnings(['SpaceAfterOpeningBrace', 'AbcMetric', 'NestedBlockDepth'])
class HttpConsumer {
  CompatibilitySuiteWorld world
  MockServerData mockServerData

  PactVerificationResult mockServerResult
  String scenarioId
  File pactFile
  Pact loadedPact
  Object loadPactJson

  HttpConsumer(CompatibilitySuiteWorld world, MockServerData mockServerData) {
    this.mockServerData = mockServerData
    this.world = world
  }

  @Before
  void before(Scenario scenario) {
    scenarioId = scenario.id
  }

  @After
  void after(Scenario scenario) {
    if (!scenario.failed) {
      def dir = "build/compatibility-suite/v1/$scenarioId" as File
      dir.deleteDir()
    }
  }

  @ParameterType('first|second|third')
  static Integer numType(String numType) {
    switch (numType) {
      case 'first' -> yield 0
      case 'second'-> yield 1
      case 'third' -> yield 2
      default -> throw new IllegalArgumentException("$numType is not a valid number type")
    }
  }

  @When('the mock server is started with interactions {string}')
  void the_mock_server_is_started_with_interactions(String ids) {
    def interactions = ids.split(',\\s*').collect {
      def index = it.toInteger()
      world.interactions[index - 1]
    }
    mockServerData.pact = new RequestResponsePact(new Provider('p'), new Consumer('v1-compatibility-suite-c'),
      interactions)
    mockServerData.config = new MockProviderConfig()
    mockServerData.mockServer = mockServer(mockServerData.pact, mockServerData.config)
    mockServerData.mockServer.start()
  }

  @When('the pact test is done')
  void the_pact_test_is_done() {
    mockServerData.mockServer.stop()
    PactTestExecutionContext testContext = new PactTestExecutionContext("build/compatibility-suite/v1/$scenarioId")
    mockServerResult = mockServerData.mockServer.verifyResultAndWritePact(true, testContext,
      mockServerData.pact, PactSpecVersion.V1)
    def dir = "build/compatibility-suite/v1/$scenarioId" as File
    pactFile = new File(dir, 'v1-compatibility-suite-c-p.json')
  }

  @Then('the mock server will write out a Pact file for the interaction(s) when done')
  void the_mock_server_will_write_out_a_pact_file_for_the_interaction_when_done() {
    assert pactFile.exists()
    loadPactJson = new JsonSlurper().parse(pactFile)
    loadedPact = DefaultPactReader.INSTANCE.loadPact(pactFile)
    assert loadedPact != null
    assert loadPactJson['metadata']['pactSpecification']['version'] == '1.0.0'
  }

  @Then('the mock server will NOT write out a Pact file for the interaction(s) when done')
  void the_mock_server_will_not_write_out_a_pact_file_for_the_interaction_when_done() {
    assert !pactFile.exists()
  }

  @Then('the pact file will contain \\{{int}} interaction(s)')
  void the_pact_file_will_contain_interaction(Integer num) {
    assert loadedPact.interactions.size() == num
  }

  @Then('the \\{{numType}} interaction request will be for a {string}')
  void the_interaction_request_will_be_for_a(Integer num, String method) {
    assert loadedPact.interactions[num].asSynchronousRequestResponse().request.method == method
  }

  @Then('the \\{{numType}} interaction response will contain the {string} document')
  void the_interaction_response_will_contain_the_document(Integer num, String fixture) {
    File contents = new File("pact-compatibility-suite/fixtures/${fixture}")
    if (fixture.endsWith('.json')) {
      def json = new JsonSlurper().parse(contents)
      assert loadedPact.interactions[num].asSynchronousRequestResponse().response.body.value ==
        JsonOutput.toJson(json).bytes
    } else {
      assert loadedPact.interactions[num].asSynchronousRequestResponse().response.body.value ==
        contents.bytes
    }
  }

  @Then('the mock server status will be OK')
  void the_mock_server_status_will_be_ok() {
    assert mockServerResult instanceof PactVerificationResult.Ok
  }

  @Then('the mock server status will NOT be OK')
  void the_mock_server_status_will_be_error() {
    assert !(mockServerResult instanceof PactVerificationResult.Ok)
  }

  @Then('the mock server error will contain {string}')
  void the_mock_server_error_will_contain(String error) {
    switch (mockServerResult) {
      case PactVerificationResult.Error -> assert mockServerResult.error.message ==~ error
      default -> throw new IllegalArgumentException("$mockServerResult is not an expected result")
    }
  }

  @Then('the mock server status will be an expected but not received error for interaction \\{{int}}')
  void the_mock_server_status_will_be_an_expected_but_not_received_error_for_interaction(Integer num) {
    switch (mockServerResult) {
      case PactVerificationResult.ExpectedButNotReceived ->
        assert mockServerResult.expectedRequests.first() == world.interactions[num - 1].request
      default -> throw new IllegalArgumentException("$mockServerResult is not an expected result")
    }
  }

  @Then('the \\{{numType}} interaction request query parameters will be {string}')
  void the_interaction_request_query_parameters_will_be(Integer num, String queryStr) {
    def query = queryStringToMap(queryStr)
    assert loadedPact.interactions[num].asSynchronousRequestResponse().request.query == query
  }

  @Then('the mock server status will be a partial mismatch')
  void the_mock_server_status_will_be_a_partial_mismatch() {
    assert mockServerResult instanceof  PactVerificationResult.PartialMismatch
  }

  @Then('the mock server status will be mismatches')
  void the_mock_server_status_will_be_mismatches() {
    assert mockServerResult instanceof  PactVerificationResult.Mismatches
  }

  @Then('the mock server status will be an unexpected {string} request received error for interaction \\{{int}}')
  void the_mock_server_status_will_be_an_unexpected_request_received_error_for_interaction(String method, Integer num) {
    switch (mockServerResult) {
      case PactVerificationResult.Mismatches -> {
        def mismatch = mockServerResult.mismatches.find {
          it instanceof PactVerificationResult.UnexpectedRequest
        } as PactVerificationResult.UnexpectedRequest

        def expectedRequest = world.interactions[num - 1].request
        assert mismatch.request.method == method
        assert mismatch.request.path == expectedRequest.path
        assert mismatch.request.query == expectedRequest.query
      }
      default -> throw new IllegalArgumentException("$mockServerResult is not an expected result")
    }
  }

  @Then('the mock server status will be an unexpected {string} request received error for path {string}')
  void the_mock_server_status_will_be_an_unexpected_request_received_error(String method, String path) {
    switch (mockServerResult) {
      case PactVerificationResult.Mismatches -> {
        def mismatch = mockServerResult.mismatches.find {
          it instanceof PactVerificationResult.UnexpectedRequest
        } as PactVerificationResult.UnexpectedRequest
        assert mismatch.request.method == method
        assert mismatch.request.path == path
      }
      default -> throw new IllegalArgumentException("$mockServerResult is not an expected result")
    }
  }

  @Then('the \\{{numType}} interaction request will contain the header {string} with value {string}')
  void the_interaction_request_will_contain_the_header_with_value(Integer num, String key, String value) {
    def headers = loadedPact.interactions[num].asSynchronousRequestResponse().request.headers
    assert headers[key] == [ value ]
  }

  @Then('the \\{{numType}} interaction request content type will be {string}')
  void the_interaction_request_content_type_will_be(Integer num, String contentType) {
    assert loadedPact.interactions[num].asSynchronousRequestResponse().request.contentTypeHeader() == contentType
  }

  @Then('the \\{{numType}} interaction request will contain the {string} document')
  void the_interaction_request_will_contain_the_document(Integer num, String fixture) {
    File contents = new File("pact-compatibility-suite/fixtures/${fixture}")
    if (fixture.endsWith('.json')) {
      def json = new JsonSlurper().parse(contents)
      assert loadedPact.interactions[num].asSynchronousRequestResponse().request.body.value ==
        JsonOutput.toJson(json).bytes
    } else {
      assert loadedPact.interactions[num].asSynchronousRequestResponse().request.body.value ==
        contents.bytes
    }
  }

  @Then('the mismatches will contain a {string} mismatch with path {string} with error {string}')
  void the_mismatches_will_contain_a_mismatch_with_path_with_error(String mismatchType, String path, String error) {
    switch (mockServerResult) {
      case PactVerificationResult.Mismatches -> {
        def mismatchResult = mockServerResult.mismatches.find {
          it instanceof PactVerificationResult.PartialMismatch
        } as PactVerificationResult.PartialMismatch
        def mismatches = mismatchResult?.mismatches?.findAll {it.type() == mismatchType }
        assert mismatches?.find {
          switch (it) {
            case QueryMismatch -> it.path == path && it.description() == error
            case HeaderMismatch -> it.headerKey == path && it.description() == error
            case BodyMismatch -> it.path == path && it.description() == error
            default -> false
          }
        } != null
      }
      case PactVerificationResult.PartialMismatch -> {
        def mismatches = mockServerResult.mismatches.findAll {it.type() == mismatchType }
        assert mismatches?.find {
          switch (it) {
            case QueryMismatch -> it.path == path && it.description() == error
            case HeaderMismatch -> it.headerKey == path && it.description() == error
            case BodyMismatch -> it.path == path && it.description() == error
            default -> false
          }
        } != null
      }
      default -> throw new IllegalArgumentException("$mockServerResult is not an expected result")
    }
  }
}
