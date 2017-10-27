package au.com.dius.pact.provider.spring

import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.provider.junit.InteractionRunner
import au.com.dius.pact.provider.junit.MessagePactRunner
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks

/**
 * Pact runner for REST providers that boots up the spring context
 */
open class SpringMessagePactRunner(clazz: Class<*>) : MessagePactRunner(clazz) {

  private var testContextManager: TestContextManager? = null

  init {
    initTestContextManager(clazz)
  }

  override fun withBeforeClasses(statement: Statement?): Statement {
    val withBeforeClasses = super.withBeforeClasses(statement)
    return RunBeforeTestClassCallbacks(withBeforeClasses, testContextManager)
  }

  override fun withAfterClasses(statement: Statement?): Statement {
    val withAfterClasses = super.withAfterClasses(statement)
    return RunAfterTestClassCallbacks(withAfterClasses, testContextManager)
  }

  private fun initTestContextManager(clazz: Class<*>) : TestContextManager {
    if (testContextManager == null) {
      testContextManager = TestContextManager(clazz)
    }

    return testContextManager!!
  }

  override fun newInteractionRunner(testClass: TestClass, pact: Pact, pactSource: PactSource): InteractionRunner {
    return SpringInteractionRunner(testClass, pact, pactSource, initTestContextManager(testClass.javaClass))
  }
}
