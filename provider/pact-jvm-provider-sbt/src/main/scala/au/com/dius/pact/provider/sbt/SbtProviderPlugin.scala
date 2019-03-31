package au.com.dius.pact.provider.sbt

import java.util

import au.com.dius.pact.provider.{ProviderInfo, ConsumerInfo, PactVerification, ProviderVerifier, ProviderUtils}
import org.apache.http.HttpRequest
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Logger => LogbackLogger, Level}
import sbt.Keys.TaskStreams
import sbt._

import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer

object SbtProviderPlugin extends AutoPlugin {
  object autoImport {
    lazy val pactProviders = SettingKey[Seq[ProviderConfig]]("providers", "Providers to verify")
    lazy val pactVerify = taskKey[Unit]("Verify the pacts for all defined providers")

    lazy val pactProvidersConfig = Seq(
      pactProviders := Seq(),
      pactVerify := {
        val s: TaskStreams = Keys.streams.value

        if (System.getProperty("pact.logLevel") != null) {
          LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger].setLevel(
            Level.toLevel(System.getProperty("pact.logLevel"))
          )
        }

        if (pactProviders.value.isEmpty)
          sys.error("No providers have been defined. Configure them by setting the providers build setting.")
        else
          Verification.verify(pactProviders.value)
      })
  }

  override def trigger: PluginTrigger = allRequirements
}

trait ConsumerConfigInfo
case class ConsumerConfig(name: String,
                          pactFile: File,
                          stateChange: Option[URL] = None,
                          stateChangeUsesBody: Boolean = false,
                          verificationType: PactVerification = PactVerification.REQUST_RESPONSE,
                          packagesToScan: List[String] = List()
                         ) extends ConsumerConfigInfo
case class ConsumersFromDirectory(dir: File,
                                  stateChange: Option[URL] = None,
                                  stateChangeUsesBody: Boolean = false,
                                  verificationType: PactVerification = PactVerification.REQUST_RESPONSE,
                                  packagesToScan: List[String] = List()
                          ) extends ConsumerConfigInfo
case class ConsumersFromPactBroker(pactBrokerUrl: URL, tag: Option[String] = None) extends ConsumerConfigInfo

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
                          consumers: ArrayBuffer[ConsumerConfigInfo] = ArrayBuffer(),
                          pactFileDirectory: Option[File] = None,
                          pactBrokerUrl: Option[URL] = None,
                          requestFilter: Option[HttpRequest => Unit] = None,
                          stateChangeRequestFilter: Option[HttpRequest => Unit] = None

//def startProviderTask
//def terminateProviderTask
                         ) {

  def hasPactWith(consumer: ConsumerConfig) = {
    consumers += consumer
    this
  }

  def hasPactsInDirectory(dir: File, stateChange: Option[URL] = None,
                          stateChangeUsesBody: Boolean = false,
                          verificationType: PactVerification = PactVerification.REQUST_RESPONSE,
                          packagesToScan: List[String] = List()) = {
    consumers += ConsumersFromDirectory(dir, stateChange, stateChangeUsesBody, verificationType, packagesToScan)
    this
  }

  def hasPactsFromPactBroker(pactBrokerUrl: URL, tag:  Option[String] = None) = {
    consumers += ConsumersFromPactBroker(pactBrokerUrl, tag)
    this
  }

}

object Verification {

  implicit def providerConfigToProviderInfo(provider: ProviderConfig) : ProviderInfo = {
    val provInfo = new ProviderInfo(provider.name)

    provInfo.setProtocol(provider.protocol)
    provInfo.setHost(provider.host)
    provInfo.setPort(provider.port)
    provInfo.setPath(provider.path)

//    def startProviderTask
//    def terminateProviderTask

    if (provider.requestFilter.isDefined) {
      provInfo.setRequestFilter(provider.requestFilter.get)
    }
    if (provider.stateChangeRequestFilter.isDefined) {
      provInfo.setStateChangeRequestFilter(provider.stateChangeRequestFilter.get)
    }

    provInfo.setInsecure(provider.insecure)
    if (provider.trustStore.isDefined) {
      provInfo.setTrustStore(provider.trustStore.get)
    }
    provInfo.setTrustStorePassword(provider.trustStorePassword)
    if (provider.stateChangeUrl.isDefined) {
      provInfo.setStateChangeUrl(provider.stateChangeUrl.get)
    }
    provInfo.setStateChangeUsesBody(provider.stateChangeUsesBody)
    provInfo.setVerificationType(provider.verificationType)
    provInfo.setPackagesToScan(JavaConversions.seqAsJavaList(provider.packagesToScan))

    provider.consumers.foreach {
      case ci: ConsumerConfig => provInfo.getConsumers.add(
        new ConsumerInfo(ci.name, ci.pactFile, ci.stateChange.orNull,
          ci.stateChangeUsesBody, ci.verificationType,
          JavaConversions.seqAsJavaList(ci.packagesToScan)))
      case dir: ConsumersFromDirectory => provInfo.getConsumers.addAll(
        ProviderUtils.loadPactFiles(provInfo, dir.dir, dir.stateChange.orNull, dir.stateChangeUsesBody,
        dir.verificationType, JavaConversions.seqAsJavaList(dir.packagesToScan)).asInstanceOf[util.List[ConsumerInfo]])
      case ConsumersFromPactBroker(pactBrokerUrl, None) => provInfo.hasPactsFromPactBroker(pactBrokerUrl.toString)
      case ConsumersFromPactBroker(pactBrokerUrl, Some(tag)) => provInfo.hasPactsFromPactBrokerWithTag(pactBrokerUrl.toString, tag)
    }

    provInfo
  }

  def verify(providers: Seq[ProviderConfig]) = {
    val failures = new util.HashMap[String, AnyRef]()
    val verifier = new ProviderVerifier()
    verifier.setProjectHasProperty( (property: String) => System.getProperty(property) != null )
    verifier.setProjectGetProperty( (property: String) => System.getProperty(property) )
    verifier.setPactLoadFailureMessage( (consumer: ConsumerInfo) =>
      s"You must specify the pactFile to execute for consumer '${consumer.getName}'."
    )
//    verifier.isBuildSpecificTask = { false }
//
//    verifier.projectClasspath = {
//      List<URL> urls = []
//      for (element in classpathElements) {
//        urls.add(new File(element).toURI().toURL())
//      }
//      urls as URL[]
//    }

    providers.foreach { provider =>
      failures.putAll(verifier.verifyProvider(provider).asInstanceOf[util.HashMap[String, AnyRef]])
    }

    if (!failures.isEmpty) {
      verifier.displayFailures(failures)
      sys.error(s"There were ${failures.size()} pact failures")
    }
  }

}
