package au.com.dius.pact.core.matchers

import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
class MatchingConfigSpec extends Specification {

  def 'maps JSON content types to JSON body matcher'() {
    expect:
    MatchingConfig.lookupContentMatcher(contentType).class.name == matcherClass

    where:
    contentType                              | matcherClass
    'application/vnd.schemaregistry.v1+json' | 'au.com.dius.pact.core.matchers.KafkaJsonSchemaContentMatcher'
    'application/xml'                        | 'au.com.dius.pact.core.matchers.XmlContentMatcher'
    'application/hal+json'                   | 'au.com.dius.pact.core.matchers.JsonContentMatcher'
    'application/thrift+json'                | 'au.com.dius.pact.core.matchers.JsonContentMatcher'
    'application/stuff+xml'                  | 'au.com.dius.pact.core.matchers.XmlContentMatcher'
    'application/json-rpc'                   | 'au.com.dius.pact.core.matchers.JsonContentMatcher'
    'application/jsonrequest'                | 'au.com.dius.pact.core.matchers.JsonContentMatcher'
  }

  def 'allows content type matchers to be overridden'() {
    given:
    System.setProperty('pact.content_type.override.application/x-thrift', 'json')
    System.setProperty('pact.content_type.override.application/x-other', 'text')
    System.setProperty('pact.content_type.override.text/plain', 'application/xml')

    expect:
    MatchingConfig.lookupContentMatcher(contentType).class.name == matcherClass

    where:
    contentType                              | matcherClass
    'application/x-thrift'                   | 'au.com.dius.pact.core.matchers.JsonContentMatcher'
    'application/x-other'                    | 'au.com.dius.pact.core.matchers.PlainTextContentMatcher'
    'text/plain'                             | 'au.com.dius.pact.core.matchers.XmlContentMatcher'
  }
}
