package au.com.dius.pact.consumer

object PactConsumerConfig {

  val config = scala.collection.mutable.Map("pactRootDir" -> "target/pacts")

  def pactRootDir = System.getProperty("pact.rootDir", config("pactRootDir"))
}
