package au.com.dius.pact.provider

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import au.com.dius.pact.core.support.Auth
import au.com.dius.pact.core.support.Result
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
      new ConsumerVersionSelectors.Selector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectorsV2(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectorsV2('TestProvider', selectors, [], '', false, '') >> new Result.Ok([
      new PactBrokerResult('consumer', '', url, [], [], false, null, false, false, null)
    ])
    result.size() == 1
    result[0].name == 'consumer'
    !result[0].pending
  }

  def 'hasPactsFromPactBrokerWithSelectors - does include pending pacts if the option is present and tags are specified'() {
    given:
    def options = [
      enablePending: true,
      providerTags: ['master']
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelectors.Selector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectorsV2(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectorsV2('TestProvider', selectors, ['master'], '', true, '') >> new Result.Ok([
      new PactBrokerResult('consumer', '', url, [], [], true, null, false, false, null)
    ])
    result.size() == 1
    result[0].name == 'consumer'
    result[0].pending
  }

  def 'hasPactsFromPactBrokerWithSelectors - does include pending pacts if the option is present and branch is specified'() {
    given:
    def options = [
            enablePending: true,
            providerBranch: 'master'
    ]
    def url = 'http://localhost:8080'
    def selectors = [
            new ConsumerVersionSelectors.Selector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectorsV2(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectorsV2('TestProvider', selectors, [], 'master', true, '') >> new Result.Ok([
            new PactBrokerResult('consumer', '', url, [], [], true, null, false, false, null)
    ])
    result.size() == 1
    result[0].name == 'consumer'
    result[0].pending
  }

  def 'hasPactsFromPactBrokerWithSelectors - throws an exception if the pending pacts option is present but there is no provider tags or provider branch'() {
    given:
    def options = [
      enablePending: true
    ]
    def url = 'http://localhost:8080'
    def selectors = [
      new ConsumerVersionSelectors.Selector('test', true, null, null)
    ]

    when:
    providerInfo.hasPactsFromPactBrokerWithSelectorsV2(options, url, selectors)

    then:
    def exception = thrown(RuntimeException)
    exception.message == 'No providerTags or providerBranch: To use the pending pacts feature, you need to provide the list of ' +
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
      new ConsumerVersionSelectors.Selector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectorsV2(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectorsV2('TestProvider', selectors, ['master'], '', true, '') >> new Result.Ok([
      new PactBrokerResult('consumer', '', url, [], [], false, null, false, false, null)
    ])
    result.size() == 1
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
      new ConsumerVersionSelectors.Selector('test', true, null, null)
    ]

    when:
    def result = providerInfo.hasPactsFromPactBrokerWithSelectorsV2(options, url, selectors)

    then:
    pactBrokerClient.fetchConsumersWithSelectorsV2('TestProvider', selectors, ['master'], '', true, '2020-05-23') >> new Result.Ok([
      new PactBrokerResult('consumer', '', url, [], [], true, null, true, false, null)
    ])
    result.size() == 1
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
    def result = providerInfo.hasPactsFromPactBrokerWithSelectorsV2(options, url, selectors)

    then:
    providerInfo.pactBrokerClient(_, { it.auth == new Auth.BearerAuthentication('Authorization', '123ABC') }) >> pactBrokerClient
    pactBrokerClient.fetchConsumersWithSelectorsV2('TestProvider', selectors, [], '', false, '') >> new Result.Ok([
      new PactBrokerResult('consumer', '', url, [], [], true, null, true, false, null)
    ])
    result.size() == 1
    result[0].name == 'consumer'
    result[0].pending
  }
}
