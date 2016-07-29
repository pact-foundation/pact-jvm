package au.com.dius.pact.provider.scalatest

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExampleConfigProviderSpec extends ProviderSpec {

  import ScalaTest.strToConsumer

  /**
    * Runs 'test_provider' pact verifications from 'pacts-dependents' directory with server restart just against 'test_consumer' pacts
    */
  verify(VerificationConfig(Pact(provider = "test_provider", consumer = "test_consumer", uri = "pacts-dependents"), ServerConfig(serverStarter = new ProviderServerStarter, restartServer = true)))
}
