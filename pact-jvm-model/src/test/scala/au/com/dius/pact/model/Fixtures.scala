package au.com.dius.pact.model

object Fixtures {
  import HttpMethod._
  import org.json4s.JsonDSL._

  val provider = Provider("test_provider")
  val consumer = Consumer("test_consumer")


  val request = Request(Get, "/",
    Map("testreqheader" -> "testreqheadervalue"),
    "test" -> true, null)

  val response = Response(200,
    Map("testreqheader" -> "testreqheaderval"),
    "responsetest" -> true, null)

  val interaction = Interaction(
    description = "test interaction",
    providerState = Some("test state"),
    request = request,
    response = response
  )

  val interactions = List(interaction)

  val pact: Pact = Pact(
    provider = provider,
    consumer = consumer,
    interactions = interactions
  )
}
