package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.junit.target.AmqpTarget
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import java.lang.reflect.Method
import java.util.function.Function

/**
 * Target for message verification that supports a spring application context. For each annotated method, the owning
 * bean will be looked up from the application context
 */
open class SpringAwareAmqpTarget : AmqpTarget(), BeanFactoryAware {
  private lateinit var beanFactory: BeanFactory

  override fun setBeanFactory(beanFactory: BeanFactory) {
    this.beanFactory = beanFactory
  }

  override fun setupVerifier(interaction: Interaction, provider: ProviderInfo, consumer: ConsumerInfo): ProviderVerifier {
    val verifier = super.setupVerifier(interaction, provider, consumer)
    verifier.providerMethodInstance = Function<Method, Any?> { m -> beanFactory.getBean(m.declaringClass) }
    return verifier
  }
}
