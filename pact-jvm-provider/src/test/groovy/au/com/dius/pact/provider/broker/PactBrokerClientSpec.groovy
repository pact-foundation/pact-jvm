package au.com.dius.pact.provider.broker

import spock.lang.Specification

class PactBrokerClientSpec extends Specification {

  def 'when fetching consumers, sets the auth if there is any'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> args.first().call([name: 'bob', href: 'http://bob.com/']) }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }
    client.options.authentication = ['Basic', '1', '2']

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().pactFile == new URL('http://bob.com/')
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

}
