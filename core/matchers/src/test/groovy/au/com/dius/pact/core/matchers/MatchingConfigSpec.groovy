package au.com.dius.pact.core.matchers

import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
class MatchingConfigSpec extends Specification {

  def setupSpec() {
    System.setProperty('pact.content_type.override.application/x-thrift', 'json')
    System.setProperty('pact.content_type.override.application/x-other', 'text')
  }

  @Unroll
  def 'maps JSON content types to JSON body matcher'() {
    expect:
    MatchingConfig.lookupContentMatcher(contentType).class.name == matcherClass

    where:
    contentType               | matcherClass
    'application/json'        | 'au.com.dius.pact.core.matchers.JsonBodyMatcher'
    'application/xml'         | 'au.com.dius.pact.core.matchers.XmlBodyMatcher'
    'application/hal+json'    | 'au.com.dius.pact.core.matchers.JsonBodyMatcher'
    'application/thrift+json' | 'au.com.dius.pact.core.matchers.JsonBodyMatcher'
    'application/stuff+xml'   | 'au.com.dius.pact.core.matchers.XmlBodyMatcher'
    'application/json-rpc'    | 'au.com.dius.pact.core.matchers.JsonBodyMatcher'
    'application/jsonrequest' | 'au.com.dius.pact.core.matchers.JsonBodyMatcher'
    'application/x-thrift'    | 'au.com.dius.pact.core.matchers.JsonBodyMatcher'
    'application/x-other'     | 'au.com.dius.pact.core.matchers.PlainTextBodyMatcher'
  }
}
