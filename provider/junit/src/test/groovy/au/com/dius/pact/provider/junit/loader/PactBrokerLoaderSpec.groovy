package au.com.dius.pact.provider.junit.loader

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.pactbroker.InvalidHalResponse
import au.com.dius.pact.core.pactbroker.InvalidNavigationRequest
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import au.com.dius.pact.core.support.expressions.ExpressionParser
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector
import au.com.dius.pact.provider.junitsupport.loader.NoPactsFoundException
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerLoader
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import javax.net.ssl.SSLHandshakeException
import java.lang.annotation.Annotation

import static au.com.dius.pact.core.support.expressions.ExpressionParser.VALUES_SEPARATOR

@SuppressWarnings(['LineLength', 'UnnecessaryGetter', 'GStringExpressionWithinString', 'ClassSize'])
class PactBrokerLoaderSpec extends Specification {

  private Closure<PactBrokerLoader> pactBrokerLoader
  private String host
  private String port
  private String protocol
  private String url
  private List tags
  private List<VersionSelector> consumerVersionSelectors
  private List consumers
  private String enablePendingPacts
  private List<String> providerTags
  private String includeWipPactsSince
  private IPactBrokerClient brokerClient
  private Pact mockPact
  private PactReader mockReader
  private ValueResolver valueResolver
  private ExpressionParser expressionParser

  void setup() {
    host = 'pactbroker'
    port = '1234'
    protocol = 'http'
    url = null
    tags = []
    consumerVersionSelectors = []
    consumers = []
    enablePendingPacts = ''
    providerTags = []
    includeWipPactsSince = ''
    brokerClient = Mock(IPactBrokerClient) {
      getOptions() >> [:]
    }
    mockPact = Mock(Pact)
    mockReader = Mock(PactReader) {
      loadPact(_) >> mockPact
    }
    valueResolver = null
    expressionParser = new ExpressionParser()

    pactBrokerLoader = { boolean failIfNoPactsFound = true ->
      IPactBrokerClient client = brokerClient
      def loader = new PactBrokerLoader(host, port, protocol, tags, consumerVersionSelectors, consumers,
        failIfNoPactsFound, null, null, valueResolver, enablePendingPacts, providerTags, includeWipPactsSince, url,
        expressionParser) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
          client
        }
      }
      loader.pactReader = mockReader
      loader
    }
  }

  def 'Returns an empty list if the pact broker client returns an empty list'() {
    when:
    def list = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithSelectors('test', _, _, _, _) >> new Ok([])
    notThrown(NoPactsFoundException)
    list.empty
  }

  def 'Returns Empty List if flagged to do so and the pact broker client returns an empty list'() {
    when:
    def result = pactBrokerLoader(false).load('test')

    then:
    1 * brokerClient.fetchConsumersWithSelectors('test', _, _, _, _) >> new Ok([])
    result == []
  }

  def 'Throws any Exception On Execution Exception'() {
    given:
    1 * brokerClient.fetchConsumersWithSelectors('test', _, _, _, _) >> new Err(new InvalidHalResponse('message'))

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
    thrown(IllegalArgumentException)
  }

  def 'Throws an Exception if the broker host has a slash'() {
    given:
    host = 'pactflow.io/'

    when:
    pactBrokerLoader().load('test')

    then:
    thrown(IllegalArgumentException)
  }

  void 'Loads Pacts Configured From A Pact Broker Annotation'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(FullPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
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
    1 * brokerClient.fetchConsumersWithSelectors('test', _, _, _, _) >> new Ok([])
  }

  @RestoreSystemProperties
  void 'Uses fallback PactBroker System Properties'() {
    given:
    System.setProperty('pactbroker.host', 'my.pactbroker.host')
    System.setProperty('pactbroker.port', '4711')
    pactBrokerLoader = {
      new PactBrokerLoader(MinimalPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
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
    1 * brokerClient.fetchConsumersWithSelectors('test', _, _, _, _) >> new Ok([])
  }

  @RestoreSystemProperties
  void 'Uses fallback PactBroker System Properties for URL'() {
    given:
    System.setProperty('pactbroker.url', 'http://my.pactbroker.host:4751')
    pactBrokerLoader = {
      new PactBrokerLoader(MinimalPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
          assert url.host == 'my.pactbroker.host'
          assert url.port == 4751
          assert url.toString() == 'http://my.pactbroker.host:4751'
          brokerClient
        }
      }
    }

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result == []
    1 * brokerClient.fetchConsumersWithSelectors('test', _, _, _, _) >> new Ok([])
  }

  @RestoreSystemProperties
  void 'Uses fallback PactBroker System Properties for PactSource'() {
    given:
    host = 'my.pactbroker.host'
    port = '4711'
    url = 'http://my.pactbroker.host:4711'
    System.setProperty('pactbroker.host', host)
    System.setProperty('pactbroker.port', port)
    System.setProperty('pactbroker.url', url)

    when:
    def pactSource = new PactBrokerLoader(MinimalPactBrokerAnnotation.getAnnotation(PactBroker)).pactSource

    then:
    assert pactSource instanceof PactBrokerSource

    def pactBrokerSource = (PactBrokerSource) pactSource
    assert pactBrokerSource.scheme == 'http'
    assert pactBrokerSource.host == null
    assert pactBrokerSource.port == null
    assert pactBrokerSource.url == url
  }

  @RestoreSystemProperties
  void 'Fails when no fallback system properties are set'() {
    given:
    System.clearProperty('pactbroker.host')
    System.clearProperty('pactbroker.port')
    System.clearProperty('pactbroker.url')
    pactBrokerLoader = {
      new PactBrokerLoader(MinimalPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
          assert url.host == 'my.pactbroker.host'
          assert url.port == 4711
          brokerClient
        }
      }
    }

    when:
    pactBrokerLoader().load('test')

    then:
    IllegalArgumentException exception = thrown(IllegalArgumentException)
    exception.message.startsWith('Invalid pact broker host specified')
  }

  @RestoreSystemProperties
  void 'Does not fail when no fallback port system properties is set'() {
    given:
    System.setProperty('pactbroker.host', 'my.pactbroker.host')
    System.clearProperty('pactbroker.port')
    pactBrokerLoader = {
      new PactBrokerLoader(MinimalPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
          assert url.host == 'my.pactbroker.host'
          assert url.port == -1
          brokerClient
        }
      }
    }

    when:
    pactBrokerLoader().load('test')

    then:
    noExceptionThrown()
    1 * brokerClient.fetchConsumersWithSelectors('test', _, _, _, _) >> new Ok([])
  }

  def 'Loads pacts for each provided tag'() {
    given:
    tags = ['a', 'b', 'c']
    def selectors = [
      new ConsumerVersionSelector('a', true, null, null),
      new ConsumerVersionSelector('b', true, null, null),
      new ConsumerVersionSelector('c', true, null, null)
    ]
    def expected = [
      new PactBrokerResult('test', 'a', '', [], [], false, null, false, true),
      new PactBrokerResult('test', 'b', '', [], [], false, null, false, true),
      new PactBrokerResult('test', 'c', '', [], [], false, null, false, true)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  def 'Loads pacts for each provided consumer version selector'() {
    given:
    consumerVersionSelectors = [
      createVersionSelector(tag: 'a', latest: 'true'),
      createVersionSelector(tag: 'b', latest: 'false'),
      createVersionSelector(tag: 'c', latest: 'true')
    ]
    def selectors = [
      new ConsumerVersionSelector('a', true, '', null),
      new ConsumerVersionSelector('b', false, '', null),
      new ConsumerVersionSelector('c', true, '', null)
    ]
    def expected = [
      new PactBrokerResult('test', 'a', '', [], [], false, null, false, true),
      new PactBrokerResult('test', 'b', '', [], [], false, null, false, true),
      new PactBrokerResult('test', 'c', '', [], [], false, null, false, true)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Processes tags before pact load'() {
    given:
    System.setProperty('composite', "one${VALUES_SEPARATOR}two")
    tags = ['${composite}']
    def selectors = [
      new ConsumerVersionSelector('one', true, null, null),
      new ConsumerVersionSelector('two', true, null, null)
    ]
    def expected = [
      new PactBrokerResult('test', 'one', '', [], [], false, null, false, true),
      new PactBrokerResult('test', 'two', '', [], [], false, null, false, true)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    result.size() == 2
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Processes consumer version selectors before pact load'() {
    given:
    System.setProperty('compositeTag', "one${VALUES_SEPARATOR}two")
    System.setProperty('compositeLatest', "true${VALUES_SEPARATOR}false")
    consumerVersionSelectors = [createVersionSelector(tag: '${compositeTag}', latest: '${compositeLatest}')]
    def selectors = [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('two', false, '', null)
    ]
    def expected = [
      new PactBrokerResult('test', 'one', '', [], [], false, null, false, true),
      new PactBrokerResult('test', 'two', '', [], [], false, null, false, true)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    result.size() == 2
  }

  def 'Loads the latest pacts if no consumer version selector or tag is provided'() {
    given:
    tags = []
    def expected = [ new PactBrokerResult('test', 'latest', '', [], [], false, null, false, true) ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result.size() == 1
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> new Ok(expected)
  }

  @SuppressWarnings('GStringExpressionWithinString')
  def 'processes tags with the provided value resolver'() {
    given:
    tags = ['${a}', '${latest}', '${b}']
    def loader = pactBrokerLoader()
    loader.valueResolver = [resolveValue: { val -> 'X' } ] as ValueResolver
    def expected = [ new PactBrokerResult('test', 'a', '', [], [], false, null, false, true) ]
    def selectors = [
      new ConsumerVersionSelector('X', true, null, null),
      new ConsumerVersionSelector('X', true, null, null),
      new ConsumerVersionSelector('X', true, null, null)
    ]

    when:
    def result = loader.load('test')

    then:
    1 * brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 1
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'processes consumer version selectors with the provided value resolver'() {
    given:
    consumerVersionSelectors = [
      createVersionSelector(tag: '${a}', latest: 'true'),
      createVersionSelector(tag: '${latest}', latest: 'false'),
      createVersionSelector(tag: '${c}', latest: 'true', fallbackTag: '${d}')
    ]
    def newLoader = pactBrokerLoader()
    newLoader.valueResolver = [resolveValue: { val -> val == 'd' ? 'D' : 'X' }] as ValueResolver
    def expected = [ new PactBrokerResult('test', 'a', '', [], [], false, null, false, true) ]
    def selectors = [
      new ConsumerVersionSelector('X', true, '', null),
      new ConsumerVersionSelector('X', false, '', null),
      new ConsumerVersionSelector('X', true, '', 'D')
    ]

    when:
    def result = newLoader.load('test')

    then:
    1 * brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 1
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Uses true for latest when only tags are specified for consumer version selector'() {
    given:
    System.setProperty('compositeTag', "one${VALUES_SEPARATOR}two${VALUES_SEPARATOR}three")
    consumerVersionSelectors = [createVersionSelector(tag: '${compositeTag}')]
    def selectors = [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('two', true, '', null),
      new ConsumerVersionSelector('three', true, '', null)
    ]
    def expected = [
      new PactBrokerResult('test', 'one', '', [], [], false, null, false, true),
      new PactBrokerResult('test', 'two', '', [], [], false, null, false, true)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    result.size() == 2
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Throws exception if consumer version selector properties do not match in length'() {
    given:
    System.setProperty('compositeTag', "one${VALUES_SEPARATOR}two${VALUES_SEPARATOR}three")
    System.setProperty('compositeLatest', "true${VALUES_SEPARATOR}false")
    consumerVersionSelectors = [createVersionSelector(tag: '${compositeTag}', latest: '${compositeLatest}')]

    when:
    pactBrokerLoader().load('test')

    then:
    thrown(IllegalArgumentException)
  }

  def 'Loads pacts only for provided consumers'() {
    given:
    consumers = ['a', 'b', 'c']
    def selectors = [
      new ConsumerVersionSelector(null, true, 'a', null),
      new ConsumerVersionSelector(null, true, 'b', null),
      new ConsumerVersionSelector(null, true, 'c', null)
    ]
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, null, false, false),
      new PactBrokerResult('b', '', '', [], [], false, null, false, false),
      new PactBrokerResult('c', '', '', [], [], false, null, false, false),
      new PactBrokerResult('d', '', '', [], [], false, null, false, false)
    ]

    when:
    def result = pactBrokerLoader.call().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  def 'Loads pacts only for provided consumers on the selector'() {
    given:
    consumerVersionSelectors = [
      createVersionSelector(consumer: 'a'),
      createVersionSelector(consumer: 'b'),
      createVersionSelector(consumer: 'c')
    ]
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, null, false, true),
      new PactBrokerResult('b', '', '', [], [], false, null, false, true),
      new PactBrokerResult('c', '', '', [], [], false, null, false, true),
      new PactBrokerResult('d', '', '', [], [], false, null, false, true)
    ]
    def selectors = [
      new ConsumerVersionSelector(null, true, 'a', null),
      new ConsumerVersionSelector(null, true, 'b', null),
      new ConsumerVersionSelector(null, true, 'c', null)
    ]

    when:
    def result = pactBrokerLoader.call().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 4
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Processes consumers before pact load'() {
    given:
    System.setProperty('composite', "a${VALUES_SEPARATOR}b${VALUES_SEPARATOR}c")
    consumers = ['${composite}']
    def selectors = [
      new ConsumerVersionSelector(null, true, 'a', null),
      new ConsumerVersionSelector(null, true, 'b', null),
      new ConsumerVersionSelector(null, true, 'c', null)
    ]
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, null, false, false),
      new PactBrokerResult('b', '', '', [], [], false, null, false, false),
      new PactBrokerResult('c', '', '', [], [], false, null, false, false),
      new PactBrokerResult('d', '', '', [], [], false, null, false, false)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  def 'Loads all consumer pacts if no consumer is provided'() {
    given:
    consumers = []
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, null, false, false),
      new PactBrokerResult('b', '', '', [], [], false, null, false, false),
      new PactBrokerResult('c', '', '', [], [], false, null, false, false),
      new PactBrokerResult('d', '', '', [], [], false, null, false, false)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 4
  }

  @RestoreSystemProperties
  @SuppressWarnings('GStringExpressionWithinString')
  def 'Loads all consumers by default'() {
    given:
    consumers = ['${pactbroker.consumers:}']
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, null, false, false),
      new PactBrokerResult('b', '', '', [], [], false, null, false, false),
      new PactBrokerResult('c', '', '', [], [], false, null, false, false),
      new PactBrokerResult('d', '', '', [], [], false, null, false, false)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 4
  }

  def 'Loads pacts only for provided consumers with the specified tags'() {
    given:
    consumers = ['a', 'b', 'c']
    tags = ['demo']
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('b', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('c', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('d', '', '', [], [], false, 'demo', false, false)
    ]
    def selectors = [
      new ConsumerVersionSelector('demo', true, 'a', null),
      new ConsumerVersionSelector('demo', true, 'b', null),
      new ConsumerVersionSelector('demo', true, 'c', null)
    ]

    when:
    def result = pactBrokerLoader.call().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  def 'Loads pacts only for provided consumers with the specified consumer version selectors'() {
    given:
    consumers = ['a', 'b', 'c']
    consumerVersionSelectors = [createVersionSelector(tag: 'demo', latest: 'true')]
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('b', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('c', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('d', '', '', [], [], false, 'demo', false, false)
    ]
    def selectors = [ new ConsumerVersionSelector('demo', true, '', null) ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  def 'Falls back to tags when consumer version selectors are not specified'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(PactBrokerAnnotationWithTags.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
          assert url.host == 'pactbroker.host'
          assert url.port == 1000
          brokerClient
        }
      }
    }
    def selectors = [
      new ConsumerVersionSelector('master', true, null, null)
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result == []
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok([])
  }

  @Issue('#1208')
  @SuppressWarnings('GStringExpressionWithinString')
  def 'When falling back to tags when consumer version selectors are not specified, use the supplied value resolver'() {
    given:
    valueResolver = Mock(ValueResolver)
    valueResolver.propertyDefined(_) >> false
    def selectors = [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('two', true, '', null),
      new ConsumerVersionSelector('three', true, '', null)
    ]
    def expected = [
      new PactBrokerResult('d', '', '', [], [], false, 'one', false, false)
    ]
    consumerVersionSelectors = [
      createVersionSelector(tag: '${pactbroker.consumerversionselectors.tags}', latest: 'true')
    ]

    when:
    pactBrokerLoader().load('test')

    then:
    valueResolver.propertyDefined('pactbroker.consumerversionselectors.tags') >> true
    valueResolver.resolveValue('pactbroker.consumerversionselectors.tags') >> 'one,two,three'
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
  }

  def 'Loads pacts with consumer version selectors when consumer version selectors and tags are both present'() {
    given:
    tags = ['master', 'prod']
    consumerVersionSelectors = [createVersionSelector(tag: 'demo', latest: 'true')]
    def expected = [
      new PactBrokerResult('a', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('b', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('c', '', '', [], [], false, 'demo', false, false),
      new PactBrokerResult('d', '', '', [], [], false, 'demo', false, false)
    ]
    def selectors = [ new ConsumerVersionSelector('demo', true, '', null) ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 4
  }

  def 'Loads pacts with no selectors when none are specified'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(FullPactBrokerAnnotation.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
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
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> new Ok([])
  }

  def 'Does not loads wip pacts when pending is false'() {
    given:
    consumerVersionSelectors = [
      createVersionSelector(tag: 'a', latest: 'true'),
      createVersionSelector(tag: 'b', latest: 'false'),
    ]
    includeWipPactsSince = '2020-06-25'
    def selectors = [
      new ConsumerVersionSelector('a', true, '', null),
      new ConsumerVersionSelector('b', false, '', null),
    ]
    def expected = [
      new PactBrokerResult('test', 'a', '', [], [], false, null, false, false),
      new PactBrokerResult('test', 'b', '', [], [], false, null, false, false),
      new PactBrokerResult('test', 'c', '', [], [], false, null, false, false),
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, [], false, '') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  def 'Loads wip pacts when pending and includeWipPactsSince parameters set'() {
    given:
    consumerVersionSelectors = [
      createVersionSelector(tag: 'a', latest: 'true'),
      createVersionSelector(tag: 'b', latest: 'false'),
    ]
    enablePendingPacts = 'true'
    providerTags = ['dev']
    includeWipPactsSince = '2020-06-25'
    def selectors = [
      new ConsumerVersionSelector('a', true, '', null),
      new ConsumerVersionSelector('b', false, '', null),
    ]
    def expected = [
      new PactBrokerResult('test', 'a', '', [], [], false, null, false, false),
      new PactBrokerResult('test', 'b1', '', [], [], true, null, false, false),
      new PactBrokerResult('test', 'b2', '', [], [], true, null, true, false),
    ]

    when:
    def result = pactBrokerLoader().load('test')

    then:
    brokerClient.getOptions() >> [:]
    1 * brokerClient.fetchConsumersWithSelectors('test', selectors, ['dev'], true, '2020-06-25') >> new Ok(expected)
    0 * brokerClient._
    result.size() == 3
  }

  def 'use the overridden pact URL'() {
    given:
    consumers = ['a', 'b', 'c']
    tags = ['demo']
    PactBrokerLoader loader = Spy(pactBrokerLoader())
    loader.overridePactUrl('http://overridden.com', 'overridden')

    when:
    def result = loader.load('test')

    then:
    brokerClient.getOptions() >> [:]
    0 * brokerClient._
    result.size() == 1
  }

  def 'does not fail if the port is not provided'() {
    when:
    port = null
    pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> new Ok([])
    noExceptionThrown()
  }

  def 'configured from annotation with no port'() {
    given:
    pactBrokerLoader = {
      def loader = new PactBrokerLoader(PactBrokerAnnotationNoPort.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
          assert url.host == 'pactbroker.host'
          assert url.port == -1
          brokerClient
        }
      }
      loader.pactReader = mockReader
      loader
    }

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result == []
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> new Ok([])
  }

  def 'configured from annotation with https and no port'() {
    given:
    pactBrokerLoader = {

      def loader = new PactBrokerLoader(PactBrokerAnnotationHttpsNoPort.getAnnotation(PactBroker)) {
        @Override
        IPactBrokerClient newPactBrokerClient(URI url, ValueResolver resolver) {
          assert url.scheme == 'https'
          assert url.host == 'pactbroker.host'
          assert url.port == -1
          brokerClient
        }
      }
      loader.pactReader = mockReader
      loader
    }

    when:
    def result = pactBrokerLoader().load('test')

    then:
    result == []
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> new Ok([])
  }

  def 'Auth: Uses no auth if no auth is provided'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(PactBrokerAnnotationAuthNotSet.getAnnotation(PactBroker))
    }

    when:
    def pactBrokerClient = pactBrokerLoader()
            .newPactBrokerClient(new URI('http://localhost'), new SystemPropertyResolver())

    then:
    pactBrokerClient.options == [:]
  }

  def 'Auth: Uses basic auth if username and password are provided'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(PactBrokerAnnotationWithUsernameAndPassword.getAnnotation(PactBroker))
    }

    when:
    def pactBrokerClient = pactBrokerLoader()
            .newPactBrokerClient(new URI('http://localhost'), new SystemPropertyResolver())

    then:
    pactBrokerClient.options == ['authentication': ['basic', 'user', 'pw']]
  }

  def 'Auth: Uses basic auth if username and token are provided'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(PactBrokerAnnotationAuthWithUsernameAndToken.getAnnotation(PactBroker))
    }

    when:
    def pactBrokerClient = pactBrokerLoader()
            .newPactBrokerClient(new URI('http://localhost'), new SystemPropertyResolver())

    then:
    pactBrokerClient.options == ['authentication': ['basic', 'user', '']]
  }

  def 'Auth: Uses bearer auth if token is provided'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(PactBrokerAnnotationWithOnlyToken.getAnnotation(PactBroker))
    }

    when:
    def pactBrokerClient = pactBrokerLoader()
            .newPactBrokerClient(new URI('http://localhost'), new SystemPropertyResolver())

    then:
    pactBrokerClient.options == ['authentication': ['bearer', 'token-value']]
  }

  def 'Auth: Uses bearer auth if token and password are provided'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(PactBrokerAnnotationWithPasswordAndToken.getAnnotation(PactBroker))
    }

    when:
    def pactBrokerClient = pactBrokerLoader()
            .newPactBrokerClient(new URI('http://localhost'), new SystemPropertyResolver())

    then:
    pactBrokerClient.options == ['authentication': ['bearer', 'token-value']]
  }

  def 'Auth: No auth if neither token nor username is provided'() {
    given:
    pactBrokerLoader = {
      new PactBrokerLoader(PactBrokerAnnotationEmptyAuth.getAnnotation(PactBroker))
    }

    when:
    def pactBrokerClient = pactBrokerLoader().newPactBrokerClient(new URI('http://localhost'), new SystemPropertyResolver())

    then:
    pactBrokerClient.options == [:]
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'shouldFallBackToTags is #result when #desc'() {
    given:
    valueResolver = Mock(ValueResolver) {
      it.propertyDefined(_) >> { it[0] == 'tag' || it[0] == 'tag2' }
      it.resolveValue(_) >> {
        if (it[0] == 'tag') {
          ''
        } else if (it[0] == 'tag2') {
          'value'
        } else {
          it[0]
        }
      }
    }

    expect:
    pactBrokerLoader.call().shouldFallBackToTags(['one'], values, valueResolver) == result

    where:

    desc                                                              | values                                                                 | result
    'selectors is empty'                                              | []                                                                     | true
    'selectors has one empty value'                                   | [createVersionSelector()]                                              | true
    'selectors has one item that resolves to an empty string'         | [createVersionSelector(tag: '${tag}')]                                 | true
    'selectors has more than one item'                                | [createVersionSelector(tag: 'one'), createVersionSelector(tag: 'two')] | false
    'selectors has one item that does not resolve to an empty string' | [createVersionSelector(tag: '${tag2}')]                                | false
  }

  def 'do not fall back to tags if there is a selector but not any tags'() {
    given:
    valueResolver = Mock(ValueResolver)
    consumerVersionSelectors = [createVersionSelector(consumer: 'bob')]

    expect:
    !pactBrokerLoader.call().shouldFallBackToTags([], consumerVersionSelectors, valueResolver)
  }

  def 'when building the list of selectors, if falling back to tags create a selector for each tag'() {
    given:
    valueResolver = Mock(ValueResolver) {
      it.propertyDefined(_) >> { it[0] == 'two' }
      it.resolveValue(_) >> {
        if (it[0] == 'two') {
          '2,3'
        } else {
          null
        }
      }
    }
    consumerVersionSelectors = []
    tags = ['one', '${two}', 'three']

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector('one', true, null, null),
      new ConsumerVersionSelector('2', true, null, null),
      new ConsumerVersionSelector('3', true, null, null),
      new ConsumerVersionSelector('three', true, null, null)
    ]
  }

  def 'when building the list of selectors, if falling back to tags create a selector with any consumers'() {
    given:
    valueResolver = Mock(ValueResolver)
    consumerVersionSelectors = []
    tags = ['one', 'two', 'three']
    consumers = ['bob', 'fred']

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector('one', true, 'bob', null),
      new ConsumerVersionSelector('one', true, 'fred', null),
      new ConsumerVersionSelector('two', true, 'bob', null),
      new ConsumerVersionSelector('two', true, 'fred', null),
      new ConsumerVersionSelector('three', true, 'bob', null),
      new ConsumerVersionSelector('three', true, 'fred', null)
    ]
  }

  def 'building the list of selectors'() {
    given:
    valueResolver = Mock(ValueResolver) {
      it.propertyDefined(_) >> { it[0] == 'two' }
      it.resolveValue(_) >> {
        if (it[0] == 'two') {
          '2,3'
        } else {
          null
        }
      }
    }
    consumerVersionSelectors = [
      createVersionSelector(tag: 'one'),
      createVersionSelector(tag: 'two'),
      createVersionSelector(tag: 'three', fallbackTag: 'four')
    ]

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('two', true, '', null),
      new ConsumerVersionSelector('three', true, '', 'four')
    ]
  }

  def 'building the list of selectors expands any expressions'() {
    given:
    valueResolver = Mock(ValueResolver) {
      it.propertyDefined(_) >> { it[0] == 'two' || it[0] == 'X' }
      it.resolveValue(_) >> {
        if (it[0] == 'two') {
          '2,3'
        } else if (it[0] == 'X') {
          'Y'
        } else {
          null
        }
      }
    }
    consumerVersionSelectors = [
      createVersionSelector(tag: 'one'),
      createVersionSelector(tag: '${two}'),
      createVersionSelector(tag: 'three', fallbackTag: '${X}')
    ]

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('2', true, '', null),
      new ConsumerVersionSelector('3', true, '', null),
      new ConsumerVersionSelector('three', true, '', 'Y')
    ]
  }

  def 'building the list of selectors expands any expressions for latest as well'() {
    given:
    valueResolver = Mock(ValueResolver) {
      it.propertyDefined(_) >> { it[0] == 'two' || it[0] == 'two_latest' }
      it.resolveValue(_) >> {
        if (it[0] == 'two') {
          '2,3'
        } else if (it[0] == 'two_latest') {
          'true,false'
        } else {
          null
        }
      }
    }
    consumerVersionSelectors = [
      createVersionSelector(tag: 'one'),
      createVersionSelector(tag: '${two}', latest: '${two_latest}'),
      createVersionSelector(tag: 'three')
    ]

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('2', true, '', null),
      new ConsumerVersionSelector('3', false, '', null),
      new ConsumerVersionSelector('three', true, '', null)
    ]
  }

  def 'building the list of selectors when latest expands to a single value use it for all selectors'() {
    given:
    valueResolver = Mock(ValueResolver) {
      it.propertyDefined(_) >> { it[0] == 'two' || it[0] == 'two_latest' }
      it.resolveValue(_) >> {
        if (it[0] == 'two') {
          '2,3'
        } else if (it[0] == 'two_latest') {
          'false'
        } else {
          null
        }
      }
    }
    consumerVersionSelectors = [
      createVersionSelector(tag: 'one'),
      createVersionSelector(tag: '${two}', latest: '${two_latest}'),
      createVersionSelector(tag: 'three')
    ]

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('2', false, '', null),
      new ConsumerVersionSelector('3', false, '', null),
      new ConsumerVersionSelector('three', true, '', null)
    ]
  }

  def 'building the list of selectors includes any consumer name'() {
    given:
    valueResolver = Mock(ValueResolver)
    consumerVersionSelectors = [
      createVersionSelector(tag: 'one'),
      createVersionSelector(tag: 'two', latest: 'false'),
      createVersionSelector(tag: 'three', consumer: 'test')
    ]

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector('one', true, '', null),
      new ConsumerVersionSelector('two', false, '', null),
      new ConsumerVersionSelector('three', true, 'test', null)
    ]
  }

  def 'building the list of selectors supports a consumer name but no tags'() {
    given:
    valueResolver = Mock(ValueResolver)
    consumerVersionSelectors = [
      createVersionSelector(consumer: 'test')
    ]

    when:
    def result = pactBrokerLoader.call().buildConsumerVersionSelectors(valueResolver)

    then:
    result == [
      new ConsumerVersionSelector(null, true, 'test', null)
    ]
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'getPactBrokerSource uses the URL if it is set'() {
    given:
    def valueResolver = Mock(ValueResolver)

    when:
    host = p_host
    port = p_port
    protocol = p_protocol
    url = p_url
    def source = pactBrokerLoader().getPactBrokerSource(valueResolver)

    then:
    source == expected

    where:

    p_host      | p_port | p_protocol | p_url              | expected
    null        | null   | null       | 'http://localhost' | new PactBrokerSource(null, null, 'http', [:], 'http://localhost')
    'localhost' | '1234' | null       | null               | new PactBrokerSource('localhost', '1234', 'http', [:], null)
    'localhost' | '1234' | 'https'    | null               | new PactBrokerSource('localhost', '1234', 'https', [:], null)
  }

  @Unroll
  def 'brokerUrl returns the url if it is set'() {
    given:
    def valueResolver = Mock(ValueResolver)

    when:
    host = p_host
    port = p_port
    protocol = p_protocol
    url = p_url
    def result = pactBrokerLoader().brokerUrl(valueResolver).toString()

    then:
    result == expected

    where:

    p_host      | p_port | p_protocol | p_url               | expected
    null        | null   | null       | 'http://localhost/' | 'http://localhost/'
    'localhost' | '1234' | null       | null                | 'http://localhost:1234'
    'localhost' | '1234' | 'https'    | null                | 'https://localhost:1234'
  }

  @Issue('#1322')
  def 'Throws an Exception if there is a certificate error'() {
    when:
    pactBrokerLoader().load('test')

    then:
    1 * brokerClient.fetchConsumersWithSelectors('test', [], [], false, '') >> {
      throw new InvalidNavigationRequest('PKIX path building failed', new SSLHandshakeException('PKIX path building failed'))
    }
    thrown(InvalidNavigationRequest)
  }

  private static VersionSelector createVersionSelector(Map args = [:]) {
    new VersionSelector() {
      @Override
      String tag() {
        args.tag ?: ''
      }

      @Override
      String latest() {
        args.latest ?: true
      }

      @Override
      String consumer() {
        args.consumer ?: ''
      }

      @Override
      String fallbackTag() {
        args.fallbackTag
      }

      @Override
      Class<? extends Annotation> annotationType() {
        VersionSelector
      }
    }
  }

  @PactBroker(host = 'pactbroker.host', port = '1000')
  static class FullPactBrokerAnnotation {

  }

  @PactBroker
  static class MinimalPactBrokerAnnotation {

  }

  @PactBroker(host = 'pactbroker.host')
  static class PactBrokerAnnotationNoPort {

  }

  @PactBroker(host = 'pactbroker.host', scheme = 'https')
  static class PactBrokerAnnotationHttpsNoPort {

  }

  @PactBroker(host = 'pactbroker.host')
  static class PactBrokerAnnotationAuthNotSet {

  }

  @PactBroker(host = 'pactbroker.host',
      authentication = @PactBrokerAuth(username = 'user', password =  'pw'))
  static class PactBrokerAnnotationWithUsernameAndPassword {

  }

  @PactBroker(host = 'pactbroker.host',
      authentication = @PactBrokerAuth(username = 'user', token = 'ignored'))
  static class PactBrokerAnnotationAuthWithUsernameAndToken {

  }

  @PactBroker(host = 'pactbroker.host',
      authentication = @PactBrokerAuth(password = 'pw', token = 'token-value'))
  static class PactBrokerAnnotationWithPasswordAndToken {

  }

  @PactBroker(host = 'pactbroker.host',
      authentication = @PactBrokerAuth(token = 'token-value'))
  static class PactBrokerAnnotationWithOnlyToken {

  }

  @PactBroker(host = 'pactbroker.host',
          authentication = @PactBrokerAuth)
  static class PactBrokerAnnotationEmptyAuth {

  }

  @PactBroker(host = 'pactbroker.host', port = '1000', tags = 'master')
  static class PactBrokerAnnotationWithTags {

  }

}
