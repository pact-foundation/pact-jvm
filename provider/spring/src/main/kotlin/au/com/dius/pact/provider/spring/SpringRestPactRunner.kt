package au.com.dius.pact.provider.spring

import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.provider.junit.InteractionRunner
import au.com.dius.pact.provider.junit.RestPactRunner
import au.com.dius.pact.provider.junit.loader.PactLoader
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import org.springframework.beans.BeanUtils
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
import org.springframework.test.context.support.DirtiesContextTestExecutionListener
import org.springframework.test.context.web.ServletTestExecutionListener

/**
 * Pact runner for REST providers that boots up the spring context
 */
open class SpringRestPactRunner(clazz: Class<*>) : RestPactRunner<RequestResponseInteraction>(clazz) {

  private var testContextManager: TestContextManager? = null

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
      testContextManager!!.registerTestExecutionListeners(
        BeanUtils.instantiateClass(ServletTestExecutionListener::class.java),
        BeanUtils.instantiateClass(DirtiesContextBeforeModesTestExecutionListener::class.java),
        BeanUtils.instantiateClass(DependencyInjectionTestExecutionListener::class.java),
        BeanUtils.instantiateClass(DirtiesContextTestExecutionListener::class.java)
      )
    }

    return testContextManager!!
  }

  override fun newInteractionRunner(testClass: TestClass, pact: Pact<RequestResponseInteraction>, pactSource: PactSource): InteractionRunner<RequestResponseInteraction> {
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
