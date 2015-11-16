package au.com.dius.pact.provider.configuration

import groovy.transform.Canonical

/**
 * Address Configuration
 */
@Canonical
class Address {
  String protocol
  String host
  Integer port
  String path = ''

  String url() {
    "$protocol$host:$port$path"
  }
}
