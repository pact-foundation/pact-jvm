package au.com.dius.pact.provider.maven

import groovy.transform.Canonical

/**
 * Authentication for the pact broker, defaulting to Basic Authentication
 */
@Canonical
class PactBrokerAuth {
  String scheme = 'basic'
  String username
  String password
}
