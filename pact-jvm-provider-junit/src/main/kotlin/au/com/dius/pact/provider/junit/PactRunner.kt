package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.provider.junit.JUnitProviderTestSupport.filterPactsByAnnotations
import au.com.dius.pact.provider.junit.loader.NoPactsFoundException
import au.com.dius.pact.provider.junit.loader.PactBroker
import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.loader.PactLoader
import au.com.dius.pact.provider.junit.loader.PactSource
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import groovy.json.JsonException
import mu.KLogging
import org.junit.Ignore
import org.junit.runner.notification.RunNotifier
import org.junit.runners.ParentRunner
import org.junit.runners.model.InitializationError
import org.junit.runners.model.TestClass
import java.io.IOException
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

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
open class PactRunner<I>(clazz: Class<*>) : ParentRunner<InteractionRunner<I>>(clazz) where I: Interaction {

  private val child = mutableListOf<InteractionRunner<I>>()

  init {
    if (clazz.getAnnotation(Ignore::class.java) != null) {
      logger.info("Ignore annotation detected, exiting")
    } else {

      val providerInfo = clazz.getAnnotation(Provider::class.java) ?: throw InitializationError(
              "Provider name should be specified by using ${Provider::class.java.name} annotation")
      val serviceName = providerInfo.value

      val consumerInfo = clazz.getAnnotation(Consumer::class.java)
      val consumerName = consumerInfo?.value

      val testClass = TestClass(clazz)

      val pactLoader = getPactSource(testClass)
      val pacts = try {
        filterPacts(pactLoader.load(serviceName)
          .filter { p -> consumerName == null || p.consumer.name == consumerName } as List<Pact<I>>)
      } catch (e: IOException) {
        throw InitializationError(e)
      } catch (e: JsonException) {
        throw InitializationError(e)
      } catch (e: NoPactsFoundException) {
        logger.debug(e) { "No pacts found" }
        emptyList<Pact<I>>()
      }

      if (pacts.isEmpty()) {
        if (clazz.isAnnotationPresent(IgnoreNoPactsToVerify::class.java)) {
          logger.warn { "Did not find any pact files for provider ${providerInfo.value}" }
        } else {
          throw InitializationError("Did not find any pact files for provider ${providerInfo.value}")
        }
      }

      setupInteractionRunners(testClass, pacts, pactLoader)
    }
  }

  protected open fun setupInteractionRunners(testClass: TestClass, pacts: List<Pact<I>>, pactLoader: PactLoader) {
    for (pact in pacts) {
      this.child.add(newInteractionRunner(testClass, pact, pactLoader.pactSource))
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

  override fun getChildren() = child

  override fun describeChild(child: InteractionRunner<I>) = child.description

  override fun runChild(interaction: InteractionRunner<I>, notifier: RunNotifier) {
    interaction.run(notifier)
  }

  protected open fun getPactSource(clazz: TestClass): PactLoader {
    val pactSource = clazz.getAnnotation(PactSource::class.java)
    val pactLoaders = clazz.annotations
      .filter { annotation -> annotation.annotationClass.findAnnotation<PactSource>() != null }
    if ((if (pactSource == null) 0 else 1) + pactLoaders.size != 1) {
      throw InitializationError("Exactly one pact source should be set")
    }

    try {
      if (pactSource != null) {
        val pactLoaderClass = pactSource.value
        return try {
          // Checks if there is a constructor with one argument of type Class.
          val constructorWithClass = pactLoaderClass.java.getDeclaredConstructor(Class::class.java)
          if (constructorWithClass != null) {
            constructorWithClass.isAccessible = true
            constructorWithClass.newInstance(clazz.javaClass)
          } else {
            pactLoaderClass.createInstance()
          }
        } catch (e: NoSuchMethodException) {
          logger.error(e) { e.message }
          pactLoaderClass.createInstance()
        }
      } else {
        val annotation = pactLoaders.first()
        return annotation.annotationClass.findAnnotation<PactSource>()!!.value.java
          .getConstructor(annotation.annotationClass.java).newInstance(annotation)
      }
    } catch (e: ReflectiveOperationException) {
      logger.error(e) { "Error while creating pact source" }
      throw InitializationError(e)
    }
  }

  companion object : KLogging()
}
