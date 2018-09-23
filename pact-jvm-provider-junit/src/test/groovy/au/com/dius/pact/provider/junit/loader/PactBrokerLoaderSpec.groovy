package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.model.Pact
import au.com.dius.pact.pactbroker.InvalidHalResponse
import au.com.dius.pact.pactbroker.PactBrokerConsumer
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.broker.PactBrokerClient
import au.com.dius.pact.provider.junit.sysprops.ValueResolver
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.VALUES_SEPARATOR

class PactBrokerLoaderSpec extends Specification {

  private Closure<PactBrokerLoader> pactBrokerLoader
  private String host
  private String port
  private String protocol
  private List tags
  private List consumers
  private PactBrokerClient brokerClient
  private Pact mockPact

  void setup() {
    host = 'pactbroker'
    port = '1234'
    protocol = 'http'
    tags = ['latest']
    consumers = []
    brokerClient = Mock(PactBrokerClient)
    mockPact = Mock(Pact)

    pactBrokerLoader = { boolean failIfNoPactsFound = true ->
      def loader = new PactBrokerLoader(host, port, protocol, tags, consumers) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) throws URISyntaxException {
          brokerClient
        }

        @Override
        Pact loadPact(ConsumerInfo consumer, Map options) {
          mockPact
        }
      }
      loader.failIfNoPactsFound = failIfNoPactsFound
      loader
    }
  }

  def 'Returns an empty list if the pact broker client returns an empty list'() {
    when:
    def list = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> []
    notThrown(NoPactsFoundException)
    list.empty
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
      new PactBrokerLoader(FullPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) throws URISyntaxException {
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

  @RestoreSystemProperties
  void 'Uses fallback PactBroker System Properties'() {
    given:
    System.setProperty('pactbroker.host', 'my.pactbroker.host')
    System.setProperty('pactbroker.port', '4711')
    pactBrokerLoader = {
      new PactBrokerLoader(MinimalPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) throws URISyntaxException {
          assert url.host == 'my.pactbroker.host'
          assert url.port == 4711
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

  @RestoreSystemProperties
  void 'Fails when no fallback system properties are set'() {
    given:
    System.clearProperty('pactbroker.host')
    System.clearProperty('pactbroker.port')
    pactBrokerLoader = {
      new PactBrokerLoader(MinimalPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        PactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) throws URISyntaxException {
          assert url.host == 'my.pactbroker.host'
          assert url.port == 4711
          brokerClient
        }
      }
    }

    when:
    pactBrokerLoader().load('test')

    then:
    Exception exception = thrown(Exception)
    exception.message.startsWith('Invalid pact broker port')
  }

  def 'Loads pacts for each provided tag'() {
    given:
    tags = ['a', 'b', 'c']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithTag('test', 'a') >> [ new PactBrokerConsumer('test', 'a', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'b') >> [ new PactBrokerConsumer('test', 'b', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'c') >> [ new PactBrokerConsumer('test', 'c', '', []) ]
    0 * _
    result.size() == 3
  }

  def 'Loads latest pacts together with other tags'() {
    given:
    tags = ['a', 'latest', 'b']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithTag('test', 'a') >> [ new PactBrokerConsumer('test', 'a', '', []) ]
    1 * brokerClient.fetchConsumers('test') >> [ new PactBrokerConsumer('test', 'latest', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'b') >> [ new PactBrokerConsumer('test', 'b', '', []) ]
    0 * _
    result.size() == 3
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Processes tags before pact load'() {
    given:
    System.setProperty('composite', "one${VALUES_SEPARATOR}two")
    tags = ['${composite}']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithTag('test', 'one') >> [ new PactBrokerConsumer('test', 'one', '', []) ]
    1 * brokerClient.fetchConsumersWithTag('test', 'two') >> [ new PactBrokerConsumer('test', 'two', '', []) ]
    result.size() == 2
  }

  def 'Loads the latest pacts if no tag is provided'() {
    given:
    tags = []

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result.size() == 1
    1 * brokerClient.fetchConsumers('test') >> [ new PactBrokerConsumer('test', 'latest', '', []) ]
  }

  @SuppressWarnings('GStringExpressionWithinString')
  def 'processes tags with the provided value resolver'() {
    given:
    tags = ['${a}', '${latest}', '${b}']
    def loader = pactBrokerLoader()
    loader.valueResolver = [resolveValue: { val -> 'X' } ] as ValueResolver

    when:
    def result = loader.load('test')

    then:
    3 * brokerClient.fetchConsumersWithTag('test', 'X') >> [ new PactBrokerConsumer('test', 'a', '', []) ]
    0 * _
    result.size() == 3
  }

  def 'Loads pacts only for provided consumers'() {
    given:
    consumers = ['a', 'b', 'c']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> [
            new PactBrokerConsumer('a', 'latest', '', []),
            new PactBrokerConsumer('b', 'latest', '', []),
            new PactBrokerConsumer('c', 'latest', '', []),
            new PactBrokerConsumer('d', 'latest', '', [])
    ]
    0 * _
    result.size() == 3
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Processes consumers before pact load'() {
    given:
    System.setProperty('composite', "a${VALUES_SEPARATOR}b${VALUES_SEPARATOR}c")
    consumers = ['${composite}']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> [
            new PactBrokerConsumer('a', 'latest', '', []),
            new PactBrokerConsumer('b', 'latest', '', []),
            new PactBrokerConsumer('c', 'latest', '', []),
            new PactBrokerConsumer('d', 'latest', '', [])
    ]
    0 * _
    result.size() == 3
  }

  def 'Loads all consumer pacts if no consumer is provided'() {
    given:
    consumers = []

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> [
            new PactBrokerConsumer('a', 'latest', '', []),
            new PactBrokerConsumer('b', 'latest', '', []),
            new PactBrokerConsumer('c', 'latest', '', []),
            new PactBrokerConsumer('d', 'latest', '', [])
    ]
    0 * _
    result.size() == 4
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Loads all consumers by default'() {
    given:
    consumers = ['${pactbroker.consumers:}']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumers('test') >> [
      new PactBrokerConsumer('a', 'latest', '', []),
      new PactBrokerConsumer('b', 'latest', '', []),
      new PactBrokerConsumer('c', 'latest', '', []),
      new PactBrokerConsumer('d', 'latest', '', [])
    ]
    0 * _
    result.size() == 4
  }

  def 'Loads pacts only for provided consumers with the specified tags'() {
    given:
    consumers = ['a', 'b', 'c']
    tags = ['demo']

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithTag('test', 'demo') >> [
            new PactBrokerConsumer('a', 'demo', '', []),
            new PactBrokerConsumer('b', 'demo', '', []),
            new PactBrokerConsumer('c', 'demo', '', []),
            new PactBrokerConsumer('d', 'demo', '', [])
    ]
    0 * _
    result.size() == 3
  }

  @PactBroker(host = 'pactbroker.host', port = '1000', failIfNoPactsFound = false)
  static class FullPactBrokerAnnotation {

  }

  @PactBroker(failIfNoPactsFound = false)
  static class MinimalPactBrokerAnnotation {

  }

}
