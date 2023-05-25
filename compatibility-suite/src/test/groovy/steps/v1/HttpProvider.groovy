package steps.v1

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.KTorMockServer
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
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
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import groovy.json.JsonSlurper
import io.cucumber.java.After
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class HttpProvider {
  CompatibilitySuiteWorld world
  BaseMockServer mockProvider
  ProviderInfo providerInfo
  ProviderVerifier verifier
  List<VerificationResult> verificationResults
  List<BaseMockServer> mockBrokers = []
  Map<String, String> verificationProperties = [:]

  HttpProvider(CompatibilitySuiteWorld world) {
    this.world = world
  }

  @After
  void after(Scenario scenario) {
    mockProvider?.stop()
    mockBrokers.each { it.stop() }
  }

  @Given('a provider is started that returns the response from interaction \\{{int}}')
  void a_provider_is_started_that_returns_the_response_from_interaction(Integer num) {
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [world.interactions[num - 1] ])
    mockProvider = new KTorMockServer(pact, new MockProviderConfig())
    mockProvider.start()
    providerInfo = new ProviderInfo('p')
    providerInfo.port = mockProvider.port
  }

  @Given('a Pact file for interaction \\{{int}} is to be verified')
  void a_pact_file_for_interaction_is_to_be_verified(Integer num) {
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer('v1-compatibility-suite-c'), [world.interactions[num - 1] ])
    StringWriter writer = new StringWriter()
    writer.withPrintWriter {
      DefaultPactWriter.INSTANCE.writePact(pact, it, PactSpecVersion.V1)
    }
    ConsumerInfo consumerInfo = new ConsumerInfo('c')
    consumerInfo.pactSource = new StringSource(writer.toString())
    providerInfo.consumers << consumerInfo
  }

  @When('the verification is run')
  void the_verification_is_run() {
    verifier = new ProviderVerifier()
    verifier.projectHasProperty = { name -> verificationProperties.containsKey(name) }
    verifier.projectGetProperty = { name -> verificationProperties[name] }
    verificationResults = verifier.verifyProvider(providerInfo)
  }

  @Then('the verification will be successful')
  void the_verification_will_be_successful() {
    assert verificationResults.inject(true) { acc, result -> acc && result instanceof VerificationResult.Ok }
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
    providerInfo = new ProviderInfo('p')
    providerInfo.port = mockProvider.port
  }

  @Then('the verification will NOT be successful')
  void the_verification_will_not_be_successful() {
    assert verificationResults.any { it instanceof VerificationResult.Failed }
  }

  @Then('the verification results will contain a {string} error')
  void the_verification_results_will_contain_a_error(String error) {
    assert verificationResults.any {
      it instanceof VerificationResult.Failed && it.description == error
    }
  }

  @Given('a Pact file for interaction \\{{int}} is to be verified from a Pact broker')
  void a_pact_file_for_interaction_is_to_be_verified_from_a_pact_broker(Integer num) {
    Pact pact = new RequestResponsePact(new Provider('p'),
      new Consumer("c_$num"), [ world.interactions[num - 1] ])
    def pactJson = pact.toMap(PactSpecVersion.V1)
    pactJson['_links'] = [
      "pb:publish-verification-results": [
        "title": "Publish verification results",
        "href": "http://localhost:1234/pacts/provider/p/consumer/c_$num/verification-results"
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

    providerInfo.hasPactsFromPactBrokerWithSelectorsV2("http://127.0.0.1:${mockBroker.port}", [])
  }

  @Then('a verification result will NOT be published back')
  void a_verification_result_will_not_be_published_back() {
    assert mockBrokers.every { mock ->
      mock.matchedRequests.find { it.path.endsWith('/verification-results') } == null
    }
  }

  @Given('publishing of verification results is enabled')
  void publishing_of_verification_results_is_enabled() {
    verificationProperties['pact.verifier.publishResults'] = 'true'
  }

  @Then('a successful verification result will be published back for interaction \\{{int}}')
  void a_successful_verification_result_will_be_published_back_for_interaction(Integer num) {
    def request = mockBrokers.collect {
      it.matchedRequests.find { it.path == "/pacts/provider/p/consumer/c_$num/verification-results".toString() }
    }.find()
    assert request != null
    def json = new JsonSlurper().parseText( request.body.valueAsString())
    assert json.success == true
  }

  @Then("a failed verification result will be published back for the interaction \\{{int}}")
  void a_failed_verification_result_will_be_published_back_for_the_interaction(Integer num) {
    def request = mockBrokers.collect {
      it.matchedRequests.find { it.path == "/pacts/provider/p/consumer/c_$num/verification-results".toString() }
    }.find()
    assert request != null
    def json = new JsonSlurper().parseText( request.body.valueAsString())
    assert json.success == false
  }
}
