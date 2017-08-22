package au.com.dius.pact.consumer

import au.com.dius.pact.model.{MockProviderConfig, PactSpecVersion}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MockProviderConfigSpec extends Specification {
  "port server config" should {
    "select a random port" in {
      val config = PactSpecVersion.V3
      MockProviderConfig.createDefault(config).getPort must beGreaterThanOrEqualTo(MockProviderConfig.portLowerBound)
      MockProviderConfig.createDefault(config).getPort must beLessThanOrEqualTo(MockProviderConfig.portUpperBound)
    }
  }
}
