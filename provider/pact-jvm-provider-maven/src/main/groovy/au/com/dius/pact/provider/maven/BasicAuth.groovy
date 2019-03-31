package au.com.dius.pact.provider.maven

import groovy.transform.Canonical

/**
 * Basic authentication for the pact broker
 */
@Canonical
class BasicAuth {
  String username
  String password
}
