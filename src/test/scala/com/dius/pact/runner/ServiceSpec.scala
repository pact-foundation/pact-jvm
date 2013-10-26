package com.dius.pact.runner

import org.specs2.mutable.Specification
import util.MockHttp
import com.dius.pact.model.Fixtures._

import scala.concurrent.ExecutionContext.Implicits.global

class ServiceSpec extends Specification with MockHttp {

  "service" should {
    "invoke pact request " in {
      val mockHttp = mock[HttpCalls]
      val baseUrl = "http://base.url"
      val serviceUrl = s"$baseUrl${request.path}"

      val mockHolder = stubWs(mockHttp, serviceUrl, request)(response)

      lazy val serviceResult = new Service(baseUrl, mockHttp).invoke(request)

      serviceResult must beEqualTo(response).await
      there was one(mockHttp).url(serviceUrl)

      there was one (mockHolder).withHeaders(requestHeaders.toList :_*)

      there was one (mockHttp).post(mockHolder, request.body.get)
    }
  }

}
