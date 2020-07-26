package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junit.target.BaseTarget
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter
import au.com.dius.pact.provider.spring.WebFluxProviderVerifier
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import java.net.URLClassLoader
import java.util.function.Consumer
import java.util.function.Supplier

class WebFluxTarget(
  var runTimes: Int = 1
) : BaseTarget() {

  lateinit var routerFunction: RouterFunction<out ServerResponse>

  override fun getProviderInfo(source: PactSource): ProviderInfo {
    val provider = testClass.getAnnotation(Provider::class.java)
    val providerInfo = ProviderInfo(provider.value)

    val methods = testClass.getAnnotatedMethods(TargetRequestFilter::class.java)
    if (methods.isNotEmpty()) {
      validateTargetRequestFilters(methods)
      providerInfo.requestFilter = Consumer<Any> { httpRequest ->
        methods.forEach { method ->
          try {
            method.invokeExplosively(testTarget, httpRequest)
          } catch (t: Throwable) {
            throw AssertionError("Request filter method ${method.name} failed with an exception", t)
          }
        }
      }
    }

    return providerInfo
  }

  override fun setupVerifier(
    interaction: Interaction,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    pactSource: PactSource?
  ): IProviderVerifier {
    var verifier = WebFluxProviderVerifier()

    setupReporters(verifier)

    verifier.projectClasspath = Supplier { (ClassLoader.getSystemClassLoader() as URLClassLoader).urLs.toList() }

    verifier.initialiseReporters(provider)
    verifier.reportVerificationForConsumer(consumer, provider, pactSource)

    if (interaction.providerStates.isNotEmpty()) {
      for ((name) in interaction.providerStates) {
        verifier.reportStateForInteraction(name.toString(), provider, consumer, true)
      }
    }

    verifier.reportInteractionDescription(interaction)

    return verifier
  }

  override fun testInteraction(
    consumerName: String,
    interaction: Interaction,
    source: PactSource,
    context: Map<String, Any>
  ) {
    val provider = getProviderInfo(source)
    val consumer = consumerInfo(consumerName, source)
    provider.verificationType = PactVerification.ANNOTATED_METHOD

    val verifier = setupVerifier(interaction, provider, consumer, source) as WebFluxProviderVerifier

    val failures = HashMap<String, Any>()

    val results = 1.rangeTo(runTimes).map {
      val webClient = WebTestClient.bindToRouterFunction(routerFunction).build()

      verifier.verifyResponseFromProvider(
        provider, interaction as RequestResponseInteraction, interaction.description,
        failures, webClient, consumer.pending
      )
    }
    val result = results.fold(VerificationResult.Ok) { acc: VerificationResult, r -> acc.merge(r) }

    reportTestResult(result, verifier)

    try {
      if (result is VerificationResult.Failed) {
        val errors = results.filterIsInstance<VerificationResult.Failed>()
        verifier.displayFailures(errors)
        throw AssertionError(verifier.generateErrorStringFromVerificationResult(errors))
      }
    } finally {
      verifier.finaliseReports()
    }
  }

  override fun getRequestClass(): Class<*> = WebTestClient.RequestHeadersSpec::class.java
}
