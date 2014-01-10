package com.dius.pact.runner

import scala.concurrent.{ExecutionContext, Future}
import com.dius.pact.runner.http.Client
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

case class HttpSetupHook(providerUrl: String, http: Client)(implicit executionContext: ExecutionContext) extends SetupHook {
  def setup(setupIdentifier: String): Future[Boolean] = {
    http.invoke(providerUrl, compact(render(("state" -> setupIdentifier))))
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
