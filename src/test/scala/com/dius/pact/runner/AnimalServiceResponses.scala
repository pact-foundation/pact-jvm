package com.dius.pact.runner

import com.dius.pact.model.Response

object AnimalServiceResponses {
  def alligator(name:String) = Response(
    200,
    None,
    Some(s"""{"alligators": [{"name":"$name"}]}""")
  )
  val bobResponse = alligator("Bob")
  val maryResponse = alligator("Mary")

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
      "/alligators/Mary" -> Response(404, None, None)
    ),
    "an error has occurred" -> Map (
      "/alligators" -> Response(500, None, Some("""{"error":"Argh!!!"}"""))
    )
  )

}
