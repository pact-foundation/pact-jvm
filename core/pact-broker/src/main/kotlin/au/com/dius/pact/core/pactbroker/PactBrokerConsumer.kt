package au.com.dius.pact.core.pactbroker

data class PactBrokerConsumer @JvmOverloads constructor (
  val name: String,
  val source: String,
  val pactBrokerUrl: String,
  val pactFileAuthentication: List<String> = listOf(),
  val tag: String? = null
)
