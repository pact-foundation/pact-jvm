package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.support.json.JsonValue
import kotlin.Pair
import spock.lang.Specification

class HttpInteractionBuilderSpec extends Specification {
  HttpInteractionBuilder builder
  String description
  List<ProviderState> states
  List<JsonValue.StringValue> comments

  def setup() {
    description = 'test'
    states = []
    comments = []
    builder = new HttpInteractionBuilder(description, states, comments)
  }

  def 'default builder values'() {
    when:
    def interaction = builder.build()

    then:
    interaction.v4
    interaction.description == 'test'
    interaction.key == ''
    interaction.comments.size() == 0
    interaction.providerStates.size() == 0
    !interaction.pending
  }

  def 'allows setting the unique key'() {
    when:
    def interaction = builder.key('key').build()

    then:
    interaction.key == 'key'
  }

  def 'allows setting the description'() {
    when:
    def interaction = builder.description('description').build()

    then:
    interaction.description == 'description'
  }

  def 'allows adding provider states'() {
    when:
    def interaction = builder
      .state('state1')
      .state('state2', [a: 'b', c: 'd'])
      .state('state3', 'a', 'b')
      .state('state4', new Pair('a', 100), new Pair('b', 1000))
      .build()

    then:
    interaction.providerStates == [
      new ProviderState('state1'),
      new ProviderState('state2', [a: 'b', c: 'd']),
      new ProviderState('state3', [a: 'b']),
      new ProviderState('state4', [a: 100, b: 1000])
    ]
  }

  def 'allows marking the interaction as pending'() {
    when:
    def interaction = builder.pending(true).build()

    then:
    interaction.pending
  }

  def 'allows adding comments to the interaction'() {
    when:
    def interaction = builder
      .comment('comment1')
      .comment('comment2')
      .build()

    then:
    interaction.comments['text'].values*.asString() == [
      'comment1',
      'comment2'
    ]
  }

  def 'allows building a request part'() {
    when:
    def interaction = builder
      .withRequest { it.method('post') }
      .build()

    then:
    interaction.asSynchronousRequestResponse().request == new HttpRequest('post')
  }

  def 'allows building a response part'() {
    when:
    def interaction = builder
      .willRespondWith { it.status(333) }
      .build()

    then:
    interaction.asSynchronousRequestResponse().response == new HttpResponse(333)
  }
}
