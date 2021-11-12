package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.pactbroker.TestResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import spock.lang.Specification

class DefaultVerificationReporterSpec extends Specification {

  def 'for Pact broker sources, publish the test results and return the result'() {
    given:
    def links = ['publish': 'true']
    def interaction = new RequestResponseInteraction('interaction1')
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
      interaction
    ], [:], new BrokerUrlSource('', '', links))
    def testResult = new TestResult.Ok()
    def brokerClient = Mock(PactBrokerClient)

    when:
    def result = DefaultVerificationReporter.INSTANCE.reportResults(pact, testResult, '0', brokerClient, [], null)

    then:
    1 * brokerClient.publishVerificationResults(links, testResult, '0') >> new Ok(true)
    result == new Ok(true)
  }

  def 'for non-Pact broker sources, do not publish anything and return Ok'() {
    given:
    def interaction = new RequestResponseInteraction('interaction1')
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
      interaction
    ], [:], UnknownPactSource.INSTANCE)
    def testResult = new TestResult.Ok()
    def brokerClient = Mock(PactBrokerClient)

    when:
    def result = DefaultVerificationReporter.INSTANCE.reportResults(pact, testResult, '', brokerClient, [], null)

    then:
    0 * brokerClient.publishVerificationResults(_, new TestResult.Ok(), '0')
    result == new Ok(false)
  }

  def 'return an error if publishing the test results fails'() {
    given:
    def interaction = new RequestResponseInteraction('interaction1')
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
      interaction
    ], [:], new BrokerUrlSource('', ''))
    def testResult = new TestResult.Ok()
    def brokerClient = Mock(PactBrokerClient)

    when:
    def result = DefaultVerificationReporter.INSTANCE.reportResults(pact, testResult, '', brokerClient, [], null)

    then:
    1 * brokerClient.publishVerificationResults(_, testResult, _) >> new Err('failed')
    result == new Err(['failed'])
  }

  def 'return an error if publishing the provider tag fails'() {
    given:
    def interaction = new RequestResponseInteraction('interaction1')
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
      interaction
    ], [:], new BrokerUrlSource('', ''))
    def testResult = new TestResult.Ok()
    def brokerClient = Mock(PactBrokerClient)
    def tags = ['tag1', 'tag2', 'tag3']

    when:
    def result = DefaultVerificationReporter.INSTANCE.reportResults(pact, testResult, '', brokerClient, tags, null)

    then:
    0 * brokerClient.publishProviderBranch(_, 'provider', _, '')
    1 * brokerClient.publishProviderTags(_, 'provider', tags, '') >> new Err(['failed'])
    1 * brokerClient.publishVerificationResults(_, testResult, _) >> new Ok(true)
    result == new Err(['failed'])
  }

  def 'return an error if publishing the provider branch fails'() {
    given:
    def interaction = new RequestResponseInteraction('interaction1')
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
            interaction
    ], [:], new BrokerUrlSource('', ''))
    def testResult = new TestResult.Ok()
    def brokerClient = Mock(IPactBrokerClient)
    def branch = "main"

    when:
    def result = DefaultVerificationReporter.INSTANCE.reportResults(pact, testResult, '', brokerClient, [], branch)

    then:
    0 * brokerClient.publishProviderTags(_, 'provider', _, '')
    1 * brokerClient.publishProviderBranch(_, 'provider', branch, '') >> new Err('failed')
    1 * brokerClient.publishVerificationResults(_, testResult, _) >> new Ok(true)
    result == new Err(['failed'])
  }

  def 'return list of errors if publishing the provider tags and branch fails'() {
    given:
    def interaction = new RequestResponseInteraction('interaction1')
    def pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
            interaction
    ], [:], new BrokerUrlSource('', ''))
    def testResult = new TestResult.Ok()
    def brokerClient = Mock(IPactBrokerClient)
    def tags = ['tag1', 'tag2', 'tag3']
    def branch = "main"

    when:
    def result = DefaultVerificationReporter.INSTANCE.reportResults(pact, testResult, '', brokerClient, tags, branch)

    then:
    1 * brokerClient.publishProviderTags(_, 'provider', tags, '') >> new Err(['tags failed'])
    1 * brokerClient.publishProviderBranch(_, 'provider', branch, '') >> new Err('branch failed')
    1 * brokerClient.publishVerificationResults(_, testResult, _) >> new Ok(true)
    result == new Err(['tags failed', 'branch failed'])
  }
}
