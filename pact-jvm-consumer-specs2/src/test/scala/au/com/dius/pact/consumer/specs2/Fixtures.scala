package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.model._
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.Consumer
import scala.collection.mutable.ArrayBuffer
import java.util
import java.util.Collections

object Fixtures {
  import au.com.dius.pact.model.HttpMethod._
  import org.json4s.JsonDSL._

  val provider = Provider("test_provider")
  val consumer = Consumer("test_consumer")

  val request = Request(Post, "/", "q=p",
    Map("testreqheader" -> "testreqheadervalue"),
    "test" -> true, null)

  val response = Response(200,
    Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*"),
    "responsetest" -> true, null)

  val interaction = Interaction(
    description = "test interaction",
    providerState = Some("test state"),
    request = request,
    response = response
  )

  val interactions = ArrayBuffer(interaction)

  val pact: Pact = Pact(
    provider = provider,
    consumer = consumer,
    interactions = interactions
  )
}