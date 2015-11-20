package au.com.dius.pact.model

import spock.lang.Specification
import spock.lang.Unroll

class PactSpec extends Specification {
  private pact, interaction, request, response, provider, consumer

  def setup() {
    request = new Request('Get', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], '{"test":true}')
    response = new Response(200, [testreqheader: 'testreqheaderval'], '{"responsetest":true}')
    interaction = new Interaction('test interaction', 'test state', request, response)
    provider = new Provider('test_provider')
    consumer = new Consumer('test_consumer')
    pact = new Pact(provider, consumer, [interaction])
  }

  def 'Pact should Locate Interactions'() {
    given:
    def description = 'descriptiondata'
    def state = 'stateData'

    def interaction = new Interaction(
      description,
      state,
      new Request('Get', ''),
      new Response(200)
    )

    def pact = new Pact(
      new Provider('foo'),
      new Consumer('bar'),
      [interaction]
    )

    when:
    def result = pact.interactionFor(description, state)

    then:
    result == interaction
  }

  def 'Pact merge should allow different descriptions'() {
    given:
    def newInteraction = new Interaction('different', 'test state', request, response)

    when:
    def result = PactMerge$.MODULE$.merge(pact, new Pact(provider, consumer, [newInteraction]))
    def expected = new Pact(provider, consumer, [interaction] + newInteraction)
    expected.sortInteractions()

    then:
    result == new MergeSuccess(expected)
  }

  def 'Pact merge should allow different states'() {
    given:
    def newInteraction = new Interaction('test interaction', 'different', request, response)

    when:
    def result = PactMerge$.MODULE$.merge(pact, new Pact(provider, consumer, [newInteraction]))
    def expected = new Pact(provider, consumer, [interaction] + newInteraction)
    expected.sortInteractions()

    then:
    result == new MergeSuccess(expected)
  }

  def 'Pact merge should allow identical interactions without duplication'() {
    when:
    def result = PactMerge$.MODULE$.merge(pact, pact)

    then:
    result == new MergeSuccess(pact)
  }

  def 'Pact merge should refuse different requests for identical description and states'() {
    given:
    def differentRequest = request.copy()
    differentRequest.path = 'different'
    def newInteraction = new Interaction('test interaction', 'test state', differentRequest, response)
    def pactCopy = new Pact(pact.provider, pact.consumer, [newInteraction])

    when:
    def result = PactMerge$.MODULE$.merge(pact, pactCopy)

    then:
    result instanceof MergeConflict
  }

  def 'Pact merge should refuse different responses for identical description and states'() {
    given:
    def differentResponse = response.copy()
    differentResponse.status = 503
    def newInteraction = new Interaction('test interaction', 'test state', request, differentResponse)
    def pactCopy = new Pact(pact.provider, pact.consumer, [newInteraction])

    when:
    def result = PactMerge$.MODULE$.merge(pact, pactCopy)

    then:
    result instanceof MergeConflict
  }

  @SuppressWarnings('LineLength')
  @Unroll
  def 'Pact mimeType'() {
    expect:
    request.mimeType() == mimeType

    where:
    request                                                                                          | mimeType
    new Request('Get', '')                                                                           | 'text/plain'
    new Request('Get', '', null, ['Content-Type': 'text/html'])                                      | 'text/html'
    new Request('Get', '', null, ['Content-Type': 'application/json; charset=UTF-8'])                | 'application/json'
    new Request('Get', '', null, null, '{"json": true}')                                             | 'application/json'
    new Request('Get', '', null, null, '{}')                                                         | 'application/json'
    new Request('Get', '', null, null, '[]')                                                         | 'application/json'
    new Request('Get', '', null, null, '[1,2,3]')                                                    | 'application/json'
    new Request('Get', '', null, null, '"string"')                                                   | 'application/json'
    new Request('Get', '', null, null, '<?xml version="1.0" encoding="UTF-8"?>\n<json>false</json>') | 'application/xml'
    new Request('Get', '', null, null, '<json>false</json>')                                         | 'application/xml'
    new Request('Get', '', null, null, 'this is not json')                                           | 'text/plain'
    new Request('Get', '', null, null, '<html><body>this is also not json</body></html>')            | 'text/html'
  }

}
