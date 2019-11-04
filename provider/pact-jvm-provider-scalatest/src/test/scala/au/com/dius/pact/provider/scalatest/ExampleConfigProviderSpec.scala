package au.com.dius.pact.provider.scalatest

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
@Ignore
class ExampleConfigProviderSpec extends ProviderSpec {

  /**
    * Runs 'test_provider' pact verifications from 'pacts-dependents' directory with server restart just against 'test_consumer' pacts
    */
  verify(VerificationConfig(Pact(provider = "test_provider", consumer = "test_consumer", uri = "pacts-dependents"),
    ServerConfig(serverStarter = new ProviderServerStarter, restartServer = true)))
}
