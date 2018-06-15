package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.junit.InteractionRunner
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.spring.target.SpringBootHttpTarget
import org.junit.After
import org.junit.Before
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.MultipleFailureException
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import org.springframework.test.context.TestContextManager

open class SpringBeforeRunner(
  private val next: Statement,
  private val befores: List<FrameworkMethod>,
  private val testInstance: Any,
  private val testContextManager: TestContextManager
) : Statement() {

  override fun evaluate() {
    for (before in befores) {
      testContextManager.beforeTestMethod(testInstance, before.method)
      before.invokeExplosively(testInstance)
    }
    next.evaluate()
  }
}

open class SpringAfterRunner(
  private val next: Statement,
  private val afters: List<FrameworkMethod>,
  private val testInstance: Any,
  private val testContextManager: TestContextManager
) : Statement() {

  override fun evaluate() {
    val errors: MutableList<Throwable> = mutableListOf()
    try {
      next.evaluate()
    } catch (e: Throwable) {
      errors.add(e)
    } finally {
      for (each in afters) {
        var testException: Throwable? = null
        try {
          each.invokeExplosively(testInstance)
        } catch (e: Throwable) {
          errors.add(e)
          testException = e
        }

        try {
          testContextManager.afterTestMethod(testInstance, each.method, testException)
        } catch (ex: Throwable) {
          errors.add(ex)
        }
      }
    }

    MultipleFailureException.assertEmpty(errors)
  }
}

open class SpringInteractionRunner<I>(
  private val testClass: TestClass,
  pact: Pact<I>,
  pactSource: PactSource?,
  private val testContextManager: TestContextManager
) : InteractionRunner(testClass, pact, pactSource) where I : Interaction {

  override fun withBefores(interaction: Interaction, testInstance: Any, statement: Statement): Statement {
    val befores = testClass.getAnnotatedMethods(Before::class.java)
    return if (befores.isNotEmpty()) {
      SpringBeforeRunner(statement, befores, testInstance, testContextManager)
    } else {
      statement
    }
  }

  override fun withAfters(interaction: Interaction, testInstance: Any, statement: Statement): Statement {
    val afters = testClass.getAnnotatedMethods(After::class.java)
    return if (afters.isNotEmpty()) {
      SpringAfterRunner(statement, afters, testInstance, testContextManager)
    } else {
      statement
    }
  }

  override fun createTest(): Any {
    val test = super.createTest()
    testContextManager.prepareTestInstance(test)
    return test
  }

  override fun setupTargetForInteraction(target: Target) {
    super.setupTargetForInteraction(target)

    if (target is SpringBootHttpTarget) {
      val environment = testContextManager.testContext.applicationContext.environment
      val port = environment.getProperty("local.server.port")
      target.setPort(Integer.parseInt(port))
    }
  }
}
