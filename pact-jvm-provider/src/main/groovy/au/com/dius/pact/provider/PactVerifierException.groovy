package au.com.dius.pact.provider

import groovy.transform.InheritConstructors

/**
 * Exception indicating failure to setup pact verification
 */
@InheritConstructors
class PactVerifierException extends RuntimeException {
}
