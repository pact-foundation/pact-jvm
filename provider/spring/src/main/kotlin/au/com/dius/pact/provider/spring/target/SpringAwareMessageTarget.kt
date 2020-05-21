package au.com.dius.pact.provider.spring.target

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.junit.target.MessageTarget
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import java.util.function.Function

/**
 * Target for message verification that supports a spring application context. For each annotated method, the owning
 * bean will be looked up from the application context
 */
open class SpringAwareMessageTarget : MessageTarget(), BeanFactoryAware {
  private lateinit var beanFactory: BeanFactory

  override fun setBeanFactory(beanFactory: BeanFactory) {
    this.beanFactory = beanFactory
  }

  override fun setupVerifier(
    interaction: Interaction,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    pactSource: PactSource?
  ): IProviderVerifier {
    val verifier = super.setupVerifier(interaction, provider, consumer, pactSource)
    verifier.providerMethodInstance = Function { m -> beanFactory.getBean(m.declaringClass) }
    return verifier
  }
}
