package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.provider.junit.loader.PactFilter
import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import spock.lang.Specification

class MessagePactRunnerSpec extends Specification {

  private List<Pact> pacts
  private au.com.dius.pact.core.model.Consumer consumer, consumer2
  private au.com.dius.pact.core.model.Provider provider
  private List<RequestResponseInteraction> interactions
  private List<Message> interactions2
  private MessagePact messagePact

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @PactFilter('State 1')
  @IgnoreNoPactsToVerify
  class TestClass {
    @TestTarget
    Target target
  }

  @Provider('myAwesomeService')
  @PactFolder('pacts')
  @IgnoreNoPactsToVerify
  class TestClass2 {
    @TestTarget
    Target target
  }

  def setup() {
    consumer = new au.com.dius.pact.core.model.Consumer('Consumer 1')
    consumer2 = new au.com.dius.pact.core.model.Consumer('Consumer 2')
    provider = new au.com.dius.pact.core.model.Provider('myAwesomeService')
    interactions = [
      new RequestResponseInteraction('Req 1', [
        new ProviderState('State 1')
      ], new Request(), new Response()),
      new RequestResponseInteraction('Req 2', [
        new ProviderState('State 1'),
        new ProviderState('State 2')
      ], new Request(), new Response())
    ]
    interactions2 = [
      new Message('Req 3', [
        new ProviderState('State 1')
      ], OptionalBody.body('{}'.bytes)),
      new Message('Req 4', [
        new ProviderState('State X')
      ], OptionalBody.empty())
    ]
    messagePact = new MessagePact(provider, consumer2, interactions2)
    pacts = [
      new RequestResponsePact(provider, consumer, interactions),
      messagePact
    ]
  }

  def 'only verifies message pacts'() {
    given:
    MessagePactRunner pactRunner = new MessagePactRunner(TestClass)

    when:
    def result = pactRunner.filterPacts(pacts)

    then:
    result.size() == 1
    result*.pact.contains(messagePact)
  }

  def 'handles filtered pacts'() {
    given:
    MessagePactRunner pactRunner = new MessagePactRunner(TestClass2)
    pacts = [ new FilteredPact(messagePact, { true }) ]

    when:
    def result = pactRunner.filterPacts(pacts)

    then:
    result.size() == 1
  }

}
