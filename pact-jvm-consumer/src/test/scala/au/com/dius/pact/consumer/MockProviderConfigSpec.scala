package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.model.{PactConfig, MockProviderConfig}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MockProviderConfigSpec extends Specification {
  "port server config" should {
    "select a random port" in {
      val config = PactConfig(2)
      MockProviderConfig.createDefault(config).port must beGreaterThanOrEqualTo(MockProviderConfig.portLowerBound)
      MockProviderConfig.createDefault(config).port must beLessThanOrEqualTo(MockProviderConfig.portUpperBound)
    }
  }
}
