package com.dius.pact.runner

import org.specs2.mutable.Specification
import util.MockHttp

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import play.api.libs.ws.WS.WSRequestHolder

class HttpSetupHookSpec extends Specification with MockHttp {
  "HttpSetupHook" should {
    "invoke remote setup" in {
      val setupString = "There are alligators"
      val providerUrl = "provider.url"
      val mockHttp = mock[HttpCalls]

      val expectedBody = Json.obj("state" -> setupString)

      val request:WSRequestHolder = stubWs(mockHttp, providerUrl, expectedBody)(200)

      val setupHook = new HttpSetupHook(providerUrl, mockHttp)

//      there was one(mockHttp).post(request, expectedBody)
      setupHook.setup(setupString) must beTrue.await
    }
  }
}
