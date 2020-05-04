package au.com.dius.pact.provider.lein

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import clojure.java.api.Clojure
import clojure.lang.IFn
import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Proxy to pass lein project information to the pact verifier
 */
@Canonical
@CompileStatic
class LeinVerifierProxy {

  private static final String LEIN_PACT_VERIFY_NAMESPACE = 'au.com.dius.pact.provider.lein.verify-provider'

  def project
  def args

  @Delegate ProviderVerifier verifier = new ProviderVerifier()

  private final IFn hasProperty = Clojure.var(LEIN_PACT_VERIFY_NAMESPACE, 'has-property?')
  private final IFn getProperty = Clojure.var(LEIN_PACT_VERIFY_NAMESPACE, 'get-property')

  Map<String, Object> verifyProvider(ProviderInfo provider) {
    verifier.projectHasProperty = { property ->
      this.hasProperty.invoke(Clojure.read(":$property"), args)
    }
    verifier.projectGetProperty =  { property ->
      this.getProperty.invoke(Clojure.read(":$property"), args)
    }
    verifier.pactLoadFailureMessage = { ConsumerInfo consumer ->
      "You must specify the pactfile to execute for consumer '${consumer.name}' (use :pact-file)"
    }
    verifier.checkBuildSpecificTask = { false }

    verifier.verifyProvider(provider)
  }

  Closure wrap(IFn fn) {
    return { args -> fn.invoke(args) }
  }
}
