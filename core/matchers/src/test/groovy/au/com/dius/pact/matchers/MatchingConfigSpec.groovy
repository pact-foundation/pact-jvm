package au.com.dius.pact.matchers

import spock.lang.Specification
import spock.lang.Unroll

class MatchingConfigSpec extends Specification {

  @Unroll
  def 'maps JSON content types to JSON body matcher'() {
    expect:
    MatchingConfig.lookupBodyMatcher(contentType).class.name == matcherClass

    where:
    contentType               | matcherClass
    'application/json'        | 'au.com.dius.pact.matchers.JsonBodyMatcher'
    'application/xml'         | 'au.com.dius.pact.matchers.XmlBodyMatcher'
    'application/hal+json'    | 'au.com.dius.pact.matchers.JsonBodyMatcher'
    'application/thrift+json' | 'au.com.dius.pact.matchers.JsonBodyMatcher'
    'application/stuff+xml'   | 'au.com.dius.pact.matchers.XmlBodyMatcher'
    'application/json-rpc'    | 'au.com.dius.pact.matchers.JsonBodyMatcher'
    'application/jsonrequest' | 'au.com.dius.pact.matchers.JsonBodyMatcher'
  }

}
