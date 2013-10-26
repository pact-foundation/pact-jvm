package com.dius.pact.runner

import org.specs2.mutable.Specification
import util.MockHttp
import com.dius.pact.model.Fixtures._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.http.{ContentTypeOf, Writeable}

class ServiceSpec extends Specification with MockHttp {

  //TODO: I can't believe how hard it is to mock out WS for a unit test, this will have to be tested with some sort of running http server
//  "service" should {
//    "invoke pact request " in {
//      val mockHttp = mock[HttpCalls]
//      val baseUrl = "http://base.url"
//      val serviceUrl = s"$baseUrl${request.path}"
//      val mockHolder = anyHeaders(stubWs(mockHttp, serviceUrl, post)(response))
//      lazy val serviceResult = new Service(baseUrl, mockHttp).invoke(request)
//
//      serviceResult must beEqualTo(response).await
//      there was one(mockHttp).url(serviceUrl)
//
//      there was one (mockHolder).post(request.body.get)(any[Writeable[Any]], any[ContentTypeOf[Any]])
//      there was one (mockHolder).withHeaders(requestHeaders.toList :_*)
//    }
//  }

}
