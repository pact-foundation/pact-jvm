package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.pactbroker.VerificationNotice
import au.com.dius.pact.core.support.Auth
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.PactVerification
import javax.inject.Inject

open class GradleConsumerInfo(
  override var name: String,
  override var stateChange: Any? = null,
  override var stateChangeUsesBody: Boolean = false,
  override var packagesToScan: List<String> = emptyList(),
  override var verificationType: PactVerification? = null,
  override var pactSource: Any? = null,
  @Deprecated("Replaced with auth")
  override var pactFileAuthentication: List<Any?> = emptyList(),
  override val notices: List<VerificationNotice> = mutableListOf(),
  override val pending: Boolean = false,
  override val wip: Boolean = false,
  override val auth: Auth? = Auth.None
  ) : IConsumerInfo {
  @Inject
  constructor(name: String): this(name, null, false, emptyList(), null, null, emptyList())

  override fun toPactConsumer() = Consumer(name)

  override fun resolvePactSource() = ConsumerInfo.resolvePactSource(pactSource)
}
