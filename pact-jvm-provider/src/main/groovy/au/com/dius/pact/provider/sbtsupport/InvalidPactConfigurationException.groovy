package au.com.dius.pact.provider.sbtsupport

import groovy.transform.InheritConstructors

/**
 * Exception to indicate the pact config JSON was invalid
 */
@InheritConstructors
class InvalidPactConfigurationException extends RuntimeException {
}
