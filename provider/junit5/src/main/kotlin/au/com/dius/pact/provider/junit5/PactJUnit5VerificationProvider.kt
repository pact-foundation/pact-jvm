package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.pactbroker.NotFoundHalResponse
import au.com.dius.pact.core.support.Utils
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.getOrElse
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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import java.io.IOException
import java.util.stream.Stream

val namespace: ExtensionContext.Namespace = ExtensionContext.Namespace.create("pact-jvm")
private val logger = KotlinLogging.logger {}

/**
 * Main TestTemplateInvocationContextProvider for JUnit 5 Pact verification tests. This class needs to be applied to
 * a test template method on a test class annotated with a @Provider annotation.
 */
open class PactVerificationInvocationContextProvider : TestTemplateInvocationContextProvider {

  private val ep: ExpressionParser = ExpressionParser()

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    logger.trace { "provideTestTemplateInvocationContexts called" }
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
    val serviceName = lookupProviderName(context, ep)
    if (serviceName.isNullOrEmpty()) {
      throw UnsupportedOperationException("Provider name should be specified by using either " +
        "@${Provider::class.java.name} annotation or the 'pact.provider.name' system property")
    }
    description += "Provider: $serviceName"

    val consumerName = lookupConsumerName(context, ep)
    if (consumerName.isNotEmpty()) {
      description += "\nConsumer: $consumerName"
    }

    validateStateChangeMethods(context.requiredTestClass)

    logger.debug { "Verifying pacts for provider '$serviceName' and consumer '$consumerName'" }

    val valueResolver = getValueResolver(context)
    val pactSources = findPactSources(context).flatMap { loader ->
      if (valueResolver != null) {
        loader.setValueResolver(valueResolver)
      }
      description += "\nSource: ${loader.description()}"
      val pacts = handleWith<List<Pact>> { loader.load(serviceName) }.getOrElse {
        handleException(context, valueResolver, it)
      }
      filterPactsByAnnotations(pacts, context.requiredTestClass)
    }.filter { p -> consumerName == null || p.consumer.name == consumerName }

    val interactionFilter = System.getProperty("pact.filter.description")
    return Pair(pactSources.flatMap { pact ->
      pact.interactions
        .filter {
          interactionFilter.isNullOrEmpty() || it.description.matches(Regex(interactionFilter))
        }
        .map {
          PactVerificationExtension(pact, pact.source, it, serviceName, consumerName, valueResolver ?: SystemPropertyResolver)
        }
    }, description)
  }

  fun handleException(context: ExtensionContext, valueResolver: ValueResolver?, exception: Exception): List<Pact> {
    val ignoreAnnotation = AnnotationSupport.findAnnotation(context.requiredTestClass, IgnoreNoPactsToVerify::class.java)
    return when {
      ignoreAnnotation.isPresent -> {
        val noPactsToVerify = ignoreAnnotation.get()
        when (exception) {
          is IOException -> when {
            noPactsToVerify.ignoreIoErrors == "true" -> emptyList()
            valueResolver != null &&
              ep.parseExpression(noPactsToVerify.ignoreIoErrors, DataType.RAW, valueResolver) == "true" -> emptyList()
            else -> throw exception
          }
          is NotFoundHalResponse -> emptyList()
          else -> throw exception
        }
      }
      else -> throw exception
    }
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

    logger.debug { "Pact sources on test class:\n ${pactSources.joinToString("\n") { it.first.toString() }}" }
    return pactSources.map { (pactSource, annotation) ->
      instantiatePactLoader(pactSource, context.requiredTestClass, context.testInstance.orElse(null), annotation)
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

  companion object {
    fun lookupConsumerName(context: ExtensionContext, ep: ExpressionParser): String? {
      val consumerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Consumer::class.java)
      return ep.parseExpression(consumerInfo.orElse(null)?.value, DataType.STRING)?.toString()
    }

    fun lookupProviderName(context: ExtensionContext, ep: ExpressionParser): String? {
      val providerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Provider::class.java)
      return if (providerInfo.isPresent && providerInfo.get().value.isNotEmpty()) {
        ep.parseExpression(providerInfo.get().value, DataType.STRING)?.toString()
      } else {
        Utils.lookupEnvironmentValue("pact.provider.name")
      }
    }
  }
}
