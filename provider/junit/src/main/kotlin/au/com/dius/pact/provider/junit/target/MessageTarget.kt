package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junit.descriptions.DescriptionGenerator
import au.com.dius.pact.provider.junitsupport.Provider
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URLClassLoader
import java.util.function.Function
import java.util.function.Supplier

private val logger = KotlinLogging.logger {}

/**
 * Out-of-the-box implementation of [Target], that run [Interaction] against message pact and verify response
 * By default it will scan all packages for annotated methods, but a list of packages can be provided to reduce
 * the performance cost
 * @param packagesToScan List of JVM packages
 */
open class MessageTarget @JvmOverloads constructor(
  private val packagesToScan: List<String> = emptyList(),
  private val classLoader: ClassLoader? = null
) : BaseTarget() {

  /**
   * {@inheritDoc}
   */
  override fun testInteraction(
    consumerName: String,
    interaction: Interaction,
    source: PactSource,
    context: MutableMap<String, Any>,
    pending: Boolean
  ) {
    // TODO: Require the plugin config here
    val result = verifier.verifyResponseByInvokingProviderMethods(provider, consumer, interaction,
      interaction.description, mutableMapOf(), false, emptyMap())
    reportTestResult(result, verifier)

    try {
      if (result is VerificationResult.Failed) {
        verifier.displayFailures(listOf(result))
        val descriptionGenerator = DescriptionGenerator(testClass, source, consumerName)
        val description = descriptionGenerator.generate(interaction).methodName
        throw AssertionError(description + verifier.generateErrorStringFromVerificationResult(listOf(result)))
      }
    } finally {
      verifier.finaliseReports()
    }
  }

  override fun setupVerifier(
    interaction: Interaction,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    pactSource: PactSource?
  ): IProviderVerifier {
    val verifier = ProviderVerifier()
    verifier.projectClassLoader = Supplier { this.classLoader }
    verifier.projectClasspath = Supplier {
      logger.debug { "Classloader = ${this.classLoader}" }
      when (this.classLoader) {
        is URLClassLoader -> this.classLoader.urLs.asList()
        else -> emptyList()
      }
    }
    val defaultProviderMethodInstance = verifier.providerMethodInstance
    verifier.providerMethodInstance = Function { m ->
      if (m.declaringClass == testTarget.javaClass) {
        testTarget
      } else {
        defaultProviderMethodInstance.apply(m)
      }
    }

    setupReporters(verifier)

    verifier.initialiseReporters(provider)
    verifier.reportVerificationForConsumer(consumer, provider, pactSource)

    if (interaction.providerStates.isNotEmpty()) {
      for ((name) in interaction.providerStates) {
        verifier.reportStateForInteraction(name.toString(), provider, consumer, true)
      }
    }

    return verifier
  }

  override fun getProviderInfo(source: PactSource): ProviderInfo {
    val provider = ProviderUtils.findAnnotation(testClass.javaClass, Provider::class.java)!!
    val providerInfo = ProviderInfo(provider.value)
    providerInfo.verificationType = PactVerification.ANNOTATED_METHOD
    providerInfo.packagesToScan = packagesToScan

    if (source is PactBrokerSource<*>) {
      val (_, _, _, pacts) = source
      providerInfo.consumers = pacts.entries.flatMap { e ->
        e.value.map { p -> ConsumerInfo(e.key.name, p) }
      }.toMutableList()
    } else if (source is DirectorySource) {
      val (_, pacts) = source
      providerInfo.consumers = pacts.entries.map { e -> ConsumerInfo(e.value.consumer.name, e.value) }.toMutableList()
    }

    return providerInfo
  }

  override fun validForInteraction(interaction: Interaction) = interaction.isAsynchronousMessage()
}
