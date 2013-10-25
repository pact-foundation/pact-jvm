package com.dius.pact.runner

import play.api.libs.json.Json
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit


class HttpSetupHook(providerUrl:String, http:HttpCalls)(implicit executionContext: ExecutionContext) extends SetupHook {
  def setup(setupIdentifier : String) : Boolean = {
    val future = http.url(providerUrl)
      .post(Json.obj("state" -> setupIdentifier))
      .map { response =>
        response.status > 199 && response.status < 300
      }.recover {
        case e:Throwable => {
          e.printStackTrace()
          false
        }
      }

    Await.result(future, Duration(1, TimeUnit.SECONDS))

  }
}
