package au.com.dius.pact.consumer

import org.specs2.mutable.Specification

class PactServerConfigSpec extends Specification {
  "port server config" should {
    "select a random port" in {
      MockProviderConfig().port must beGreaterThanOrEqualTo(MockProviderConfig.portLowerBound)
      MockProviderConfig().port must beLessThanOrEqualTo(MockProviderConfig.portUpperBound)
    }
  }
}
