package steps.shared

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.KTorMockServer
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.HeaderParser
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.StringSource
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.MockServerURLGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import groovy.json.JsonSlurper
import io.cucumber.datatable.DataTable
import io.cucumber.java.After
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.io.entity.StringEntity

import static io.ktor.http.HttpHeaderValueParserKt.parseHeaderValue
import static steps.shared.SharedSteps.configureBody
import static steps.shared.SharedSteps.determineContentType

@SuppressWarnings(['ThrowRuntimeException', 'AbcMetric'])
class SharedHttpProvider {
  CompatibilitySuiteWorld world
  VerificationData verificationData
  BaseMockServer mockProvider
  List<BaseMockServer> mockBrokers = []

  SharedHttpProvider(CompatibilitySuiteWorld world, VerificationData verificationData) {
    this.verificationData = verificationData
    this.world = world
  }

  @After
  @SuppressWarnings('UnusedMethodParameter')
  void after(Scenario scenario) {
    mockProvider?.stop()
    mockBrokers.each { it.stop() }
  }

  @Given('a provider is started that returns the response from interaction {int}')
  void a_provider_is_started_that_returns_the_response_from_interaction(Integer num) {
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [ world.interactions[num - 1].copy() ])
    mockProvider = new KTorMockServer(pact, new MockProviderConfig())
    mockProvider.start()
    verificationData.providerInfo = new ProviderInfo('p')
    verificationData.providerInfo.port = mockProvider.port
    verificationData.providerInfo.stateChangeTeardown = true
  }

  @Given('a provider is started that returns the response from interaction {int}, with the following changes:')
  void a_provider_is_started_that_returns_the_response_from_interaction_with_the_following_changes(
    Integer num,
    DataTable dataTable
  ) {
    def interaction = world.interactions[num - 1].copy()
    def entry = dataTable.entries().first()
    if (entry['response']) {
      interaction.response.status = entry['response'].toInteger()
    }

    if (entry['response headers']) {
      entry['response headers'].split(',').collect {
        it.trim()[1..-2].split(':')
      }.collect {
        [it[0].trim(), parseHeaderValue(it[1].trim()).collect { HeaderParser.INSTANCE.hvToString(it) }]
      }.inject(interaction.response.headers) { headers, e ->
        if (headers.containsKey(e[0])) {
          headers[e[0]] += e[1].flatten()
        } else {
          headers[e[0]] = e[1].flatten()
        }
        headers
      }
    }

    if (entry['response body']) {
      def part = configureBody(entry['response body'], determineContentType(entry['response body'],
        interaction.response.contentTypeHeader()))
      interaction.response.body = part.body
      interaction.response.headers.putAll(part.headers)
    }

    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [ interaction ])
    mockProvider = new KTorMockServer(pact, new MockProviderConfig())
    mockProvider.start()
    verificationData.providerInfo = new ProviderInfo('p')
    verificationData.providerInfo.port = mockProvider.port
    verificationData.providerInfo.stateChangeTeardown = true
  }

  @Given('a Pact file for interaction {int} is to be verified')
  void a_pact_file_for_interaction_is_to_be_verified(Integer num) {
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [ world.interactions[num - 1].copy() ])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V1)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  @Given('a Pact file for interaction {int} is to be verified with a provider state {string} defined')
  void a_pact_file_for_interaction_is_to_be_verified_with_a_provider_state_defined(Integer num, String providerState) {
    def interaction = world.interactions[num - 1].copy()
    interaction.providerStates << new ProviderState(providerState)
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [interaction])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V1)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    if (verificationData.providerInfo.stateChangeRequestFilter) {
      consumerInfo.stateChange = verificationData.providerInfo.stateChangeRequestFilter
    }
    verificationData.providerInfo.consumers << consumerInfo
  }

  @Given('a provider is started that returns the responses from interactions {string}')
  void a_provider_is_started_that_returns_the_responses_from_interactions(String ids) {
    def interactions = ids.split(',\\s*').collect {
      def index = it.toInteger()
      world.interactions[index - 1]
    }
    Pact pact = new RequestResponsePact(new Provider('p'), new Consumer('v1-compatibility-suite-c'),
      interactions)
    mockProvider = new KTorMockServer(pact, new MockProviderConfig())
    mockProvider.start()
    verificationData.providerInfo = new ProviderInfo('p')
    verificationData.providerInfo.port = mockProvider.port
  }

  @Given('a Pact file for interaction {int} is to be verified from a Pact broker')
  void a_pact_file_for_interaction_is_to_be_verified_from_a_pact_broker(Integer num) {
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer("c_$num"), [ world.interactions[num - 1] ])
    def pactJson = pact.toMap(PactSpecVersion.V1)
    pactJson['_links'] = [
      'pb:publish-verification-results': [
        'title': 'Publish verification results',
        'href': "http://localhost:1234/pacts/provider/p/consumer/c_$num/verification-results"
      ]
    ]
    pactJson['interactions'][0]['_id'] = world.interactions[num - 1].interactionId

    File contents = new File("pact-compatibility-suite/fixtures/pact-broker_c${num}.json")
    Pact brokerPact = DefaultPactReader.INSTANCE.loadPact(contents) as BasePact
    /// AAARGH! My head. Adding a Pact Interaction to a Pact file for fetching a Pact file for verification
    def matchingRules = new MatchingRulesImpl()
    matchingRules
      .addCategory('body')
      .addRule('$._links.pb:publish-verification-results.href',
        new RegexMatcher(".*\\/(pacts\\/provider\\/p\\/consumer\\/c_$num\\/verification-results)"))
    Generators generators = new Generators([
      (Category.BODY): [
        '$._links.pb:publish-verification-results.href': new MockServerURLGenerator(
          "http://localhost:1234/pacts/provider/p/consumer/c_$num/verification-results",
          ".*\\/(pacts\\/provider\\/p\\/consumer\\/c_$num\\/verification-results)"
        )
      ]
    ])
    Interaction interaction = new RequestResponseInteraction("Interaction $num", [],
      new Request('GET', "/pacts/provider/p/consumer/c_$num"),
      new Response(200,
        ['content-type': ['application/json']],
        OptionalBody.body(Json.INSTANCE.prettyPrint(pactJson).bytes, ContentType.JSON),
        matchingRules, generators
      )
    )
    brokerPact.interactions << interaction

    def mockBroker = new KTorMockServer(brokerPact, new MockProviderConfig())
    mockBroker.start()
    mockBrokers << mockBroker

    verificationData.providerInfo.hasPactsFromPactBrokerWithSelectorsV2("http://127.0.0.1:${mockBroker.port}", [])
  }

  @Then('a verification result will NOT be published back')
  void a_verification_result_will_not_be_published_back() {
    assert mockBrokers.every { mock ->
      mock.matchedRequests.find { it.first.path.endsWith('/verification-results') } == null
    }
  }

  @Given('publishing of verification results is enabled')
  void publishing_of_verification_results_is_enabled() {
    verificationData.verificationProperties['pact.verifier.publishResults'] = 'true'
  }

  @Then('a successful verification result will be published back for interaction \\{{int}}')
  void a_successful_verification_result_will_be_published_back_for_interaction(Integer num) {
    def request = mockBrokers.collect {
      it.matchedRequests.find { it.first.path == "/pacts/provider/p/consumer/c_$num/verification-results".toString() }
    }.find()
    assert request != null
    def json = new JsonSlurper().parseText( request.first.body.valueAsString())
    assert json.success == true
  }

  @Then('a failed verification result will be published back for the interaction \\{{int}}')
  void a_failed_verification_result_will_be_published_back_for_the_interaction(Integer num) {
    def request = mockBrokers.collect {
      it.matchedRequests.find { it.first.path == "/pacts/provider/p/consumer/c_$num/verification-results".toString() }
    }.find()
    assert request != null
    def json = new JsonSlurper().parseText( request.first.body.valueAsString())
    assert json.success == false
  }

  @Given('a request filter is configured to make the following changes:')
  void a_request_filter_is_configured_to_make_the_following_changes(DataTable dataTable) {
    verificationData.providerInfo.requestFilter = { ClassicHttpRequest request ->
      def entry = dataTable.entries().first()
      if (entry['path']) {
        request.path = entry['path']
      }

      if (entry['headers']) {
        entry['headers'].split(',').collect {
          it.trim()[1..-2].split(':')
        }.collect {
          [it[0].trim(), it[1].trim()]
        }.each {
          request.addHeader(it[0].toString(), it[1])
        }
      }

      if (entry['body']) {
        if (entry['body'].startsWith('JSON:')) {
          request.addHeader('content-type', 'application/json')
          def ct = new org.apache.hc.core5.http.ContentType('application/json', null)
          request.entity = new StringEntity(entry['body'][5..-1], ct)
        } else if (entry['body'].startsWith('XML:')) {
          request.addHeader('content-type', 'application/xml')
          def ct = new org.apache.hc.core5.http.ContentType('application/xml', null)
          request.entity = new StringEntity(entry['body'][4..-1], ct)
        } else {
          String contentType = 'text/plain'
          if (entry['body'].endsWith('.json')) {
            contentType = 'application/json'
          } else if (entry['body'].endsWith('.xml')) {
            contentType = 'application/xml'
          }
          request.addHeader('content-type', contentType)
          File contents = new File("pact-compatibility-suite/fixtures/${entry['body']}")
          contents.withInputStream {
            def ct = new org.apache.hc.core5.http.ContentType(contentType, null)
            request.entity = new StringEntity(it.text, ct)
          }
        }
      }
    }
  }

  @Then('the request to the provider will contain the header {string}')
  void the_request_to_the_provider_will_contain_the_header(String header) {
    def h = header.split(':\\s+', 2)
    assert mockProvider.matchedRequests.every {
      it.second.headers.containsKey(h[0]) && it.second.headers[h[0]][0] == h[1]
    }
  }
}
