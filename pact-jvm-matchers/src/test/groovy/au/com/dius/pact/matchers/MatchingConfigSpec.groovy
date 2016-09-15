package au.com.dius.pact.matchers

import spock.lang.Specification
import spock.lang.Unroll

class MatchingConfigSpec extends Specification {

  @Unroll
  def 'maps JSON content types to JSON body matcher'() {
    expect:
    MatchingConfig.lookupBodyMatcher(contentType).get()._2().class.simpleName == matcherClass

    where:
    contentType               | matcherClass
    'application/json'        | 'JsonBodyMatcher'
    'application/xml'         | 'XmlBodyMatcher'
    'application/hal+json'    | 'JsonBodyMatcher'
    'application/thrift+json' | 'JsonBodyMatcher'
    'application/stuff+xml'   | 'XmlBodyMatcher'
    'application/json-rpc'    | 'JsonBodyMatcher'
    'application/jsonrequest' | 'JsonBodyMatcher'
  }

}
