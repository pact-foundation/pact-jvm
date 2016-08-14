package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import spock.lang.Specification

class PactMergeSpec extends Specification {

  private Consumer consumer
  private Provider provider
  private pact, interaction, request, response

  def setup() {
    request = new Request('Get', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'))
    response = new Response(200, [testreqheader: 'testreqheaderval'], OptionalBody.body('{"responsetest":true}'))
    interaction = new RequestResponseInteraction('test interaction',
      [new ProviderState('test state')], request, response)
    provider = new Provider('test_provider')
    consumer = new Consumer('test_consumer')
    pact = new RequestResponsePact(provider, consumer, [interaction])
  }

  def 'Pacts with different consumers are compatible'() {
    given:
    def newPact = new RequestResponsePact(provider, new Consumer('other consumer'), [])
    def existingPact = new RequestResponsePact(provider, consumer, [])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    result.ok
  }

  def 'Pacts with different providers are not compatible'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [])
    def existingPact = new RequestResponsePact(new Provider('other provider'), consumer, [])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    !result.ok
    result.message == 'Cannot merge pacts as they are not compatible'
  }

  def 'Pacts with different types are not compatible'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [])
    def existingPact = new MessagePact(new Provider('other provider'), consumer, [])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    !result.ok
    result.message == 'Cannot merge pacts as they are not compatible'
  }

  def 'two empty compatible pacts merge ok'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [])
    def existingPact = new RequestResponsePact(provider, consumer, [])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    result.ok
  }

  def 'empty pact merges with any compatible pact'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [])
    def existingPact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(), new Response())
    ])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    result.ok
  }

  def 'any compatible pact merges with an empty pact'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(), new Response())
    ])
    def existingPact = new RequestResponsePact(provider, consumer, [])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    result.ok
  }

  def 'two compatible pacts merge if their interactions are compatible'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(), new Response())
    ])
    def existingPact = new RequestResponsePact(provider, consumer, [])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    result.ok
  }

  def 'two compatible pacts do not merge if their interactions have conflicts'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(), new Response()),
      new RequestResponseInteraction('test 2', [new ProviderState('test')], new Request(), new Response()),
    ])
    def existingPact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request('POST'), new Response())
    ])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    !result.ok
    result.message == 'Cannot merge pacts as there were 1 conflicts between the interactions'
  }

  def 'pact merge removes duplicates'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(), new Response()),
      new RequestResponseInteraction('test 2', [new ProviderState('test')], new Request('POST'), new Response()),
    ])
    def existingPact = new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(), new Response())
    ])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    result.ok
    result.result.interactions.size() == 2
    result.result.interactions*.description == ['test', 'test 2']
  }

  def 'Pact merge should allow different descriptions'() {
    given:
    def newInteraction = new RequestResponseInteraction('different',
      [new ProviderState('test state')], request, response)

    when:
    def result = PactMerge.merge(pact, new RequestResponsePact(provider, consumer, [newInteraction]))
    def expected = new RequestResponsePact(provider, consumer, [interaction] + newInteraction)
    expected.sortInteractions()

    then:
    result.ok
    result.result == expected
  }

  def 'Pact merge should allow different states'() {
    given:
    def newInteraction = new RequestResponseInteraction('test interaction',
      [new ProviderState('different')], request, response)

    when:
    def result = PactMerge.merge(pact, new RequestResponsePact(provider, consumer, [newInteraction]))
    def expected = new RequestResponsePact(provider, consumer, [interaction] + newInteraction)
    expected.sortInteractions()

    then:
    result.ok
    result.result == expected
  }

  def 'Pact merge should allow identical interactions without duplication'() {
    when:
    def result = PactMerge.merge(pact, pact)

    then:
    result.ok
  }

  def 'Pact merge should refuse different requests for identical description and states'() {
    given:
    def differentRequest = request.copy()
    differentRequest.path = 'different'
    def newInteraction = new RequestResponseInteraction('test interaction',
      [new ProviderState('test state')], differentRequest, response)
    def pactCopy = new RequestResponsePact(pact.provider, pact.consumer, [newInteraction])

    when:
    def result = PactMerge.merge(pact, pactCopy)

    then:
    !result.ok
  }

  def 'Pact merge should refuse different responses for identical description and states'() {
    given:
    def differentResponse = response.copy()
    differentResponse.status = 503
    def newInteraction = new RequestResponseInteraction('test interaction',
      [new ProviderState('test state')], request, differentResponse)
    def pactCopy = new RequestResponsePact(pact.provider, pact.consumer, [newInteraction])

    when:
    def result = PactMerge.merge(pact, pactCopy)

    then:
    !result.ok
  }

}
