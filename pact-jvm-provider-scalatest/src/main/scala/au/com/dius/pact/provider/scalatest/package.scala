package au.com.dius.pact.provider

import java.net.URI

package object scalatest {

  trait Consumer {
    val filter: IConsumerInfo => Boolean
  }

  /**
    * Matching consumer pacts will be allowed to run against the provider
    *
    * @param consumer
    * @return
    */
  implicit def strToConsumer(consumer: String): Consumer = new Consumer {
    override val filter: IConsumerInfo => Boolean = (consumerInfo: IConsumerInfo) => consumerInfo.getName == consumer
  }

  /**
    * @param provider which provider pact should be tested
    * @param consumer which consumer pact should be tested
    * @param uri      where is the pact
    */
  case class Pact(provider: String, consumer: Consumer, uri: URI)

  case class ServerConfig(serverStarter: ServerStarter, restartServer: Boolean = false)

  case class VerificationConfig(pact: Pact, serverConfig: ServerConfig)

}
