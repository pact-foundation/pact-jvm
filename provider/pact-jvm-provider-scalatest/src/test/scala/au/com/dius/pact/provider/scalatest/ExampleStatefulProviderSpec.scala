package au.com.dius.pact.provider.scalatest

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Provider will be tested against all the defined consumers in the configured default directory.
  * The provider won't be restarted during the test suite.
  * Every interactions tests a provider which was started at the very beginning of the suite.
  * State will be initialised before a new interaction is tested.
  */
@RunWith(classOf[JUnitRunner])
class ExampleStatefulProviderSpec extends PactProviderStatefulDslSpec("test_provider") {

  lazy val serverStarter: ServerStarter = new ProviderServerStarter
}
