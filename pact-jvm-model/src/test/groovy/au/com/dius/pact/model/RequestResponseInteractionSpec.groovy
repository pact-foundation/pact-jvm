package au.com.dius.pact.model

import au.com.dius.pact.model.generators.Category
import au.com.dius.pact.model.generators.Generators
import au.com.dius.pact.model.generators.RandomStringGenerator
import spock.lang.Specification

class RequestResponseInteractionSpec extends Specification {

  def interaction, generators

  def setup() {
    generators = new Generators([(Category.HEADER): [a: new RandomStringGenerator(4)]])
    interaction = new RequestResponseInteraction('test interaction', [
      new ProviderState('state one'), new ProviderState('state two', [value: 'one', other: '2'])],
      new Request(generators: generators), new Response(generators: generators)
    )
  }

  def 'creates a V3 map format if V3 spec'() {
    when:
    def map = interaction.toMap()

    then:
    map == [
      description: 'test interaction',
      request: [method: 'GET', path: '/', generators: [header: [a: [type: 'RandomStringGenerator', size: 4]]]],
      response: [status: 200, generators: [header: [a: [type: 'RandomStringGenerator', size: 4]]]],
      providerStates: [
        [name: 'state one'],
        [name: 'state two', params: [
          value: 'one', other: '2']
        ]
      ]
    ]

  }

  def 'creates a V2 map format if not V3 spec'() {
    when:
    def map = interaction.toMap(PactSpecVersion.V1_1)

    then:
    map == [
      description: 'test interaction',
      request: [method: 'GET', path: '/'],
      response: [status: 200],
      providerState: 'state one'
    ]
  }

  def 'does not include a provide state if there is not any'() {
    when:
    interaction.providerStates = []
    def mapV3 = interaction.toMap()
    def mapV2 = interaction.toMap(PactSpecVersion.V3)

    then:
    !mapV3.containsKey('providerStates')
    !mapV3.containsKey('providerState')
    !mapV2.containsKey('providerStates')
    !mapV2.containsKey('providerState')
  }

  def 'unique key test'() {
    expect:
    interaction1.uniqueKey() == interaction1.uniqueKey()
    interaction1.uniqueKey() == interaction2.uniqueKey()
    interaction1.uniqueKey() != interaction3.uniqueKey()
    interaction1.uniqueKey() != interaction4.uniqueKey()
    interaction1.uniqueKey() != interaction5.uniqueKey()
    interaction3.uniqueKey() != interaction4.uniqueKey()
    interaction3.uniqueKey() != interaction5.uniqueKey()
    interaction4.uniqueKey() != interaction5.uniqueKey()

    where:
    interaction1 = new RequestResponseInteraction('description 1+2')
    interaction2 = new RequestResponseInteraction('description 1+2')
    interaction3 = new RequestResponseInteraction('description 1+2', [new ProviderState('state 3')])
    interaction4 = new RequestResponseInteraction('description 4')
    interaction5 = new RequestResponseInteraction('description 4', [new ProviderState('state 5')])
  }

}
