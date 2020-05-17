package au.com.dius.pact.provider

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import com.github.michaelbull.result.Ok
import spock.lang.Specification

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

  def 'returns an empty list if the directory is null'() {
    when:
    def consumers = providerInfo.hasPactsWith('testGroup') { group ->
      group.pactFileLocation = null
    }

    then:
    consumers == []
  }

  def 'raises an exception if the directory does not exist'() {
    when:
    providerInfo.hasPactsWith('testGroup') { group ->
      group.pactFileLocation = Mock(File) {
        exists() >> false
      }
    }

    then:
    thrown(RuntimeException)
  }

  def 'raises an exception if the directory is not readable'() {
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

  def 'does not include pending pacts if the option is not present'() {
    given:
    def options = [:]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectors('TestProvider', selectors, [], false) >> new Ok([
      new PactBrokerResult('consumer', '', url, [], [], false)
    ])
    result.size == 1
    result[0].name == 'consumer'
    !result[0].pending
  }

  def 'does include pending pacts if the option is present'() {
    given:
    def options = [
      enablePending: true,
      providerTags: ['master']
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectors('TestProvider', selectors, ['master'], true) >> new Ok([
      new PactBrokerResult('consumer', '', url, [], [], true)
    ])
    result.size == 1
    result[0].name == 'consumer'
    result[0].pending
  }

  def 'throws an exception if the pending pacts option is present but there is no provider tags'() {
    given:
    def options = [
      enablePending: true
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelector('test', true)
    ]

    when:
    providerInfo.hasPactsFromPactBrokerWithSelectors(options, url, selectors)

    then:
    def exception = thrown(RuntimeException)
    exception.message == 'No providerTags: To use the pending pacts feature, you need to provide the list of ' +
      'provider names for the provider application version that will be published with the verification results'
  }
}
