package au.com.dius.pact.provider.scalatest

import org.scalatest.Ignore
import org.junit.runner.RunWith
import scala.language.postfixOps
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
@Ignore
class ExampleDslProviderSpec extends ProviderSpec {

  /**
    * Runs 'test_provider' pact verifications from default 'pacts-dependents' directory with server restart just against 'test_consumer2' pacts
    */
  verify("test_provider" complying "test_consumer2" pacts from(defaultPactDirectory) testing (new ProviderServerStarter) withRestart)
}
