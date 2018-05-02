package au.com.dius.pact.model

import spock.lang.Specification

class RequestResponsePactSpec extends Specification {

  private static Provider provider
  private static Consumer consumer
  private static RequestResponseInteraction interaction

  def setupSpec() {
    provider = new Provider()
    consumer = new Consumer()
    interaction = new RequestResponseInteraction(request: new Request(method: 'GET'),
      response: new Response(body: OptionalBody.body('{"value": 1234.0}'),
        headers: ['Content-Type': 'application/json']))
  }

  def 'when writing V2 spec, query parameters must be encoded appropriately'() {
    given:
    def pact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction(request: new Request(method: 'GET', query: [a: ['b=c&d']]),
        response: new Response())
    ])

    when:
    def result = pact.toMap(PactSpecVersion.V2)

    then:
    result.interactions.first().request.query == 'a=b%3Dc%26d'
  }

  def 'should handle body types other than JSON'() {
    given:
    def pact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction(request: new Request(method: 'PUT',
        body: OptionalBody.body('<?xml version="1.0"><root/>'),
        headers: ['Content-Type': 'application/xml']),
        response: new Response(body: OptionalBody.body('Ok, no prob'), headers: ['Content-Type': 'text/plain']))
    ])

    when:
    def result = pact.toMap(PactSpecVersion.V3)

    then:
    result.interactions.first().request.body == '<?xml version="1.0"><root/>'
    result.interactions.first().response.body == 'Ok, no prob'
  }

  def 'does not lose the scale for decimal numbers'() {
    given:
    def pact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction(request: new Request(method: 'GET'),
        response: new Response(body: OptionalBody.body('{"value": 1234.0}'),
          headers: ['Content-Type': 'application/json']))
    ])

    when:
    def result = pact.toMap(PactSpecVersion.V3)

    then:
    result.interactions.first().response.body.toString() == '{value=1234.0}'
  }

  @SuppressWarnings('ComparisonWithSelf')
  def 'equality test'() {
    expect:
    pact == pact

    where:
    pact = new RequestResponsePact(provider, consumer, [ interaction ])
  }

  def 'pacts are not equal if the providers are different'() {
    expect:
    pact != pact2

    where:
    provider2 = new Provider('other provider')
    pact = new RequestResponsePact(provider, consumer, [ interaction ])
    pact2 = new RequestResponsePact(provider2, consumer, [ interaction ])
  }

  def 'pacts are not equal if the consumers are different'() {
    expect:
    pact != pact2

    where:
    consumer2 = new Consumer('other consumer')
    pact = new RequestResponsePact(provider, consumer, [ interaction ])
    pact2 = new RequestResponsePact(provider, consumer2, [ interaction ])
  }

  def 'pacts are equal if the metadata is different'() {
    expect:
    pact == pact2

    where:
    pact = new RequestResponsePact(provider, consumer, [ interaction ], [meta: 'data'])
    pact2 = new RequestResponsePact(provider, consumer, [ interaction ], [meta: 'other data'])
  }

  def 'pacts are not equal if the interactions are different'() {
    expect:
    pact != pact2

    where:
    interaction2 = new RequestResponseInteraction(request: new Request(method: 'POST'),
      response: new Response(body: OptionalBody.body('{"value": 1234.0}'),
        headers: ['Content-Type': 'application/json']))
    pact = new RequestResponsePact(provider, consumer, [ interaction ])
    pact2 = new RequestResponsePact(provider, consumer, [ interaction2 ])
  }

  def 'pacts are not equal if the number of interactions are different'() {
    expect:
    pact != pact2

    where:
    interaction2 = new RequestResponseInteraction(request: new Request(method: 'POST'),
      response: new Response(body: OptionalBody.body('{"value": 1234.0}'),
        headers: ['Content-Type': 'application/json']))
    pact = new RequestResponsePact(provider, consumer, [ interaction ])
    pact2 = new RequestResponsePact(provider, consumer, [ interaction, interaction2 ])
  }

  def 'when filtering the pact, do not loose the source of the pact'() {
    given:
    def source = new BrokerUrlSource('url', 'brokerUrl')
    def pact = new RequestResponsePact(provider, consumer, [ interaction ])
    pact.source = source

    when:
    pact.filterInteractions { true }

    then:
    pact.source == source
  }
}
