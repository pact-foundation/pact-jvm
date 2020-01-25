package au.com.dius.pact.provider.gradle

import groovy.transform.ToString

/**
 * Config for pact broker
 */
@ToString
class Broker {
  String pactBrokerUrl
  String pactBrokerToken
  String pactBrokerUsername
  String pactBrokerPassword
  String pactBrokerAuthenticationScheme
}
