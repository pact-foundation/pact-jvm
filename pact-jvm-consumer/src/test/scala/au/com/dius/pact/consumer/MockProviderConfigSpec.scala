package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.model.MockProviderConfig
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MockProviderConfigSpec extends Specification {
  "port server config" should {
    "select a random port" in {
      MockProviderConfig.createDefault().port must beGreaterThanOrEqualTo(MockProviderConfig.portLowerBound)
      MockProviderConfig.createDefault().port must beLessThanOrEqualTo(MockProviderConfig.portUpperBound)
    }
  }
}
