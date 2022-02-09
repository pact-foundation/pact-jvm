package au.com.dius.pact.provider

import au.com.dius.pact.core.support.Auth
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class PactBrokerOptionsSpec extends Specification {

  @Unroll
  def 'parseAuthSettings'() {
    expect:
    PactBrokerOptions.parseAuthSettings(options) == result

    where:

    options                                                | result
    [:]                                                    | null
    [authentication: new Auth.BearerAuthentication('123')] | new Auth.BearerAuthentication('123')
    [authentication: ['basic', 'bob']]                     | new Auth.BasicAuthentication('bob', '')
    [authentication: ['BASIC', 'bob']]                     | new Auth.BasicAuthentication('bob', '')
    [authentication: ['BASIC', null]]                      | new Auth.BasicAuthentication('', '')
    [authentication: ['basic', 'bob', '1234']]             | new Auth.BasicAuthentication('bob', '1234')
    [authentication: ['bearer', '1234']]                   | new Auth.BearerAuthentication('1234')
    [authentication: ['Bearer', '1234']]                   | new Auth.BearerAuthentication('1234')
  }

  @Unroll
  def 'throws an exception with the incorrect class'() {
    when:
    PactBrokerOptions.parseAuthSettings(options)

    then:
    def ex = thrown(RuntimeException)
    ex.message == message

    where:

    options                            | message
    [authentication: 100]              | "Authentication options needs to be a Auth class or a list of values, got '100'"
    [authentication: []]               | "Authentication options must be a list of values with the first value being the scheme, got '[]'"
    [authentication: ['X509']]         | "Authentication options must be a list of values with the first value being the scheme, got '[X509]'"
    [authentication: ['X509', 'cert']] | "'X509' ia not a valid authentication scheme. Only basic or bearer is supported"
  }
}
