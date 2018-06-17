package au.com.dius.pact.core.model

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

}
