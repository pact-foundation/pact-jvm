package au.com.dius.pact.model

import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.matchingrules.TypeMatcher
import groovy.json.JsonSlurper
import spock.lang.Specification

class PactSerialiserSpec extends Specification {

  private Request request
  private Response response

  private provider
  private consumer
  private requestWithMatchers
  private responseWithMatchers
  private interactionsWithMatcher
  private pactWithMatchers

  def loadTestFile(String name) {
    PactSerialiserSpec.classLoader.getResourceAsStream(name)
  }

  def setup() {
    request = new Request('GET', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'))
    response = new Response(200, [testreqheader: 'testreqheaderval'],
      OptionalBody.body('{"responsetest":true}'))
    provider = new Provider('test_provider')
    consumer = new Consumer('test_consumer')
    def requestMatchers = new MatchingRules()
    requestMatchers.addCategory('body').addRule('$.test', new TypeMatcher())
    requestWithMatchers = new Request('GET', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'), requestMatchers
    )
    def responseMatchers = new MatchingRules()
    responseMatchers.addCategory('body').addRule('$.responsetest', new TypeMatcher())
    responseWithMatchers = new Response(200, [testreqheader: 'testreqheaderval'],
      OptionalBody.body('{"responsetest":true}'), responseMatchers
    )
    interactionsWithMatcher = new RequestResponseInteraction('test interaction with matchers',
      [new ProviderState('test state')], requestWithMatchers, responseWithMatchers)
    pactWithMatchers = new RequestResponsePact(provider, consumer, [interactionsWithMatcher])
  }

  def 'PactSerialiser must serialise pact'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)

    when:
    PactWriter.writePact(new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')], request, response)]),
      new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must serialise V3 pact'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact_v3.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)
    def expectedRequest = new Request('GET', '/',
      ['q': ['p', 'p2'], 'r': ['s']], [testreqheader: 'testreqheadervalue'],
      OptionalBody.body('{"test": true}'))
    def expectedResponse = new Response(200, [testreqheader: 'testreqheaderval'],
      OptionalBody.body('{"responsetest" : true}'))
    def expectedPact = new RequestResponsePact(new Provider('test_provider'),
      new Consumer('test_consumer'), [
        new RequestResponseInteraction('test interaction', [
          new ProviderState('test state', [name: 'Testy']),
          new ProviderState('test state 2', [name: 'Testy2'])
        ], expectedRequest, expectedResponse)
      ])

    when:
    PactWriter.writePact(expectedPact, new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must serialise pact with matchers'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact_matchers.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)

    when:
    PactWriter.writePact(pactWithMatchers, new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must convert methods to uppercase'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)
    def pact = new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')],
        ModelFixtures.requestLowerCaseMethod,
        ModelFixtures.response)])

    when:
    PactWriter.writePact(pact, new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must de-serialise pact'() {
    expect:
    pact.provider == new Provider('test_provider')
    pact.consumer == new Consumer('test_consumer')
    pact.interactions.size() == 1
    pact.interactions[0].description == 'test interaction'
    pact.interactions[0].providerStates == [new ProviderState('test state')]
    pact.interactions[0].request == request
    pact.interactions[0].response == response

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact.json'))
  }

  def 'PactSerialiser must de-serialise V3 pact'() {
    expect:
    pact.provider == new Provider('test_provider')
    pact.consumer == new Consumer('test_consumer')
    pact.interactions.size() == 1
    pact.interactions[0].description == 'test interaction'
    pact.interactions[0].providerStates == [
      new ProviderState('test state', [name: 'Testy']), new ProviderState('test state 2', [name: 'Testy2'])]
    pact.interactions[0].request == request
    pact.interactions[0].response == response

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_v3.json'))
  }

  def 'PactSerialiser must de-serialise pact with matchers'() {
    expect:
    pact == pactWithMatchers

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_matchers.json'))
  }

  def 'PactSerialiser must de-serialise pact matchers in old format'() {
    expect:
    pact == pactWithMatchers

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_matchers_old_format.json'))
  }

  def 'PactSerialiser must convert http methods to upper case'() {
    expect:
    pact == new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')], request, response)])

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_lowercase_method.json'))
  }

  def 'PactSerialiser must not convert fields called \'body\''() {
    expect:
    pactBody == new JsonSlurper().parseText('{\n' +
      '  "body" : [ 1, 2, 3 ],\n' +
      '  "complete" : {\n' +
      '    "body" : 123456,\n' +
      '    "certificateUri" : "http://...",\n' +
      '    "issues" : {\n' +
      '      "idNotFound" : { }\n' +
      '    },\n' +
      '    "nevdis" : {\n' +
      '      "body" : null,\n' +
      '      "colour" : null,\n' +
      '      "engine" : null\n' +
      '    }\n' +
      '  }\n' +
      '}')

    where:
    pactBody = new JsonSlurper().parseText(
      PactReader.loadPact(loadTestFile('test_pact_with_bodies.json')).interactions[0].request.body.value )
  }

  def 'PactSerialiser must deserialise pact with no bodies'() {
    expect:
    pact == new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction with no bodies', [new ProviderState('test state')],
        ModelFixtures.requestNoBody, ModelFixtures.responseNoBody)])

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_bodies.json'))
  }

  def 'PactSerialiser must deserialise pact with query in old format'() {
    expect:
    pact == new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')], request, response)])

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_query_old_format.json'))
  }

  def 'PactSerialiser must deserialise pact with no version'() {
    expect:
    pact == new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')], request, response)])

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_version.json'))
  }

  def 'PactSerialiser must deserialise pact with no specification version'() {
    expect:
    pact == new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')], request, response)])

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_spec_version.json'))
  }

  def 'PactSerialiser must deserialise pact with no metadata'() {
    expect:
    pact == new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')], request, response)])

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_metadata.json'))
  }

  def 'PactSerialiser must deserialise pact with encoded query string'() {
    expect:
    pact == new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [new RequestResponseInteraction('test interaction', [new ProviderState('test state')],
        ModelFixtures.requestDecodedQuery, ModelFixtures.response)])

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_encoded_query.json'))
  }

}
