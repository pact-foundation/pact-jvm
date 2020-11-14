package au.com.dius.pact.provider.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * Extension object for pact plugin
 */
open class PactPluginExtension(
  val serviceProviders: NamedDomainObjectContainer<GradleProviderInfo>
) {
  var publish: PactPublish? = null
  var reports: VerificationReports? = null
  var broker: Broker? = null

  open fun serviceProviders(configureAction: Action<NamedDomainObjectContainer<GradleProviderInfo>>) {
    configureAction.execute(serviceProviders)
  }

  open fun publish(configureAction: Action<PactPublish>) {
    publish = PactPublish()
    configureAction.execute(publish!!)
  }

  open fun reports(configureAction: Action<VerificationReports>) {
    reports = VerificationReports()
    configureAction.execute(reports!!)
  }

  fun broker(configureAction: Action<Broker>) {
    broker = Broker()
    configureAction.execute(broker!!)
  }
}
