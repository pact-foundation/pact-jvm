package au.com.dius.pact.pactbroker

data class PactBrokerConsumer(val name: String,
                              val source: String,
                              val pactBrokerUrl: String,
                              val pactFileAuthentication: List<String> = listOf())
