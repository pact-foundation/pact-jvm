package com.dius.pact.runner

import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}

class HttpSetupHook(providerUrl:String, http:HttpCalls)(implicit executionContext: ExecutionContext) extends SetupHook {
  def setup(setupIdentifier : String) : Future[Boolean] = {
    http.url(providerUrl)
      .post(Json.obj("state" -> setupIdentifier))
      .map { response =>
        response.status > 199 && response.status < 300
      }.recover {
        case e:Throwable => {
          e.printStackTrace()
          false
        }
      }
  }
}
