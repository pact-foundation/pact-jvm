package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.xml.PactXmlBuilder
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.v4.MessageContents
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.Matchers.fromProviderState
import static au.com.dius.pact.consumer.dsl.Matchers.regexp

class MessageContentsBuilderSpec extends Specification {

  MessageContentsBuilder builder

  def setup() {
    builder = new MessageContentsBuilder(new MessageContents())
  }

  def 'allows adding metadata to the message'() {
    when:
    def message = builder
      .withMetadata([x: 'y', y: ['a', 'b', 'c']])
      .build()

    then:
    message.metadata == [
      'x': 'y',
      'y': ['a', 'b', 'c']
    ]
  }

  def 'allows using matching rules with the metadata'() {
    when:
    def message = builder
      .withMetadata([x: regexp('\\d+', '111')])
      .build()

    then:
    message.metadata == [
      'x': '111'
    ]
    message.matchingRules.rulesForCategory('metadata') == new MatchingRuleCategory('metadata',
      [
        x: new MatchingRuleGroup([new RegexMatcher('\\d+', '111')])
      ]
    )
  }

  def 'supports setting metadata values from provider states'() {
    when:
    def message = builder
      .withMetadata(['A': fromProviderState('$a', '111')])
      .build()

    then:
    message.metadata == [
      'A': '111'
    ]
    message.matchingRules.rulesForCategory('metadata') == new MatchingRuleCategory('metadata', [:])
    message.generators.categoryFor(Category.METADATA) == [A: new ProviderStateGenerator('$a')]
  }

  def 'allows setting the contents of the message as a string value'() {
    when:
    def message = builder
      .withContent('This is some text')
      .build()

    then:
    message.contents.valueAsString() == 'This is some text'
    message.contents.contentType.toString() == 'text/plain; charset=ISO-8859-1'
    message.metadata['contentType'] == 'text/plain; charset=ISO-8859-1'
  }

  def 'allows setting the contents of the message as a string value with a given content type'() {
    when:
    def message = builder
      .withContent('This is some text', 'text/test-special')
      .build()

    then:
    message.contents.valueAsString() == 'This is some text'
    message.contents.contentType.toString() == 'text/test-special'
    message.metadata['contentType'] == 'text/test-special'
  }

  def 'when setting the body, tries to detect the content type from the body contents'() {
    when:
    def message = builder
      .withContent('{"value": "This is some text"}')
      .build()

    then:
    message.contents.valueAsString() == '{"value": "This is some text"}'
    message.contents.contentType.toString() == 'application/json'
    message.metadata['contentType'] == 'application/json'
  }

  def 'when setting the body, uses any existing content type metadata value'() {
    when:
    def message = builder
      .withMetadata(['contentType': 'text/plain'])
      .withContent('{"value": "This is some text"}')
      .build()

    then:
    message.contents.valueAsString() == '{"value": "This is some text"}'
    message.contents.contentType.toString() == 'text/plain'
    message.metadata['contentType'] == 'text/plain'
  }

  def 'when setting the body, overrides any existing content type header if the content type is given'() {
    when:
    def message = builder
      .withMetadata(['contentType': 'text/plain'])
      .withContent('{"value": "This is some text"}', 'application/json')
      .build()

    then:
    message.contents.valueAsString() == '{"value": "This is some text"}'
    message.contents.contentType.toString() == 'application/json'
    message.metadata['contentType'] == 'application/json'
  }

  def 'supports setting the body from a DSLPart object'() {
    when:
    def message = builder
      .withContent(new PactDslJsonBody().stringType('value', 'This is some text'))
      .build()

    then:
    message.contents.valueAsString() == '{"value":"This is some text"}'
    message.contents.contentType.toString() == 'application/json'
    message.metadata['contentType'] == 'application/json'
    message.matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body',
      [
        '$.value': new MatchingRuleGroup([au.com.dius.pact.core.model.matchingrules.TypeMatcher.INSTANCE])
      ]
    )
  }

  def 'supports setting the body using a body builder'() {
    when:
    def message = builder
      .withContent(new PactXmlBuilder('test').build {
        it.attributes = [id: regexp('\\d+', '100')]
      })
      .build()

    then:
    message.contents.valueAsString() == '<?xml version="1.0" encoding="UTF-8" standalone="no"?>' +
      System.lineSeparator() + '<test id="100"/>' + System.lineSeparator()
    message.contents.contentType.toString() == 'application/xml'
    message.metadata['contentType'] == 'application/xml'
    message.matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body',
      [
        '$.test[\'@id\']': new MatchingRuleGroup([new RegexMatcher('\\d+', '100')])
      ]
    )
  }

  def 'supports setting up a content type matcher on the body'() {
    given:
    def gif1px = [
      0107, 0111, 0106, 0070, 0067, 0141, 0001, 0000, 0001, 0000, 0200, 0000, 0000, 0377, 0377, 0377,
      0377, 0377, 0377, 0054, 0000, 0000, 0000, 0000, 0001, 0000, 0001, 0000, 0000, 0002, 0002, 0104,
      0001, 0000, 0073
    ] as byte[]

    when:
    def message = builder
      .withContentsMatchingContentType('image/gif', gif1px)
      .build()

    then:
    message.contents.value == gif1px
    message.contents.contentType.toString() == 'image/gif'
    message.metadata['contentType'] == 'image/gif'
    message.matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body',
      [
        '$': new MatchingRuleGroup([new ContentTypeMatcher('image/gif')])
      ]
    )
  }

  def 'allows setting the contents of the message as a byte array'() {
    given:
    def gif1px = [
      0107, 0111, 0106, 0070, 0067, 0141, 0001, 0000, 0001, 0000, 0200, 0000, 0000, 0377, 0377, 0377,
      0377, 0377, 0377, 0054, 0000, 0000, 0000, 0000, 0001, 0000, 0001, 0000, 0000, 0002, 0002, 0104,
      0001, 0000, 0073
    ] as byte[]

    when:
    def message = builder
      .withContent(gif1px)
      .build()

    then:
    message.contents.unwrap() == gif1px
    message.contents.contentType.toString() == 'application/octet-stream'
    message.metadata['contentType'] == 'application/octet-stream'
  }

  def 'allows setting the contents of the message as a a byte array with a content type'() {
    given:
    def gif1px = [
      0107, 0111, 0106, 0070, 0067, 0141, 0001, 0000, 0001, 0000, 0200, 0000, 0000, 0377, 0377, 0377,
      0377, 0377, 0377, 0054, 0000, 0000, 0000, 0000, 0001, 0000, 0001, 0000, 0000, 0002, 0002, 0104,
      0001, 0000, 0073
    ] as byte[]

    when:
    def message = builder
      .withContent(gif1px, 'image/gif')
      .build()

    then:
    message.contents.unwrap() == gif1px
    message.contents.contentType.toString() == 'image/gif'
    message.metadata['contentType'] == 'image/gif'
  }
}
