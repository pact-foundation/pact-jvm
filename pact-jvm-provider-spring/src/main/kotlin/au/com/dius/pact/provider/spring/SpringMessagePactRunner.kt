package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.provider.junit.InteractionRunner
import au.com.dius.pact.provider.junit.MessagePactRunner
import au.com.dius.pact.provider.junit.loader.PactLoader
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks

/**
 * Pact runner for REST providers that boots up the spring context
 */
open class SpringMessagePactRunner(clazz: Class<*>) : MessagePactRunner<Message>(clazz) {

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

  private fun initTestContextManager(clazz: Class<*>): TestContextManager {
    if (testContextManager == null) {
      testContextManager = TestContextManager(clazz)
    }

    return testContextManager!!
  }

  override fun newInteractionRunner(testClass: TestClass, pact: Pact<Message>, pactSource: PactSource): InteractionRunner {
    return SpringInteractionRunner(testClass, pact, pactSource, initTestContextManager(testClass.javaClass))
  }

  override fun getPactSource(clazz: TestClass): PactLoader {
    initTestContextManager(clazz.javaClass)
    val environment = testContextManager!!.testContext.applicationContext.environment
    val pactSource = super.getPactSource(clazz)
    pactSource.setValueResolver(SpringEnvironmentResolver(environment))
    return pactSource
  }
}
