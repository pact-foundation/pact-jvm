package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.broker.InvalidHalResponse
import au.com.dius.pact.provider.broker.PactBrokerClient
import spock.lang.Specification

@PactBroker(host = 'pactbroker.host', port = '1000', failIfNoPactsFound = false)
class PactBrokerLoaderSpec extends Specification {

  private Closure<PactBrokerLoader> pactBrokerLoader
  private String host
  private String port
  private String protocol
  private List tags
  private PactBrokerClient brokerClient
  private Pact mockPact

  void setup() {
    host = 'pactbroker'
    port = '1234'
    protocol = 'http'
    tags = ['latest']
    brokerClient = Mock(PactBrokerClient)
    mockPact = Mock(Pact)

    pactBrokerLoader = { boolean failIfNoPactsFound = true ->
      def loader = new PactBrokerLoader(host, port, protocol, tags) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url) throws URISyntaxException {
          brokerClient
        }

        @Override
        Pact loadPact(ConsumerInfo consumer) {
          mockPact
        }
      }
      loader.failIfNoPactsFound = failIfNoPactsFound
      loader
    }
  }

  def 'Raises an exception if the pact broker client returns an empty list'() {
    when:
    pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> []
    thrown(NoPactsFoundException)
  }

  def 'Returns Empty List if flagged to do so and the pact broker client returns an empty list'() {
    when:
    def result = pactBrokerLoader(false).load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> []
    result == []
  }

  def 'Throws any Exception On Execution Exception'() {
    given:
    brokerClient.fetchConsumers('test') >> { throw new InvalidHalResponse('message') }

    when:
    pactBrokerLoader().load('test')

    then:
    thrown(InvalidHalResponse)
  }

  def 'Throws an Exception if the broker URL is invalid'() {
    given:
    host = '!@#%$^%$^^'

    when:
    pactBrokerLoader().load('test')

    then:
    thrown(IOException)
  }

  void 'Loads Pacts Configured From A Pact Broker Annotation'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(this.class.getAnnotation(PactBroker)) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url) throws URISyntaxException {
          assert url.host == 'pactbroker.host'
          assert url.port == 1000
          brokerClient
        }
      }
    }

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result == []
    1 * brokerClient.fetchConsumers('test') >> []
  }

  def 'Loads pacts for each provided tag'() {
    given:
    tags = ['latest', 'a', 'b', 'c']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithTag('test', 'latest') >> [ new ConsumerInfo('test', 'latest') ]
    1 * brokerClient.fetchConsumersWithTag('test', 'a') >> [ new ConsumerInfo('test', 'a') ]
    1 * brokerClient.fetchConsumersWithTag('test', 'b') >> [ new ConsumerInfo('test', 'b') ]
    1 * brokerClient.fetchConsumersWithTag('test', 'c') >> [ new ConsumerInfo('test', 'c') ]
    result.size() == 4
  }

  def 'Loads the latest pacts if no tag is provided'() {
    given:
    tags = []

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result.size() == 1
    1 * brokerClient.fetchConsumers('test') >> [ new ConsumerInfo('test', 'latest') ]
  }

}
