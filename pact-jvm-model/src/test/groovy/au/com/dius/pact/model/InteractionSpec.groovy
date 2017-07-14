package au.com.dius.pact.model

import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class InteractionSpec extends Specification {

  private Request request, request2
  private Response response
  private ProviderState state

  def setup() {
    request = new Request('GET', '/')
    request2 = new Request('POST', '/')
    response = new Response(200)
    state = new ProviderState('state')
  }

  @Unroll
  def 'display state should show a description of the state'() {
    expect:
    new RequestResponseInteraction(providerStates: [state]).displayState() == description

    where:
    state                           | description
    new ProviderState('some state') | 'some state'
    new ProviderState('')           | 'None'
    new ProviderState(null)         | 'None'
  }

  @Ignore('conflict logic needs to be fixed')
  def 'interactions do not conflict if their descriptions are different'() {
    given:
    RequestResponseInteraction one = new RequestResponseInteraction('One', [state], request, response)
    RequestResponseInteraction two = new RequestResponseInteraction('Two', [state], request2, response)

    expect:
    !one.conflictsWith(two)
  }

  @Ignore('conflict logic needs to be fixed')
  def 'interactions do not conflict if their provider states are different'() {
    given:
    RequestResponseInteraction one = new RequestResponseInteraction('One', [new ProviderState('state one')],
      request, response)
    RequestResponseInteraction two = new RequestResponseInteraction('One', [new ProviderState('state two')],
      request2, response)

    expect:
    !one.conflictsWith(two)
  }

  @Ignore('conflict logic needs to be fixed')
  def 'interactions do conflict if their requests are different'() {
    given:
    RequestResponseInteraction one = new RequestResponseInteraction('One', [state], request, response)
    RequestResponseInteraction two = new RequestResponseInteraction('One', [state], request2, response)

    expect:
    one.conflictsWith(two)
  }

  @Ignore('conflict logic needs to be fixed')
  def 'interactions do conflict if their responses are different'() {
    given:
    RequestResponseInteraction one = new RequestResponseInteraction('One', [state], request, response)
    RequestResponseInteraction two = new RequestResponseInteraction('One', [state], request, new Response(400))

    expect:
    one.conflictsWith(two)
  }

  @Ignore('conflict logic needs to be fixed')
  def 'interactions do not conflict if they are equal'() {
    given:
    RequestResponseInteraction one = new RequestResponseInteraction('One', [state], request, response)
    RequestResponseInteraction two = new RequestResponseInteraction('One', [state], request, response)

    expect:
    !one.conflictsWith(two)
  }

}
