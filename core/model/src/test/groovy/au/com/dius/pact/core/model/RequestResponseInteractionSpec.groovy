package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class RequestResponseInteractionSpec extends Specification {

  private RequestResponseInteraction interaction
  private generators
  private Request request

  def setup() {
    generators = new Generators([(Category.HEADER): [a: new RandomStringGenerator(4)]])
    request = new Request(generators: generators)
    interaction = new RequestResponseInteraction('test interaction', [
      new ProviderState('state one'), new ProviderState('state two', [value: 'one', other: '2'])],
      request, new Response(generators: generators))
  }

  def 'creates a V3 map format if V3 spec'() {
    when:
    def map = interaction.toMap(PactSpecVersion.V3)

    then:
    map == [
      description: 'test interaction',
      request: [method: 'GET', path: '/', generators: [header: [a: [type: 'RandomString', size: 4]]]],
      response: [status: 200, generators: [header: [a: [type: 'RandomString', size: 4]]]],
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
    interaction = new RequestResponseInteraction('test interaction', [],
      new Request(generators: generators), new Response(generators: generators))
    def mapV3 = interaction.toMap(PactSpecVersion.V3)
    def mapV2 = interaction.toMap(PactSpecVersion.V2)

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

  @Unroll
  def 'displayState test'() {
    expect:
    new RequestResponseInteraction(stateDescription, providerStates)
      .displayState() == stateDescription

    where:

    providerStates                                               | stateDescription
    []                                                           | 'None'
    [new ProviderState('')]                                      | 'None'
    [new ProviderState('state 1')]                               | 'state 1'
    [new ProviderState('state 1'), new ProviderState('state 2')] | 'state 1, state 2'
  }

  @Issue('#1018')
  def 'correctly encodes the query parameters when V2 format'() {
    given:
    request.query = ['include[]': ['term', 'total_scores', 'license', 'is_public', 'needs_grading_count', 'permissions',
                                   'current_grading_period_scores', 'course_image', 'favorites']]

    when:
    def map = interaction.toMap(PactSpecVersion.V2)

    then:
    map.request.query == 'include[]=term&include[]=total_scores&include[]=license&include[]=is_public&' +
      'include[]=needs_grading_count&include[]=permissions&include[]=current_grading_period_scores&' +
      'include[]=course_image&include[]=favorites'
  }
}
