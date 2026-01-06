package au.com.dius.pact.core.model

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
    new RequestResponseInteraction('Test', [state]).displayState() == description

    where:
    state                           | description
    new ProviderState('some state') | 'some state'
    new ProviderState('')           | 'None'
    new ProviderState(null)         | 'None'
  }
}
