package au.com.dius.pact.consumer

object PactConsumerConfig {
  val pactDirectory: String = System.getProperty("pact.rootDir", "target/pacts")
}
