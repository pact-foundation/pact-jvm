package au.com.dius.pact.provider.broker

import groovy.transform.InheritConstructors

/**
 * Exception to indicate the response from the pact broker was invalid
 */
@InheritConstructors
class InvalidHalResponse extends RuntimeException {
}
