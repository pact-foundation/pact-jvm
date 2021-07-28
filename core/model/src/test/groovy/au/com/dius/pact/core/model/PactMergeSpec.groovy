package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('PrivateFieldCouldBeFinal')
class PactMergeSpec extends Specification {
  @Shared
  private Consumer consumer = new Consumer('test_consumer'), consumer2 = new Consumer('other consumer')
  @Shared
  private Provider provider = new Provider('test_provider'), provider2 = new Provider('other provider')
  @Shared
  private pact, interaction, request, response

  def setup() {
    request = new Request('Get', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'.bytes))
    response = new Response(200, [testreqheader: 'testreqheaderval'],
      OptionalBody.body('{"responsetest":true}'.bytes))
    interaction = new RequestResponseInteraction('test interaction',
      [new ProviderState('test state')], request, response, null)
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
    result.message.startsWith 'Cannot merge pacts as they are not compatible - Provider names are different'

    where:
    type << [RequestResponsePact, MessagePact]
    newPact << [new RequestResponsePact(provider, consumer, []), new MessagePact(provider, consumer, [])]
    existingPact << [new RequestResponsePact(provider2, consumer, []), new MessagePact(provider2, consumer, [])]
    result = PactMerge.merge(newPact, existingPact)
  }

  def 'Pacts with different types are not compatible'() {
    given:
    def newPact = new RequestResponsePact(provider, consumer, [])
    def existingPact = new MessagePact(provider, consumer, [])

    when:
    def result = PactMerge.merge(newPact, existingPact)

    then:
    !result.ok
    result.message.startsWith 'Cannot merge pacts as they are not compatible - Pact types different'
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
        new RequestResponseInteraction('test', [new ProviderState('test')], new Request(),
          new Response(), null)
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', [new ProviderState('test')], OptionalBody.empty())
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
        new RequestResponseInteraction('test', [new ProviderState('test')], new Request(),
          new Response(), null)
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', [new ProviderState('test')], OptionalBody.empty())
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
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(),
        new Response(), null) ]),
      new MessagePact(provider, consumer, [ new Message('test', [new ProviderState('test')]) ]) ]
    existingPact << [ new RequestResponsePact(provider, consumer, [
      new RequestResponseInteraction('test', [new ProviderState('test')], new Request(),
        new Response(), null) ]),
      new MessagePact(provider, consumer, [ new Message('test', [new ProviderState('test')]) ]) ]
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
        new RequestResponseInteraction('test', [new ProviderState('test')], new Request(),
          new Response(), null),
        new RequestResponseInteraction('test 2', [new ProviderState('test')], new Request(),
          new Response(), null),
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', [new ProviderState('test')]),
        new Message('test 2', [new ProviderState('test')])
      ])
    ]
    existingPact << [ new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', [new ProviderState('test')], new Request('POST'),
          new Response(), null)
      ]),
      new MessagePact(provider, consumer, [ new Message('test', [new ProviderState('test')],
        OptionalBody.body('a b c'.bytes)) ])
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
        new RequestResponseInteraction('test', [new ProviderState('test')], new Request(),
          new Response(), null),
        new RequestResponseInteraction('test 2', [new ProviderState('test')],
          new Request('POST'), new Response(), null),
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', [new ProviderState('test')]),
        new Message('test 2', [new ProviderState('test')], OptionalBody.body('1 2 3'.bytes))
      ])
    ]
    existingPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test', [new ProviderState('test')],
          new Request(), new Response(), null)
      ]),
      new MessagePact(provider, consumer, [
        new Message('test', [new ProviderState('test')])
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
      new MessagePact(provider, consumer, [ new Message('test interaction',
        [new ProviderState('test state')]) ])
    ]
    newPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('different', [new ProviderState('test state')], request,
          response, null)
      ]),
      new MessagePact(provider, consumer, [ new Message('different', [new ProviderState('test state')]) ])
    ]
    result = PactMerge.merge(oldPact, newPact)
    expected << [
      new RequestResponsePact(provider, consumer, [interaction] +
        new RequestResponseInteraction('different', [new ProviderState('test state')], request,
          response, null)),
      new MessagePact(provider, consumer, [
        new Message('test interaction', [new ProviderState('test state')]),
        new Message('different', [new ProviderState('test state')])
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
      new MessagePact(provider, consumer, [ new Message('test interaction', [new ProviderState('test state')]) ])
    ]
    newPact << [
      new RequestResponsePact(provider, consumer, [
        new RequestResponseInteraction('test interaction', [new ProviderState('different')], request, response, null)
      ]),
      new MessagePact(provider, consumer, [ new Message('test interaction', [new ProviderState('different')]) ])
    ]
    result = PactMerge.merge(oldPact, newPact)
    expected << [
      new RequestResponsePact(provider, consumer, [interaction] +
        new RequestResponseInteraction('test interaction', [new ProviderState('different')], request, response, null)),
      new MessagePact(provider, consumer, [
        new Message('test interaction', [new ProviderState('test state')]),
        new Message('test interaction', [new ProviderState('different')])
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
      new MessagePact(provider, consumer, [ new Message('test interaction', [new ProviderState('test state')]) ])
    ]
    result = PactMerge.merge(identicalPact, identicalPact)
  }
}
