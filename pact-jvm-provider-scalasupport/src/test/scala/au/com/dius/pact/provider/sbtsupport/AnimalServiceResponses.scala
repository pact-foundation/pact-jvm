package au.com.dius.pact.provider.sbtsupport

import au.com.dius.pact.model.{OptionalBody, Response}

import scala.collection.JavaConverters._

object AnimalServiceResponses {
  def contentHeaders: Map[String, java.util.List[String]] = Map("Content-Type" -> List("application/json; charset=UTF-8").asJava)

  def alligator(name:String): Response = {
    val json = OptionalBody.body(("{\"alligators\": [{\"name\": \"" + name + "\"}").getBytes)
    new Response(200, contentHeaders.asJava, json)
  }
  val bobResponse = alligator("Bob")
  val maryResponse = alligator("Mary")

  val errorJson = OptionalBody.body("{\"error\": \"Argh!!!\"}".getBytes)

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
      "/alligators" -> new Response(500, contentHeaders.asJava, errorJson)
    )
  )

}
