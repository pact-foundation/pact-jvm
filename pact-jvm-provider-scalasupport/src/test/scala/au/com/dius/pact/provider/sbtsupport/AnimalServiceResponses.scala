package au.com.dius.pact.provider.sbtsupport

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.model.Response

import scala.collection.JavaConversions

object AnimalServiceResponses {
  def contentHeaders: Map[String, String] = Map("Content-Type" -> "application/json; charset=UTF-8")

  def alligator(name:String): Response = {
    val json = OptionalBody.body("{\"alligators\": [{\"name\": \"" + name + "\"}")
    new Response(200, JavaConversions.mapAsJavaMap(contentHeaders), json)
  }
  val bobResponse = alligator("Bob")
  val maryResponse = alligator("Mary")

  val errorJson = OptionalBody.body("{\"error\": \"Argh!!!\"}")

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
      "/alligators/Mary" -> new Response(404)
    ),
    "an error has occurred" -> Map (
      "/alligators" -> new Response(500, JavaConversions.mapAsJavaMap(contentHeaders), errorJson)
    )
  )

}
