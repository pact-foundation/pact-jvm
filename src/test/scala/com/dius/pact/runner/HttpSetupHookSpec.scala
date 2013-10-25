package com.dius.pact.runner

import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global
import util.MockHttp

class HttpSetupHookSpec extends Specification with MockHttp {
  "HttpSetupHook" should {
    "invoke remote setup" in {
      val setupString = "There are alligators"
      val providerUrl = "provider.url"
      val mockHttp = mock[HttpCalls]

      //TODO: assert that mockHttp is called with the body: Json.obj("state"-> setupString)
      stubWs(mockHttp, providerUrl, post)(200)

      val setupHook = new HttpSetupHook(providerUrl, mockHttp)

      setupHook.setup(setupString) must beTrue
    }
  }
}
