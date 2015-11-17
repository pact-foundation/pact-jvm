package au.com.dius.pact.provider.sbtsupport

import groovy.transform.Canonical

/**
 * Address Configuration for SBT plugin
 */
@Canonical
class Address {
  String host
  Integer port
  String path = ''
  String protocol = 'http'

  String url() {
    "$protocol://$host:$port$path"
  }
}
