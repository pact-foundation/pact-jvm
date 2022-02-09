package au.com.dius.pact.provider

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import au.com.dius.pact.core.support.Auth
import com.github.michaelbull.result.Ok
import spock.lang.Issue
import spock.lang.Specification

@SuppressWarnings(['LineLength', 'ClosureAsLastMethodParameter'])
class ProviderInfoSpec extends Specification {

  private ProviderInfo providerInfo
  private File mockPactDir
  private fileList
  private PactBrokerClient pactBrokerClient

  def setup() {
    fileList = []
    mockPactDir = Mock(File) {
      exists() >> true
      canRead() >> true
      isDirectory() >> true
      listFiles() >> { fileList as File[] }
    }
    pactBrokerClient = Mock()
    providerInfo = Spy(new ProviderInfo('TestProvider'))
    providerInfo.pactBrokerClient(_, _) >> pactBrokerClient
  }

  def 'hasPactsWith - returns an empty list if the directory is null'() {
    when:
    def consumers = providerInfo.hasPactsWith('testGroup') { group ->
      group.pactFileLocation = null
    }

    then:
    consumers == []
  }

  def 'hasPactsWith - raises an exception if the directory does not exist'() {
    when:
    providerInfo.hasPactsWith('testGroup') { group ->
      group.pactFileLocation = Mock(File) {
        exists() >> false
      }
    }

    then:
    thrown(RuntimeException)
  }

  def 'hasPactsWith - raises an exception if the directory is not readable'() {
    when:
    providerInfo.hasPactsWith('testGroup') { group ->
      group.pactFileLocation = Mock(File) {
        exists() >> true
        canRead() >> false
      }
    }

    then:
    thrown(RuntimeException)
  }

  def 'hasPactsFromPactBrokerWithSelectors - does not include pending pacts if the option is not present'() {
    given:
    def options = [:]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectors('TestProvider', selectors, [], false, '') >> new Ok([
      new PactBrokerResult('consumer', '', url, [], [], false, null, false, false)
    ])
    result.size == 1
    result[0].name == 'consumer'
    !result[0].pending
  }

  def 'hasPactsFromPactBrokerWithSelectors - does include pending pacts if the option is present'() {
    given:
    def options = [
      enablePending: true,
      providerTags: ['master']
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectors('TestProvider', selectors, ['master'], true, '') >> new Ok([
      new PactBrokerResult('consumer', '', url, [], [], true, null, false, false)
    ])
    result.size == 1
    result[0].name == 'consumer'
    result[0].pending
  }

  def 'hasPactsFromPactBrokerWithSelectors - throws an exception if the pending pacts option is present but there is no provider tags'() {
    given:
    def options = [
      enablePending: true
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true, null, null)
    ]

    when:
    providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    def exception = thrown(RuntimeException)
    exception.message == 'No providerTags: To use the pending pacts feature, you need to provide the list of ' +
      'provider names for the provider application version that will be published with the verification results'
  }

  def 'hasPactsFromPactBrokerWithSelectors - does not include wip pacts if the option is not present'() {
    given:
    def options = [
      enablePending: true,
      providerTags: ['master']
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectors('TestProvider', selectors, ['master'], true, '') >> new Ok([
      new PactBrokerResult('consumer', '', url, [], [], false, null, false, false)
    ])
    result.size == 1
    result[0].name == 'consumer'
    !result[0].pending
  }

  def 'hasPactsFromPactBrokerWithSelectors - does include wip pacts if the option is present'() {
    given:
    def options = [
      enablePending: true,
      providerTags: ['master'],
      includeWipPactsSince: '2020-05-23'
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectors('TestProvider', selectors, ['master'], true, '2020-05-23') >> new Ok([
      new PactBrokerResult('consumer', '', url, [], [], true, null, true, false)
    ])
    result.size == 1
    result[0].name == 'consumer'
    result[0].pending
  }

  @Issue('#1483')
  def 'hasPactsFromPactBrokerWithSelectors - configures the authentication correctly'() {
    given:
    def options = [
      authentication: ['bearer', '123ABC']
    ]
    def url = 'http://localhost:8080'
    def selectors = []

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    providerInfo.pactBrokerClient(_, { it.auth == new Auth.BearerAuthentication('123ABC') }) >> pactBrokerClient
    pactBrokerClient.fetchConsumersWithSelectors('TestProvider', selectors, [], false, '') >> new Ok([
      new PactBrokerResult('consumer', '', url, [], [], true, null, true, false)
    ])
    result.size == 1
    result[0].name == 'consumer'
    result[0].pending
  }
}
