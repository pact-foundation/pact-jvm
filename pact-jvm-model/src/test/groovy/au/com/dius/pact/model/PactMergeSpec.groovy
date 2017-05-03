package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PactMergeSpec extends Specification {
  @Shared
  private Consumer consumer, consumer2
  @Shared
  private Provider provider, provider2
  @Shared
  private pact, interaction, request, response

  def setup() {
    request = new Request('Get', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'))
    response = new Response(200, [testreqheader: 'testreqheaderval'], OptionalBody.body('{"responsetest":true}'))
    interaction = new RequestResponseInteraction('test interaction', 'test state', request, response)
    provider = new Provider('test_provider')
    provider2 = new Provider('other provider')
    consumer = new Consumer('test_consumer')
    consumer2 = new Consumer('other consumer')
    pact = new RequestResponsePact(provider, consumer, [interaction])
  }

  @Unroll
  def 'Pacts with different consumers are compatible for #type'() {
    expect:
    PactMerge.merge(newPact, existingPact).ok

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [ new RequestResponsePact(provider, consumer2, []), new MessagePact(provider, consumer2, []) ]
    existingPact << [ new RequestResponsePact(provider, consumer, []), new MessagePact(provider, consumer, []) ]
  }

  @Unroll
  def 'Pacts with different providers are not compatible for #type'() {
    expect:
    !result.ok
    result.message == 'Cannot merge pacts as they are not compatible'

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [new RequestResponsePact(provider, consumer, []), new MessagePact(provider, consumer, [])]
    existingPact << [new RequestResponsePact(provider2, consumer, []), new MessagePact(provider2, consumer, [])]
    result = PactMerge.merge(newPact, existingPact)
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

  @Unroll
  def 'two empty compatible pacts merge ok for #type'() {
    expect:
    result.ok

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [new RequestResponsePact(provider, consumer, []), new MessagePact(provider, consumer, [])]
    existingPact << [new RequestResponsePact(provider, consumer, []), new MessagePact(provider, consumer, [])]
    result = PactMerge.merge(newPact, existingPact)
  }

  @Unroll
  def 'empty pact merges with any compatible pact for #type'() {
    expect:
    result.ok

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [new RequestResponsePact(provider, consumer, []), new MessagePact(provider, consumer, [])]
    existingPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', 'test', new Request(), new Response())
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', 'test', new OptionalBody())
      ])
    ]
    result = PactMerge.merge(newPact, existingPact)
  }

  @Unroll
  def 'any compatible pact merges with an empty pact for #type'() {
    expect:
    result.ok

    where:
    type << [RequestResponsePact, MessagePact]
    existingPact << [new RequestResponsePact(provider, consumer, []), new MessagePact(provider, consumer, [])]
    newPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', 'test', new Request(), new Response())
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', 'test', new OptionalBody())
      ])
    ]
    result = PactMerge.merge(newPact, existingPact)
  }

  @Unroll
  def 'two compatible pacts merge if their interactions are compatible for #type'() {
    expect:
    result.ok

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [ new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', 'test', new Request(), new Response()) ]),
      new MessagePact(provider, consumer, [ new Message('test', 'test') ]) ]
    existingPact << [ new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', 'test', new Request(), new Response()) ]),
      new MessagePact(provider, consumer, [ new Message('test', 'test') ]) ]
    result = PactMerge.merge(newPact, existingPact)
  }

  @Unroll
  @Ignore('conflict logic needs to be fixed')
  def 'two compatible pacts do not merge if their interactions have conflicts for #type'() {
    expect:
    !result.ok
    result.message == 'Cannot merge pacts as there were 1 conflicts between the interactions'

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [ new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', 'test', new Request(), new Response()),
        new RequestResponseInteraction('test 2', 'test', new Request(), new Response()),
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', 'test'),
        new Message('test 2', 'test')
      ])
    ]
    existingPact << [ new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', 'test', new Request('POST'), new Response())
      ]),
      new MessagePact(provider, consumer, [ new Message('test', 'test', OptionalBody.body('a b c'), null,
        [contentType: 'text/plain']) ])
    ]
    result = PactMerge.merge(newPact, existingPact)
  }

  @Unroll
  def 'pact merge removes duplicates for #type'() {
    expect:
    result.ok
    result.result.interactions.size() == 2
    result.result.interactions*.description == ['test', 'test 2']

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', 'test', new Request(), new Response()),
        new RequestResponseInteraction('test 2', 'test', new Request('POST'), new Response()),
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', 'test'),
        new Message('test 2', 'test', OptionalBody.body('1 2 3'))
      ])
    ]
    existingPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', 'test', new Request(), new Response())
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', 'test')
      ])
    ]
    result = PactMerge.merge(newPact, existingPact)
  }

  @Unroll
  def 'Pact merge should allow different descriptions for #type'() {
    expect:
    result.ok
    result.result.interactions.size() == 2
    expected.sortInteractions()
    result.result == expected

    where:
    type << [RequestResponsePact, MessagePact]
    oldPact << [
      new RequestResponsePact(provider, consumer, [interaction]),
      new MessagePact(provider, consumer, [ new Message('test interaction', 'test state') ])
    ]
    newPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('different', 'test state', request, response)
      ]),
      new MessagePact(provider, consumer, [ new Message('different', 'test state') ])
    ]
    result = PactMerge.merge(oldPact, newPact)
    expected << [
      new RequestResponsePact(provider, consumer, [interaction] +
        new RequestResponseInteraction('different', 'test state', request, response)),
      new MessagePact(provider, consumer, [
        new Message('test interaction', 'test state'),
        new Message('different', 'test state')
      ])
    ]
  }

  @Unroll
  def 'Pact merge should allow different states for #type'() {
    expect:
    result.ok
    result.result.interactions.size() == 2
    expected.sortInteractions()
    result.result == expected

    where:
    type << [RequestResponsePact, MessagePact]
    oldPact << [
      new RequestResponsePact(provider, consumer, [interaction]),
      new MessagePact(provider, consumer, [ new Message('test interaction', 'test state') ])
    ]
    newPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test interaction', 'different', request, response)
      ]),
      new MessagePact(provider, consumer, [ new Message('test interaction', 'different') ])
    ]
    result = PactMerge.merge(oldPact, newPact)
    expected << [
      new RequestResponsePact(provider, consumer, [interaction] +
        new RequestResponseInteraction('test interaction', 'different', request, response)),
      new MessagePact(provider, consumer, [
        new Message('test interaction', 'test state'),
        new Message('test interaction', 'different')
      ])
    ]
  }

  @Unroll
  def 'Pact merge should allow identical interactions without duplication for #type'() {
    expect:
    result.ok
    result.result.interactions.size() == 1

    where:
    type << [RequestResponsePact, MessagePact]
    identicalPact << [
      pact,
      new MessagePact(provider, consumer, [ new Message('test interaction', 'test state') ])
    ]
    result = PactMerge.merge(identicalPact, identicalPact)
  }

  @Unroll
  @Ignore('conflict logic needs to be fixed')
  def 'Pact merge should refuse different requests for identical description and states for #type'() {
    expect:
    !result.ok

    where:
    type << [RequestResponsePact, MessagePact]
    basePact << [
      pact, new MessagePact(provider, consumer, [ new Message('test interaction', 'test state') ])
    ]
    newPact << [
      new RequestResponsePact(pact.provider, pact.consumer, [
        new RequestResponseInteraction('test interaction', 'test state',
          new Request('Get', '/different', PactReader.queryStringToMap('q=p&q=p2&r=s'),
            [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}')), response)
      ]),
      new MessagePact(provider, consumer, [ new Message('test interaction', 'test state', OptionalBody.body('a b c'),
        null, [contentType: 'text/plain']) ])
    ]
    result = PactMerge.merge(basePact, newPact)
  }

  @Ignore('conflict logic needs to be fixed')
  def 'Pact merge should refuse different responses for identical description and states'() {
    given:
    def differentResponse = response.copy()
    differentResponse.status = 503
    def newInteraction = new RequestResponseInteraction('test interaction', 'test state', request, differentResponse)
    def pactCopy = new RequestResponsePact(pact.provider, pact.consumer, [newInteraction])

    when:
    def result = PactMerge.merge(pact, pactCopy)

    then:
    !result.ok
  }

}
