package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.junit.Consumer
import au.com.dius.pact.provider.junit.JUnitProviderTestSupport.filterPactsByAnnotations
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.loader.PactLoader
import au.com.dius.pact.provider.junit.loader.PactSource
import mu.KLogging
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import java.util.stream.Stream
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

class PactVerificationContext(private val pact: Pact<Interaction>,
                              private val interaction: Interaction) : TestTemplateInvocationContext {
  override fun getDisplayName(invocationIndex: Int): String {
    return "${pact.consumer.name} - ${interaction.description}"
  }

  override fun getAdditionalExtensions(): MutableList<Extension> {
    return mutableListOf(PactInteractionVerificationExtension(pact, interaction))
  }
}

class PactInteractionVerificationExtension(pact: Pact<Interaction>, interaction: Interaction) : Extension

class PactVerificationInvocationContextProvider : TestTemplateInvocationContextProvider {
  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    logger.debug { "provideTestTemplateInvocationContexts called" }

    val providerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Provider::class.java)
    if (!providerInfo.isPresent) {
      throw UnsupportedOperationException("Provider name should be specified by using @${Provider::class.java.name} annotation")
    }
    val serviceName = providerInfo.get().value

    val consumerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Consumer::class.java)
    val consumerName = consumerInfo.orElse(null)?.value

    logger.debug { "Verifying pacts for provider '$serviceName' and consumer '$consumerName'" }

    val pactSources = findPactSources(context).flatMap { it.load(serviceName) }
      .filter { p -> consumerName == null || p.consumer.name == consumerName }
    val filteredPacts = filterPactsByAnnotations(pactSources, context.requiredTestClass)

    val tests = filteredPacts.flatMap { pact -> pact.interactions.map { PactVerificationContext(pact, it) } }
    return tests.stream() as Stream<TestTemplateInvocationContext>
  }

  private fun findPactSources(context: ExtensionContext): List<PactLoader> {
    val pactSource = context.requiredTestClass.getAnnotation(PactSource::class.java)
    logger.debug { "Pact source on test class: $pactSource" }
    val pactLoaders = context.requiredTestClass.annotations.filter { annotation ->
      annotation.annotationClass.findAnnotation<PactSource>() != null
    }
    logger.debug { "Pact loaders on test class: $pactLoaders" }

    if (pactSource == null && pactLoaders.isEmpty()) {
      throw UnsupportedOperationException("At least one pact source must be present on the test class")
    }

    return pactLoaders.plus(pactSource).filterNotNull().map {
      if (it is PactSource) {
        val pactLoaderClass = pactSource.value
        try {
          // Checks if there is a constructor with one argument of type Class.
          val constructorWithClass = pactLoaderClass.java.getDeclaredConstructor(Class::class.java)
          if (constructorWithClass != null) {
            constructorWithClass.isAccessible = true
            constructorWithClass.newInstance(context.requiredTestClass)
          } else {
            pactLoaderClass.createInstance()
          }
        } catch (e: NoSuchMethodException) {
          logger.error(e) { e.message }
          pactLoaderClass.createInstance()
        }
      } else {
        it.annotationClass.findAnnotation<PactSource>()!!.value.java
          .getConstructor(it.annotationClass.java).newInstance(it)
      }
    }
  }

  override fun supportsTestTemplate(context: ExtensionContext): Boolean {
    return AnnotationSupport.isAnnotated(context.requiredTestClass, Provider::class.java)
  }

  companion object : KLogging()
}
