package au.com.dius.pact.provider

import au.com.dius.pact.model.Response
import org.json4s.JsonDSL._
import org.json4s.JValue

object AnimalServiceResponses {
  def contentHeaders: Map[String, String] = Map()//"Content-Type" -> "application/json; charset=UTF-8")

  def alligator(name:String): Response = {
    val json: JValue = "alligators" -> List("name" -> s"$name")
    Response(200, contentHeaders, Some(json), null)
  }
  val bobResponse = alligator("Bob")
  val maryResponse = alligator("Mary")

  val errorJson: JValue = "error" -> "Argh!!!"

  val responses = Map(
    "there are alligators" -> Map (
      "/animal" -> bobResponse,
      "/animals" -> bobResponse,
      "/alligators" -> bobResponse
    ),
    "there is an alligator named Mary" -> Map (
      "/alligators/Mary" -> maryResponse
    ),
    "there is not an alligator named Mary" -> Map (
      "/alligators/Mary" -> Response(404, None, None, None)
    ),
    "an error has occurred" -> Map (
      "/alligators" -> Response(500, contentHeaders, Some(errorJson), null)
    )
  )

}
