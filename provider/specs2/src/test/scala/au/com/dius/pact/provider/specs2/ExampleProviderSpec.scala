package au.com.dius.pact.provider.specs2

import org.junit.Ignore
import org.specs2.execute.Result
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

//@RunWith(classOf[JUnitRunner])
@Ignore
class ExampleProviderSpec extends ProviderSpec {
  def honoursPact = getClass.getClassLoader.getResourceAsStream("exampleSpec.json")

  def inState(state: String, test: (String) => Result): Result = {
    TestServer(state).run(test)
  }
}
