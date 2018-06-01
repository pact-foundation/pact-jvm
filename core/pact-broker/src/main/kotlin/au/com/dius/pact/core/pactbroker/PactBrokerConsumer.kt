package au.com.dius.pact.core.pactbroker

data class PactBrokerConsumer(
  val name: String,
  val source: String,
  val pactBrokerUrl: String,
  val pactFileAuthentication: List<String> = listOf()
)
