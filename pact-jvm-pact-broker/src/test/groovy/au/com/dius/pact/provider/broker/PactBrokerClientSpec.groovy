package au.com.dius.pact.provider.broker

import spock.lang.Specification

@SuppressWarnings('UnnecessaryGetter')
class PactBrokerClientSpec extends Specification {

  private PactBrokerClient pactBrokerClient
  private File pactFile
  private String pactContents

  def setup() {
    pactBrokerClient = new PactBrokerClient('http://localhost:8080')
    pactFile = File.createTempFile('pact', '.json')
    pactContents = '''
      {
          "provider" : {
              "name" : "Provider"
          },
          "consumer" : {
              "name" : "Foo Consumer"
          },
          "interactions" : []
      }
    '''
    pactFile.write pactContents
  }

  def 'when fetching consumers, sets the auth if there is any'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> args.first().call([name: 'bob', href: 'http://bob.com/']) }

    def client = GroovySpy(PactBrokerClient, global: true, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }
    client.options.authentication = ['Basic', '1', '2']

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> throw new NotFoundHalResponse() }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumers('provider')

    then:
    consumers == []
  }

  def 'fetches consumers with specified tag successfully'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> args.first().call([name: 'bob', href: 'http://bob.com/']) }

    def client = GroovySpy(PactBrokerClient, global: true, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers != []
    consumers.first().name == 'bob'
    consumers.first().source == 'http://bob.com/'
  }

  def 'when fetching consumers with specified tag, sets the auth if there is any'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> args.first().call([name: 'bob', href: 'http://bob.com/']) }

    def client = GroovySpy(PactBrokerClient, global: true, constructorArgs: ['http://pactBrokerUrl']) {
      newHalClient() >> halClient
    }
    client.options.authentication = ['Basic', '1', '2']

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers.first().pactFileAuthentication == ['Basic', '1', '2']
  }

  def 'when fetching consumers with specified tag for an unknown provider, returns an empty pacts list'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    halClient.navigate(_, _) >> halClient
    halClient.pacts(_) >> { args -> throw new NotFoundHalResponse() }

    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }

    when:
    def consumers = client.fetchConsumersWithTag('provider', 'tag')

    then:
    consumers == []
  }

  def 'returns an error when uploading a pact fails'() {
    given:
    def halClient = GroovyMock(HalClient, global: true)
    def client = GroovySpy(PactBrokerClient, global: true) {
      newHalClient() >> halClient
    }

    when:
    def result = client.uploadPactFile(pactFile, '10.0.0')

    then:
    1 * halClient.uploadJson('/pacts/provider/Provider/consumer/Foo Consumer/version/10.0.0', pactContents, _) >>
      { args -> args[2].call('Failed', 'Error') }
    result == 'FAILED! Error'
  }
}
