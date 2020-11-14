package au.com.dius.pact.provider.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * Extension object for pact plugin
 */
@CompileStatic
class PactPluginExtension {

    final NamedDomainObjectContainer<GradleProviderInfo> serviceProviders

    PactPublish publish
    VerificationReports reports
    Broker broker

    PactPluginExtension(NamedDomainObjectContainer<GradleProviderInfo> serviceProviders) {
        this.serviceProviders = serviceProviders
    }

    @SuppressWarnings('ConfusingMethodName')
    void serviceProviders(Action<? extends NamedDomainObjectContainer<GradleProviderInfo>> configureAction) {
        configureAction.execute(serviceProviders)
    }

    @SuppressWarnings('ConfusingMethodName')
    void publish(Action<? extends PactPublish> configureAction) {
        publish = new PactPublish()
        configureAction.execute(publish)
    }

    @SuppressWarnings('ConfusingMethodName')
    void reports(Action<? extends VerificationReports> configureAction) {
        reports = new VerificationReports()
        configureAction.execute(reports)
    }

    @SuppressWarnings('ConfusingMethodName')
    void broker(Action<? extends Broker> configureAction) {
      broker = new Broker()
      configureAction.execute(broker)
    }
}
