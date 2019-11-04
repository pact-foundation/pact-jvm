package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.DirectorySource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactBrokerSource
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.junit.Provider
import mu.KLogging
import java.net.URLClassLoader
import java.util.function.Function
import java.util.function.Supplier

/**
 * Out-of-the-box implementation of [Target], that run [Interaction] against message pact and verify response
 * By default it will scan all packages for annotated methods, but a list of packages can be provided to reduce
 * the performance cost
 * @param packagesToScan List of JVM packages
 */
open class AmqpTarget @JvmOverloads constructor(
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
    context: Map<String, Any>
  ) {
    val provider = getProviderInfo(source)
    val consumer = ConsumerInfo(consumerName)
    val verifier = setupVerifier(interaction, provider, consumer)

    val failures = mutableMapOf<String, Any>()
    verifier.verifyResponseByInvokingProviderMethods(provider, consumer, interaction, interaction.description,
      failures)
    reportTestResult(failures.isEmpty(), verifier)

    try {
      if (failures.isNotEmpty()) {
        verifier.displayFailures(failures)
        throw getAssertionError(failures)
      }
    } finally {
      verifier.finaliseReports()
    }
  }

  override fun setupVerifier(
    interaction: Interaction,
    provider: ProviderInfo,
    consumer: ConsumerInfo
  ): IProviderVerifier {
    val verifier = ProviderVerifier()
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

    setupReporters(verifier, provider.name, interaction.description)

    verifier.initialiseReporters(provider)
    verifier.reportVerificationForConsumer(consumer, provider, null)

    if (interaction.providerStates.isNotEmpty()) {
      for ((name) in interaction.providerStates) {
        verifier.reportStateForInteraction(name.toString(), provider, consumer, true)
      }
    }

    verifier.reportInteractionDescription(interaction)

    return verifier
  }

  override fun getProviderInfo(source: PactSource): ProviderInfo {
    val provider = testClass.getAnnotation(Provider::class.java)
    val providerInfo = ProviderInfo(provider.value)
    providerInfo.verificationType = PactVerification.ANNOTATED_METHOD
    providerInfo.packagesToScan = packagesToScan

    if (source is PactBrokerSource<*>) {
      val (_, _, _, pacts) = source
      providerInfo.consumers = pacts.entries.flatMap { e -> e.value.map { p -> ConsumerInfo(e.key.name, p) } }.toMutableList()
    } else if (source is DirectorySource<*>) {
      val (_, pacts) = source
      providerInfo.consumers = pacts.entries.map { e -> ConsumerInfo(e.value.consumer.name, e.value) }.toMutableList()
    }

    return providerInfo
  }

  companion object : KLogging()
}
