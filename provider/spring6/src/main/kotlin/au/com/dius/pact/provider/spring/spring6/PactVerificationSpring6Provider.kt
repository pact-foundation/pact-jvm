package au.com.dius.pact.provider.spring.spring6

import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.junit5.PactVerificationExtension
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.springframework.test.context.TestContextManager
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.stream.Stream

open class PactVerificationSpring6Provider : PactVerificationInvocationContextProvider() {

  override fun getValueResolver(context: ExtensionContext): ValueResolver? {
    val store = context.root.getStore(ExtensionContext.Namespace.create(SpringExtension::class.java))
    val testClass = context.requiredTestClass
    val testContextManager = store.getOrComputeIfAbsent(testClass, { TestContextManager(testClass) },
      TestContextManager::class.java)
    val environment = testContextManager.testContext.applicationContext.environment
    return Spring6EnvironmentResolver(environment)
  }

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    return super.provideTestTemplateInvocationContexts(context).map {
      if (it is PactVerificationExtension) {
        PactVerificationSpring6Extension(it)
      } else {
        it
      }
    }
  }
}
