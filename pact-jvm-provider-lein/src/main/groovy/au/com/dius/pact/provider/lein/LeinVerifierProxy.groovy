package au.com.dius.pact.provider.lein

import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import clojure.java.api.Clojure
import clojure.lang.IFn
import groovy.transform.Canonical

/**
 * Proxy to pass lein project information to the pact verifier
 */
@Canonical
class LeinVerifierProxy {

  private static final String LEIN_PACT_VERIFY_NAMESPACE = 'au.com.dius.pact.provider.lein.verify-provider'

  def project
  def args

  @Delegate ProviderVerifier verifier = new ProviderVerifier()

  private final IFn hasProperty = Clojure.var(LEIN_PACT_VERIFY_NAMESPACE, 'has-property?')
  private final IFn getProperty = Clojure.var(LEIN_PACT_VERIFY_NAMESPACE, 'get-property')

  def verifyProvider(ProviderInfo provider) {
    verifier.projectHasProperty = { property ->
      this.hasProperty.invoke(Clojure.read(":$property"), args)
    }
    verifier.projectGetProperty =  { property ->
      this.getProperty.invoke(Clojure.read(":$property"), args)
    }
    verifier.pactLoadFailureMessage = { consumer ->
      "You must specify the pactfile to execute for consumer '${consumer.name}' (use :pact-file)"
    }
    verifier.isBuildSpecificTask = { false }

    verifier.verifyProvider(provider)
  }

  Closure wrap(def fn) {
    return { args ->
      fn.invoke(args)
    }
  }

}
