package com.dius.pact.runner

import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import com.dius.pact.runner.http.Client

case class HttpSetupHook(providerUrl:String, http:Client)(implicit executionContext: ExecutionContext) extends SetupHook {
  def setup(setupIdentifier : String) : Future[Boolean] = {
    http.invoke(providerUrl, Json.stringify(Json.obj("state" -> setupIdentifier)))
      .map { response =>
        response
      }.recover {
        case e:Throwable => {
          //TODO: handle report writing where setupHook fails
          e.printStackTrace()
          false
        }
      }
  }
}
