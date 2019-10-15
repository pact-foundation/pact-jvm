package au.com.dius.pact.provider.maven

import groovy.transform.Canonical

/**
 * Bean to configure a pact broker to query
 */
@Canonical
class PactBroker {
  URL url
  List<String> tags = []
  PactBrokerAuth authentication
  String serverId
}
