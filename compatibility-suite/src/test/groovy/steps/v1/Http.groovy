package steps.v1

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.PactTestExecutionContext
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderResponse
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

import static au.com.dius.pact.consumer.MockHttpServerKt.mockServer
import static au.com.dius.pact.core.model.PactReaderKt.queryStringToMap

@SuppressWarnings('SpaceAfterOpeningBrace')
class Http {
  List<RequestResponseInteraction> interactions = []
  RequestResponsePact pact
  MockProviderConfig config
  BaseMockServer mockServer
  ProviderResponse response
  PactVerificationResult mockServerResult
  String scenarioId
  File pactFile
  Pact loadedPact
  Object loadPactJson

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

  @Given('the following HTTP interactions have been defined:')
  void the_following_http_interactions_have_been_setup(DataTable dataTable) {
    dataTable.entries().eachWithIndex { Map<String, String> entry, int i ->
      Interaction interaction = new RequestResponseInteraction("Interaction $i")

      if (entry['method']) {
        interaction.request.method = entry['method']
      }

      if (entry['path']) {
        interaction.request.path = entry['path']
      }

      if (entry['query']) {
        interaction.request.query = queryStringToMap(entry['query'])
      }

      if (entry['response']) {
        interaction.response.status = entry['response'].toInteger()
      }

      if (entry['response body']) {
        String contentType = 'text/plain'
        if (entry['response content']) {
          contentType = entry['response content']
        }
        interaction.response.headers['content-type'] = [ contentType ]
        File contents = new File("pact-compatibility-suite/fixtures/${entry['response body']}")
        contents.withInputStream {
          interaction.response.body = OptionalBody.body(it.readAllBytes(), new ContentType(contentType))
        }
      }

      interactions << interaction
    }
  }

  @When('the mock server is started with interaction \\{{int}}')
  void the_mock_server_is_started_with_interaction(Integer num) {
    pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [ interactions[num - 1] ])
    config = new MockProviderConfig()
    mockServer = mockServer(pact, config)
    mockServer.start()
  }

  @When('the mock server is started with interactions {string}')
  void the_mock_server_is_started_with_interactions(String ids) {
    def interactions = ids.split(',\\s*').collect {
      def index = it.toInteger()
      interactions[index - 1]
    }
    pact = new RequestResponsePact(new Provider('p'), new Consumer('v1-compatibility-suite-c'),
      interactions)
    config = new MockProviderConfig()
    mockServer = mockServer(pact, config)
    mockServer.start()
  }

  @When('request \\{{int}} is made to the mock server')
  void request_is_made_to_the_mock_server(Integer num) {
    IProviderInfo providerInfo = new ProviderInfo()
    providerInfo.port = mockServer.port
    def client = new ProviderClient(providerInfo, new HttpClientFactory())
    response = client.makeRequest(interactions[num - 1].request)
  }

  @When('request \\{{int}} is made to the mock server with the following changes:')
  void request_is_made_to_the_mock_server_with_the_following_changes(Integer num, DataTable dataTable) {
    IProviderInfo providerInfo = new ProviderInfo()
    providerInfo.port = mockServer.port
    def client = new ProviderClient(providerInfo, new HttpClientFactory())

    def request = interactions[num - 1].request.copy()
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

    response = client.makeRequest(request)
  }

  @Then('a {int} success response is returned')
  void a_success_response_is_returned(Integer status) {
    assert response.statusCode == status
  }

  @Then('a {int} error response is returned')
  void a_error_response_is_returned(Integer status) {
    assert response.statusCode == status
  }

  @Then('the payload will contain the {string} JSON document')
  void the_payload_will_contain_the_json_document(String name) {
    File contents = new File("pact-compatibility-suite/fixtures/${name}.json")
    assert response.body.value == contents.bytes
  }

  @Then('the content type will be set as {string}')
  void the_content_type_will_be_set_as(String string) {
    assert response.body.contentType.toString() == string
  }

  @When('the pact test is done')
  void the_pact_test_is_done() {
    mockServer.stop()
    PactTestExecutionContext testContext = new PactTestExecutionContext("build/compatibility-suite/v1/$scenarioId")
    mockServerResult = mockServer.verifyResultAndWritePact(true, testContext, pact, PactSpecVersion.V1)
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
    def json = new JsonSlurper().parse(contents)
    assert loadedPact.interactions[num].asSynchronousRequestResponse().response.body.value ==
      JsonOutput.toJson(json).bytes
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
        assert mockServerResult.expectedRequests.first() == interactions[num - 1].request
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

  @Then('the mismatches will contain a {string} mismatch with error {string}')
  void the_mismatches_will_contain_a_mismatch_with_error(String mismatchType, String error) {
    switch (mockServerResult) {
      case PactVerificationResult.Mismatches -> {
        def mismatchResult = mockServerResult.mismatches.find {
          it instanceof PactVerificationResult.PartialMismatch
        } as PactVerificationResult.PartialMismatch
        def mismatches = mismatchResult.mismatches.findAll {it.type() == mismatchType }
        assert mismatches.find { it.description() == error } != null
      }
      case PactVerificationResult.PartialMismatch -> {
        def mismatches = mockServerResult.mismatches.findAll {it.type() == mismatchType }
        assert mismatches.find { it.description() == error } != null
      }
      default -> throw new IllegalArgumentException("$mockServerResult is not an expected result")
    }
  }

  @Then("the mock server status will be an unexpected request received error for interaction \\{{int}}")
  void the_mock_server_status_will_be_an_unexpected_request_received_error_for_interaction(Integer num) {
    switch (mockServerResult) {
      case PactVerificationResult.Mismatches -> {
        def mismatch = mockServerResult.mismatches.find {
          it instanceof PactVerificationResult.UnexpectedRequest
        } as PactVerificationResult.UnexpectedRequest

        def expectedRequest = interactions[num - 1].request
        assert mismatch.request.method == expectedRequest.method
        assert mismatch.request.path == expectedRequest.path
        assert mismatch.request.query == expectedRequest.query
      }
      default -> throw new IllegalArgumentException("$mockServerResult is not an expected result")
    }
  }
}
