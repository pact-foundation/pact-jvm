package au.com.dius.pact.model

import groovy.transform.InheritConstructors

/**
 * Exception class to indicate an invalid pact specification
 */
@InheritConstructors
class InvalidPactException extends RuntimeException {
}
