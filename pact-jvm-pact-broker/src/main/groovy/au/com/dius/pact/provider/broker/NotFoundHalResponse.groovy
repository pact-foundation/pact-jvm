package au.com.dius.pact.provider.broker

import groovy.transform.InheritConstructors

/**
 * Exception to indicate the response from the pact broker resulted in Not Found
 */
@InheritConstructors
class NotFoundHalResponse extends InvalidHalResponse {
}
