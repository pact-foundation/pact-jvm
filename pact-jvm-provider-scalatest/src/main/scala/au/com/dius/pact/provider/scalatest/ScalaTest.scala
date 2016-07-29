package au.com.dius.pact.provider.scalatest

import java.net.URI

import au.com.dius.pact.provider.ConsumerInfo

trait Consumer {
  val filter: ConsumerInfo => Boolean
}

object ScalaTest {
  /**
    * Matching consumer pacts will be allowed to run against the provider
    *
    * @param consumer
    * @return
    */
  implicit def strToConsumer(consumer: String) = new Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => consumerInfo.getName == consumer
  }
}

/**
  * @param provider which provider pact should be tested
  * @param consumer which consumer pact should be tested
  * @param uri      where is the pact
  */
case class Pact(provider: String, consumer: Consumer, uri: URI)

case class ServerConfig(serverStarter: ServerStarter, restartServer: Boolean = false)

case class VerificationConfig(pact: Pact, serverConfig: ServerConfig)
