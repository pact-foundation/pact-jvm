package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderUtils.instantiatePactLoader
import au.com.dius.pact.provider.junitsupport.AllowOverridePactUrl
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.JUnitProviderTestSupport.checkForOverriddenPactUrl
import au.com.dius.pact.provider.junitsupport.JUnitProviderTestSupport.filterPactsByAnnotations
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.NoPactsFoundException
import au.com.dius.pact.provider.junitsupport.loader.PactLoader
import com.github.michaelbull.result.getOrElse
import mu.KLogging
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import java.util.stream.Stream

val namespace: ExtensionContext.Namespace = ExtensionContext.Namespace.create("pact-jvm")

/**
 * Main TestTemplateInvocationContextProvider for JUnit 5 Pact verification tests. This class needs to be applied to
 * a test template method on a test class annotated with a @Provider annotation.
 */
open class PactVerificationInvocationContextProvider : TestTemplateInvocationContextProvider {

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    logger.debug { "provideTestTemplateInvocationContexts called" }
    val tests = resolvePactSources(context)
    return when {
      tests.first.isNotEmpty() -> tests.first.stream() as Stream<TestTemplateInvocationContext>
      AnnotationSupport.isAnnotated(context.requiredTestClass, IgnoreNoPactsToVerify::class.java) ->
        listOf(DummyTestTemplate).stream() as Stream<TestTemplateInvocationContext>
      else -> throw NoPactsFoundException("No Pact files were found to verify\n${tests.second}")
    }
  }

  private fun resolvePactSources(context: ExtensionContext): Pair<List<PactVerificationExtension>, String> {
    var description = ""
    val providerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Provider::class.java)
    val serviceName = if (providerInfo.isPresent && providerInfo.get().value.isNotEmpty()) {
      providerInfo.get().value
    } else {
      System.getProperty("pact.provider.name")
    }
    if (serviceName.isNullOrEmpty()) {
      throw UnsupportedOperationException("Provider name should be specified by using either " +
        "@${Provider::class.java.name} annotation or the 'pact.provider.name' system property")
    }
    description += "Provider: $serviceName"

    val consumerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Consumer::class.java)
    val consumerName = consumerInfo.orElse(null)?.value
    if (consumerName.isNotEmpty()) {
      description += "\nConsumer: $consumerName"
    }

    validateStateChangeMethods(context.requiredTestClass)

    logger.debug { "Verifying pacts for provider '$serviceName' and consumer '$consumerName'" }

    val pactSources = findPactSources(context).flatMap { loader ->
      val valueResolver = getValueResolver(context)
      if (valueResolver != null) {
        loader.setValueResolver(valueResolver)
      }
      description += "\nSource: ${loader.description()}"
      val pacts = handleWith<List<Pact>> { loader.load(serviceName) }.getOrElse {
        val ignoreAnnotation = AnnotationSupport.findAnnotation(context.requiredTestClass, IgnoreNoPactsToVerify::class.java)
        if (ignoreAnnotation.isPresent && ignoreAnnotation.get().ignoreIoErrors == "true") {
          emptyList()
        } else {
          throw it
        }
      }
      filterPactsByAnnotations(pacts, context.requiredTestClass)
    }.filter { p -> consumerName == null || p.consumer.name == consumerName }

    val interactionFilter = System.getProperty("pact.filter.description")
    return Pair(pactSources.flatMap { pact ->
      pact.interactions
        .filter {
          interactionFilter.isNullOrEmpty() || it.description.matches(Regex(interactionFilter))
        }
        .map { PactVerificationExtension(pact, pact.source, it, serviceName, consumerName) }
    }, description)
  }

  protected open fun getValueResolver(context: ExtensionContext): ValueResolver? = null

  private fun validateStateChangeMethods(testClass: Class<*>) {
    val errors = mutableListOf<String>()
    AnnotationSupport.findAnnotatedMethods(testClass, State::class.java, HierarchyTraversalMode.TOP_DOWN).forEach {
      if (it.parameterCount > 1) {
        errors.add("State change method ${it.name} should either take no parameters or a single Map parameter")
      } else if (it.parameterCount == 1 && !Map::class.java.isAssignableFrom(it.parameterTypes[0])) {
        errors.add("State change method ${it.name} should take only a single Map parameter")
      }
    }

    if (errors.isNotEmpty()) {
      throw UnsupportedOperationException(errors.joinToString("\n"))
    }
  }

  private fun findPactSources(context: ExtensionContext): List<PactLoader> {
    val pactSources = ProviderUtils.findAllPactSources(context.requiredTestClass.kotlin)
    if (pactSources.isEmpty()) {
      throw UnsupportedOperationException("Did not find any PactSource annotations. " +
        "At least one pact source must be set")
    }

    logger.debug { "Pact sources on test class: $pactSources" }
    return pactSources.map { (pactSource, annotation) ->
      instantiatePactLoader(pactSource, context.requiredTestClass, annotation)
    }.map {
      checkForOverriddenPactUrl(it,
        context.requiredTestClass.getAnnotation(AllowOverridePactUrl::class.java),
        context.requiredTestClass.getAnnotation(Consumer::class.java))
      it
    }
  }

  override fun supportsTestTemplate(context: ExtensionContext): Boolean {
    return AnnotationSupport.isAnnotated(context.requiredTestClass, Provider::class.java)
  }

  companion object : KLogging()
}
