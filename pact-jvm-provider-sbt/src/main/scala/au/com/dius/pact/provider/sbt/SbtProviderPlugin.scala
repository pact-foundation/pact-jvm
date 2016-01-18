package au.com.dius.pact.provider.sbt

import java.util

import au.com.dius.pact.provider.{ProviderInfo, ConsumerInfo, PactVerification, ProviderVerifier}
import sbt._

import scala.collection.{GenTraversableOnce, JavaConversions}
import scala.collection.mutable.ArrayBuffer

object SbtProviderPlugin extends Plugin {

  val providers = SettingKey[Seq[ProviderConfig]]("providers", "Providers to verify")
  val pactVerify = taskKey[Unit]("Verify the pacts for all defined providers")

  val config = Seq(
    providers := Seq(),
    pactVerify := {
      if (providers.value.isEmpty)
        sys.error("No providers have been defined. Configure them by setting the providers build setting.")
      else
        Verification.verify(providers.value)
    })
}

case class ProviderConfig(protocol: String = "http",
                          host: String = "localhost",
                          port: Integer = 8080,
                          path: String = "/",
                          name: String = "provider",
                          insecure: Boolean = false,
                          trustStore: Option[File] = None,
                          trustStorePassword: String = "changeme",
                          stateChangeUrl: Option[URL] = None,
                          stateChangeUsesBody: Boolean = false,
                          verificationType: PactVerification = PactVerification.REQUST_RESPONSE,
                          packagesToScan: List[String] = List(),
                          consumers: ArrayBuffer[ConsumerConfig] = ArrayBuffer(),
                          pactFileDirectory: Option[File] = None,
                          pactBrokerUrl: Option[URL] = None

//def startProviderTask
//def terminateProviderTask

//def requestFilter
//def stateChangeRequestFilter
//def createClient
                         ) {

  def hasPactWith(consumer: ConsumerConfig) = {
    consumers += consumer
    this
  }

}

case class ConsumerConfig(name: String,
                          pactFile: File,
                          stateChange: Option[URL] = None,
                          stateChangeUsesBody: Boolean = false,
                          verificationType: PactVerification = PactVerification.REQUST_RESPONSE,
                          packagesToScan: List[String] = List()
//                          pactFileAuthentication: List[AnyRef] = List()
                         )

object Verification {

  implicit def providerConfigToProviderInfo(provider: ProviderConfig) : ProviderInfo = {
    val provInfo = new ProviderInfo(provider.name)

    provInfo.setProtocol(provider.protocol)
    provInfo.setHost(provider.host)
    provInfo.setPort(provider.port)
    provInfo.setPath(provider.path)

//    def startProviderTask
//    def terminateProviderTask
//
//    def requestFilter
//    def stateChangeRequestFilter
//    def createClient

    provInfo.setInsecure(provider.insecure)
    if (provider.trustStore.isDefined)
      provInfo.setTrustStore(provider.trustStore.get)
    provInfo.setTrustStorePassword(provider.trustStorePassword)
    if (provider.stateChangeUrl.isDefined)
      provInfo.setStateChangeUrl(provider.stateChangeUrl.get)
    provInfo.setStateChangeUsesBody(provider.stateChangeUsesBody)
    provInfo.setVerificationType(provider.verificationType)
    provInfo.setPackagesToScan(JavaConversions.seqAsJavaList(provider.packagesToScan))
    provInfo.setConsumers(JavaConversions.seqAsJavaList(provider.consumers.toSeq.map{
      consumer => new ConsumerInfo(consumer.name, consumer.pactFile, consumer.stateChange.orNull,
        consumer.stateChangeUsesBody, consumer.verificationType,
        JavaConversions.seqAsJavaList(consumer.packagesToScan))
    }))

    provInfo
  }

  def consumersFromDirectory(pactFileDirectory: File) : Seq[ConsumerConfig] = {
    Seq()
  }

  def consumersFromPactBroker(pactBroker: URL): Seq[ConsumerConfig] = {
    Seq()
  }

  def verify(providers: Seq[ProviderConfig]) = {
    val failures = new util.HashMap[String, AnyRef]()
    val verifier = new ProviderVerifier()
//    verifier.projectHasProperty = { this.propertyDefined(it) }
//    verifier.projectGetProperty =  { this.property(it) }
//    verifier.pactLoadFailureMessage = { consumer ->
//      "You must specify the pactfile to execute for consumer '${consumer.name}' (use <pactFile> or <pactUrl>)"
//    }
//    verifier.isBuildSpecificTask = { false }
//
//    verifier.projectClasspath = {
//      List<URL> urls = []
//      for (element in classpathElements) {
//        urls.add(new File(element).toURI().toURL())
//      }
//      urls as URL[]
//    }
//

    providers.foreach { provider =>
      var consumers = provider.consumers.toSeq

      if (provider.pactFileDirectory.isDefined)
        provider.consumers ++= consumersFromDirectory(provider.pactFileDirectory.get)
      if (provider.pactBrokerUrl.isDefined)
        provider.consumers ++= consumersFromPactBroker(provider.pactBrokerUrl.get)

      failures.putAll(verifier.verifyProvider(provider).asInstanceOf[util.HashMap[String, AnyRef]])
    }

    if (!failures.isEmpty) {
      verifier.displayFailures(failures)
      sys.error(s"There were ${failures.size()} pact failures")
    }
  }

}
