package au.com.dius.pact.provider.scalatest

import java.net.URI

import au.com.dius.pact.provider.ConsumerInfo

/**
  * DSL extension on top of the default verify method
  */
trait ProviderDsl {

  case class Provider(provider: String) {
    def complying(consumer: Consumer): PactBetween = PactBetween(provider, consumer)
  }

  case class PactBetween(provider: String, consumer: Consumer) {
    def pacts(from: from): LocationHandler = LocationHandler(this, from.uri)
  }

  case class LocationHandler(pactBetween: PactBetween, uri: URI) {
    def testing(serverStarter: ServerStarter): ServerHandler = ServerHandler(pactBetween, uri, serverStarter)
  }

  case class ServerHandler(pactBetween: PactBetween, uri: URI, serverStarter: ServerStarter) {
    def withoutRestart() = VerificationConfig(Pact(pactBetween.provider, pactBetween.consumer, uri), ServerConfig(serverStarter))

    def withRestart() = VerificationConfig(Pact(pactBetween.provider, pactBetween.consumer, uri), ServerConfig(serverStarter, true))
  }

  /**
    * Support string provider in the DSL
    *
    * @param provider
    * @return
    */
  implicit def strToProvider(provider: String) = Provider(provider)

  /**
    * Allows every pacts to be run against the provider
    */
  case object all extends Consumer {
    override val filter = (consumerInfo: ConsumerInfo) => true
  }

  /**
    * Defines the resource uri where the pacts can be found
    *
    * @param uri
    */
  case class from(uri: URI)

  implicit def defaultPactDirectoryToUri(default: defaultPactDirectory.type): URI = stringToUri(default.directory)

  implicit def stringToUri(default: String): URI = this.getClass.getClassLoader.getResource(default).toURI

  /**
    * pacts-dependents is the default directory for pacts
    */
  object defaultPactDirectory {
    val directory = "pacts-dependents"
  }

}

object ProviderDsl extends ProviderDsl
