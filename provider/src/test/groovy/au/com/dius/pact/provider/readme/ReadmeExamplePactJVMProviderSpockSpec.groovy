package au.com.dius.pact.provider.readme

import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.readme.dropwizard.DropwizardConfiguration
import au.com.dius.pact.provider.readme.dropwizard.TestDropwizardApplication
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * This is the example from the README
 */
@SuppressWarnings('EmptyMethod')
class ReadmeExamplePactJVMProviderSpockSpec extends Specification {

  @ClassRule @Shared
  TestRule startServiceRule = new DropwizardAppRule<DropwizardConfiguration>(TestDropwizardApplication,
    ResourceHelpers.resourceFilePath('dropwizard/test-config.yaml'))

  @Shared
  ProviderInfo serviceProvider

  ProviderVerifier verifier

  def setupSpec() {
    serviceProvider = new ProviderInfo('Dropwizard App')
    serviceProvider.protocol = 'http'
    serviceProvider.host = 'localhost'
    serviceProvider.port = 8080
    serviceProvider.path = '/'

    serviceProvider.hasPactWith('zoo_app') { consumer ->
      consumer.pactSource = new FileSource(new File(ResourceHelpers.resourceFilePath(
        'pacts/zoo_app-animal_service.json')))
    }
  }

  def setup() {
    verifier = new ProviderVerifier()
  }

  def cleanup() {
    // cleanup provider state
    // ie. db.truncateAllTables()
  }

  def cleanupSpec() {
    // cleanup provider
  }

  @Unroll
  def "Provider Pact - With Consumer #consumer"() {
    expect:
    !verifyConsumerPact(consumer).empty

    where:
    consumer << serviceProvider.consumers
  }

  private Map verifyConsumerPact(ConsumerInfo consumer) {
    Map failures = [:]

    verifier.initialiseReporters(serviceProvider)
    verifier.runVerificationForConsumer(failures, serviceProvider, consumer)

    if (!failures.empty) {
      verifier.displayFailures(failures)
    }

    failures
  }
}
