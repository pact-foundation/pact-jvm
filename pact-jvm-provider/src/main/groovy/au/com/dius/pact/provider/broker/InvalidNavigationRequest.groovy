package au.com.dius.pact.provider.broker

import groovy.transform.InheritConstructors

/**
 * This exception is raised when an invalid navigation is attempted
 */
@InheritConstructors
class InvalidNavigationRequest extends RuntimeException {
}
