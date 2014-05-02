package au.com.dius.pact.consumer

import org.specs2.mutable.Specification

class PactServerConfigSpec extends Specification {
  "port server config" should {
    "select a random port" in {
      PactServerConfig.createDefault().port must beGreaterThanOrEqualTo(PactServerConfig.portLowerBound)
      PactServerConfig.createDefault().port must beLessThanOrEqualTo(PactServerConfig.portUpperBound)
    }
  }
}
