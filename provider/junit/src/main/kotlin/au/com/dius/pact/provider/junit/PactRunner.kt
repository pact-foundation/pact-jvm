package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.json.JsonException
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderUtils.findAnnotation
import au.com.dius.pact.provider.ProviderUtils.instantiatePactLoader
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junitsupport.AllowOverridePactUrl
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.JUnitProviderTestSupport.checkForOverriddenPactUrl
import au.com.dius.pact.provider.junitsupport.JUnitProviderTestSupport.filterPactsByAnnotations
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.NoPactsFoundException
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import au.com.dius.pact.provider.junitsupport.loader.PactLoader
import au.com.dius.pact.provider.junitsupport.loader.PactSource
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import mu.KLogging
import org.junit.Ignore
import org.junit.runner.notification.RunNotifier
import org.junit.runners.ParentRunner
import org.junit.runners.model.InitializationError
import org.junit.runners.model.TestClass
import java.io.IOException

/**
 * JUnit Runner runs pacts against provider
 * To set up name of tested provider use [Provider] annotation
 * To point on pact's source use [PactBroker], [PactFolder] or [PactSource] annotations
 *
 *
 * To point provider for testing use combination of [Target] interface and [TestTarget] annotation
 * There is out-of-the-box implementation of [Target]:
 * [HttpTarget] that will play interaction from pacts as http request and check http responses
 *
 *
 * Runner supports:
 * - [org.junit.BeforeClass], [org.junit.AfterClass] and [org.junit.ClassRule] annotations,
 * that will be run once - before/after whole contract test suite
 *
 *
 * - [org.junit.Before], [org.junit.After] and [org.junit.Rule] annotations,
 * that will be run before/after each test of interaction
 * **WARNING:** please note, that only [org.junit.rules.TestRule] is possible to use with this runner,
 * i.e. [org.junit.rules.MethodRule] **IS NOT supported**
 *
 *
 * - [State] - before each interaction that require state change,
 * all methods annotated by [State] with appropriate state listed will be invoked
 */
open class PactRunner<I>(private val clazz: Class<*>) : ParentRunner<InteractionRunner<I>>(clazz) where I : Interaction {

  private val children = mutableListOf<InteractionRunner<I>>()
  private var valueResolver = SystemPropertyResolver()
  private var initialized = false

  private fun initialize() {
    if (initialized) {
      return
    }

    if (clazz.getAnnotation(Ignore::class.java) != null) {
      logger.info("Ignore annotation detected, exiting")
    } else {
      val providerInfo = findAnnotation(clazz, Provider::class.java) ?: throw InitializationError(
              "Provider name should be specified by using ${Provider::class.java.simpleName} annotation")
      logger.debug { "Found annotation $providerInfo" }
      val serviceName = providerInfo.value

      val consumerInfo = findAnnotation(clazz, Consumer::class.java)
      if (consumerInfo != null) {
        logger.debug { "Found annotation $consumerInfo" }
      }
      val consumerName = consumerInfo?.value

      val testClass = TestClass(clazz)
      val ignoreNoPactsToVerify = findAnnotation(clazz, IgnoreNoPactsToVerify::class.java)
      if (ignoreNoPactsToVerify != null) {
        logger.debug { "Found annotation $ignoreNoPactsToVerify" }
      }
      val ignoreIoErrors = try {
        valueResolver.resolveValue(ignoreNoPactsToVerify?.ignoreIoErrors)
      } catch (e: RuntimeException) {
        logger.debug(e) { "Failed to resolve property value" }
        ignoreNoPactsToVerify?.ignoreIoErrors
      } ?: "false"

      val pactLoader = getPactSource(testClass, consumerInfo)
      val pacts = try {
        filterPacts(pactLoader.load(serviceName)
          .filter { p -> consumerName == null || p.consumer.name == consumerName } as List<Pact<I>>)
      } catch (e: IOException) {
        if (ignoreIoErrors == "true") {
          logger.warn { "\n" + WARNING_ON_IGNORED_IOERROR.trimIndent() }
          logger.debug(e) { "Failed to load pact files" }
          emptyList<Pact<I>>()
        } else {
          throw InitializationError(e)
        }
      } catch (e: JsonException) {
        if (ignoreIoErrors == "true") {
          logger.warn { "\n" + WARNING_ON_IGNORED_IOERROR.trimIndent() }
          logger.debug(e) { "Failed to load pact files" }
          emptyList<Pact<I>>()
        } else {
          throw InitializationError(e)
        }
      } catch (e: NoPactsFoundException) {
        logger.debug(e) { "No pacts found" }
        emptyList<Pact<I>>()
      }

      if (pacts.isEmpty()) {
        if (ignoreNoPactsToVerify != null) {
          logger.warn { "Did not find any pact files for provider ${providerInfo.value}" }
        } else {
          throw InitializationError("Did not find any pact files for provider ${providerInfo.value}")
        }
      }

      setupInteractionRunners(testClass, pacts, pactLoader)
    }
    initialized = true
  }

  protected open fun setupInteractionRunners(testClass: TestClass, pacts: List<Pact<I>>, pactLoader: PactLoader) {
    for (pact in pacts) {
      this.children.add(newInteractionRunner(testClass, pact, pact.source))
    }
  }

  protected open fun newInteractionRunner(
    testClass: TestClass,
    pact: Pact<I>,
    pactSource: au.com.dius.pact.core.model.PactSource
  ): InteractionRunner<I> {
    return InteractionRunner(testClass, pact, pactSource)
  }

  protected open fun filterPacts(pacts: List<Pact<I>>): List<Pact<I>> {
    return filterPactsByAnnotations(pacts, testClass.javaClass)
  }

  override fun getChildren(): MutableList<InteractionRunner<I>> {
    initialize()
    return children
  }

  override fun describeChild(child: InteractionRunner<I>) = child.description

  override fun runChild(interaction: InteractionRunner<I>, notifier: RunNotifier) {
    interaction.run(notifier)
  }

  protected open fun getPactSource(clazz: TestClass, consumerInfo: Consumer?): PactLoader {
    val pactSources = ProviderUtils.findAllPactSources(clazz.javaClass.kotlin)
    if (pactSources.size > 1) {
      throw InitializationError(
        "Exactly one pact source should be set, found ${pactSources.size}: " +
          pactSources.map { it.first }.joinToString(", "))
    } else if (pactSources.isEmpty()) {
      throw InitializationError("Did not find any PactSource annotations. Exactly one pact source should be set")
    }

    val (pactSource, annotation) = pactSources.first()
    return try {
      val loader = instantiatePactLoader(pactSource, clazz.javaClass, annotation)
      checkForOverriddenPactUrl(loader, findAnnotation(clazz.javaClass, AllowOverridePactUrl::class.java),
        consumerInfo)
      loader
    } catch (e: ReflectiveOperationException) {
      logger.error(e) { "Error while creating pact source" }
      throw InitializationError(e)
    }
  }

  companion object : KLogging() {
    const val WARNING_ON_IGNORED_IOERROR = """
         ---------------------------------------------------------------------------
         | WARNING! Ignoring IO Exception received when loading Pact files as      |
         | WARNING! the @IgnoreNoPactsToVerify annotation is present and           |
         | WARNING! ignoreIoErrors is set to true. Make sure this is not happening |
         | WARNING! on your CI server, as this could result in your build passing  |
         | WARNING! with no providers having been verified due to a configuration  |
         | WARNING! error.                                                         |
         -------------------------------------------------------------------------"""
  }
}
