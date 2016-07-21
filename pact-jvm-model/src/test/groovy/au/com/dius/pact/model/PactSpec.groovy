package au.com.dius.pact.model

import spock.lang.Specification

class PactSpec extends Specification {
  private pact, interaction, request, response, provider, consumer

  def setup() {
    request = new Request('Get', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'))
    response = new Response(200, [testreqheader: 'testreqheaderval'], OptionalBody.body('{"responsetest":true}'))
    interaction = new RequestResponseInteraction('test interaction', [new ProviderState('test state')],
      request, response)
    provider = new Provider('test_provider')
    consumer = new Consumer('test_consumer')
    pact = new RequestResponsePact(provider, consumer, [interaction])
  }

  def 'Pact should Locate Interactions'() {
    given:
    def description = 'descriptiondata'
    def state = new ProviderState('stateData')

    def interaction = new RequestResponseInteraction(
      description,
      [state],
      new Request('Get', ''),
      new Response(200)
    )

    def pact = new RequestResponsePact(
      new Provider('foo'),
      new Consumer('bar'),
      [interaction]
    )

    when:
    def result = pact.interactionFor(description, state.name)

    then:
    result == interaction
  }

  def 'Pact merge should allow different descriptions'() {
    given:
    def newInteraction = new RequestResponseInteraction('different', [new ProviderState('test state')],
      request, response)

    when:
    def result = PactMerge$.MODULE$.merge(pact, new RequestResponsePact(provider, consumer, [newInteraction]))
    def expected = new RequestResponsePact(provider, consumer, [interaction] + newInteraction)
    expected.sortInteractions()

    then:
    result == new MergeSuccess(expected)
  }

  def 'Pact merge should allow different states'() {
    given:
    def newInteraction = new RequestResponseInteraction('test interaction', [new ProviderState('different')],
      request, response)

    when:
    def result = PactMerge$.MODULE$.merge(pact, new RequestResponsePact(provider, consumer, [newInteraction]))
    def expected = new RequestResponsePact(provider, consumer, [interaction] + newInteraction)
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
    def newInteraction = new RequestResponseInteraction('test interaction', [new ProviderState('test state')],
      differentRequest, response)
    def pactCopy = new RequestResponsePact(pact.provider, pact.consumer, [newInteraction])

    when:
    def result = PactMerge$.MODULE$.merge(pact, pactCopy)

    then:
    result instanceof MergeConflict
  }

  def 'Pact merge should refuse different responses for identical description and states'() {
    given:
    def differentResponse = response.copy()
    differentResponse.status = 503
    def newInteraction = new RequestResponseInteraction('test interaction', [new ProviderState('test state')],
      request, differentResponse)
    def pactCopy = new RequestResponsePact(pact.provider, pact.consumer, [newInteraction])

    when:
    def result = PactMerge$.MODULE$.merge(pact, pactCopy)

    then:
    result instanceof MergeConflict
  }

}
