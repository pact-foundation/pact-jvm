package au.com.dius.pact.provider.readme

import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.HttpClientFactory
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.readme.dropwizard.DropwizardConfiguration
import au.com.dius.pact.provider.readme.dropwizard.TestDropwizardApplication
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Test
import org.junit.rules.TestRule

import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat

/**
 * This is the example from the README
 */
@SuppressWarnings(['ExplicitHashMapInstantiation', 'FieldName', 'JUnitPublicField', 'UnnecessaryGetter',
  'UnnecessaryReturnKeyword'])
// Test is failing on Windows
@Ignore('Test is failing on Windows')
class ReadmeExamplePactJVMProviderJUnitTest {

  @ClassRule
  public static final TestRule startServiceRule = new DropwizardAppRule<DropwizardConfiguration>(
    TestDropwizardApplication, ResourceHelpers.resourceFilePath('dropwizard/test-config.yaml'))

  private static ProviderInfo serviceProvider
  private static Pact<RequestResponseInteraction> testConsumerPact
  private static ConsumerInfo consumer

  @BeforeClass
  static void setupProvider() {
    serviceProvider = new ProviderInfo('Dropwizard App')
    serviceProvider.setProtocol('http')
    serviceProvider.setHost('localhost')
    serviceProvider.setPort(8080)
    serviceProvider.setPath('/')

    consumer = new ConsumerInfo()
    consumer.setName('test_consumer')
    consumer.setPactSource(new UrlSource(
      ReadmeExamplePactJVMProviderJUnitTest.getResource('/pacts/zoo_app-animal_service.json').toString()))

    testConsumerPact = DefaultPactReader.INSTANCE.loadPact(consumer.getPactSource()) as Pact<RequestResponseInteraction>
  }

  @Test
  void runConsumerPacts() {
    // grab the first interaction from the pact with consumer
    Interaction interaction = testConsumerPact.interactions.get(0)

    // setup the verifier
    ProviderVerifier verifier = setupVerifier(interaction, serviceProvider, consumer)

    // setup any provider state

    // setup the client and interaction to fire against the provider
    ProviderClient client = new ProviderClient(serviceProvider, new HttpClientFactory())
    def result = verifier.verifyResponseFromProvider(serviceProvider, interaction, interaction.getDescription(),
      [:], client)

    // normally assert all good, but in this example it will fail
    assertThat(result, is(instanceOf(VerificationResult.Failed)))

    verifier.displayFailures([result])
  }

  private ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider, ConsumerInfo consumer) {
    ProviderVerifier verifier = new ProviderVerifier()

    verifier.initialiseReporters(provider)
    verifier.reportVerificationForConsumer(consumer, provider, new UrlSource('http://example.example'))

    if (!interaction.getProviderStates().isEmpty()) {
      for (ProviderState providerState: interaction.getProviderStates()) {
        verifier.reportStateForInteraction(providerState.getName(), provider, consumer, true)
      }
    }

    verifier.reportInteractionDescription(interaction)

    return verifier
  }
}
