package au.com.dius.pact.model

import spock.lang.Specification

class RequestResponseInteractionSpec extends Specification {

  def interaction

  def setup() {
    interaction = new RequestResponseInteraction('test interaction', [
      new ProviderState('state one'), new ProviderState('state two', [value: 'one', other: '2'])],
      new Request(), new Response()
    )
  }

  def 'creates a V3 map format if V3 spec'() {
    when:
    def map = interaction.toMap()

    then:
    map == [
      description: 'test interaction',
      request: [method: 'GET', path: '/'],
      response: [status: 200],
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
}
