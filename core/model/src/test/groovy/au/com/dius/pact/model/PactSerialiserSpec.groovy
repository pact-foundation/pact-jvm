package au.com.dius.pact.model

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.PactWriter
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact
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
  private interactionsWithGenerators
  private pactWithMatchers
  private pactWithGenerators
  private messagePactWithGenerators

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
    def requestMatchers = new MatchingRulesImpl()
    requestMatchers.addCategory('body').addRule('$.test', TypeMatcher.INSTANCE)
    requestWithMatchers = new Request('GET', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'), requestMatchers
    )
    def responseMatchers = new MatchingRulesImpl()
    responseMatchers.addCategory('body').addRule('$.responsetest', TypeMatcher.INSTANCE)
    responseWithMatchers = new Response(200, [testreqheader: 'testreqheaderval'],
      OptionalBody.body('{"responsetest":true}'), responseMatchers
    )
    interactionsWithMatcher = new RequestResponseInteraction('test interaction with matchers',
      [new ProviderState('test state')], requestWithMatchers, responseWithMatchers)
    pactWithMatchers = new RequestResponsePact(provider, consumer, [interactionsWithMatcher])

    def requestWithGenerators = request.copy()
    requestWithGenerators.generators = new Generators([(Category.BODY): ['a': new RandomIntGenerator(10, 20)]])
    def responseWithGenerators = response.copy()
    responseWithGenerators.generators = new Generators([(Category.PATH): ['': new RandomStringGenerator(20)]])
    interactionsWithGenerators = new RequestResponseInteraction('test interaction with generators',
      [new ProviderState('test state')], requestWithGenerators, responseWithGenerators)
    pactWithGenerators = new RequestResponsePact(provider, consumer, [interactionsWithGenerators])

    messagePactWithGenerators = new MessagePact(provider, consumer, [ new Message('Test Message',
      [new ProviderState('message exists')], OptionalBody.body('"Test Message"'), new MatchingRulesImpl(),
      new Generators([(Category.BODY): ['a': UuidGenerator.INSTANCE]]), [contentType: 'application/json']) ])
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

  def 'PactSerialiser must serialise pact with generators'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact_generators.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)

    when:
    PactWriter.writePact(pactWithGenerators, new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must serialise message pact with generators'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('v3-message-pact-generators.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)

    when:
    PactWriter.writePact(messagePactWithGenerators, new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'Correctly handle non-ascii characters'() {
    given:
    def file = File.createTempFile('non-ascii-pact', '.json')
    def fw = new FileWriter(file)
    def request = new Request(body: OptionalBody.body('"This is a string with letters ä, ü, ö and ß"'))
    def response = new Response(body: OptionalBody.body('"This is a string with letters ä, ü, ö and ß"'))
    def interaction = new RequestResponseInteraction('test interaction with non-ascii characters in bodies',
      null, request, response)
    def pact = new RequestResponsePact(new Provider('test_provider'), new Consumer('test_consumer'),
      [interaction])

    when:
    def writer = new PrintWriter(fw)
    PactWriter.writePact(pact, writer, PactSpecVersion.V2)
    writer.close()
    def pactJson = file.text

    then:
    pactJson.contains('This is a string with letters ä, ü, ö and ß')

    cleanup:
    file.delete()
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

  def 'PactSerialiser must de-serialise pact with generators'() {
    expect:
    pact == pactWithGenerators

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_generators.json'))
  }

  def 'PactSerialiser must de-serialise message pact with generators'() {
    expect:
    pact == messagePactWithGenerators

    where:
    pact = PactReader.loadPact(loadTestFile('v3-message-pact-generators.json'))
  }

}
