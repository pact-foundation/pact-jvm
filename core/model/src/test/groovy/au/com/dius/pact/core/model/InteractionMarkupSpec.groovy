package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class InteractionMarkupSpec extends Specification {

  def 'isNotEmpty returns false for a default InteractionMarkup'() {
    expect:
    !new InteractionMarkup().notEmpty
  }

  def 'isNotEmpty returns true when markup is set'() {
    expect:
    new InteractionMarkup('# Heading', 'commonmark').notEmpty
  }

  def 'toMap returns the correct map representation'() {
    given:
    def markup = new InteractionMarkup('# Heading', 'commonmark')

    when:
    def result = markup.toMap()

    then:
    result == [markup: '# Heading', markupType: 'commonmark']
  }

  def 'merge returns other when this markup is empty'() {
    given:
    def empty = new InteractionMarkup()
    def other = new InteractionMarkup('other content', 'commonmark')

    when:
    def result = empty.merge(other)

    then:
    result == other
  }

  def 'merge returns this when other markup is empty'() {
    given:
    def thisMarkup = new InteractionMarkup('this content', 'commonmark')

    when:
    def result = thisMarkup.merge(new InteractionMarkup())

    then:
    result == thisMarkup
  }

  def 'merge concatenates both markups when both are non-empty'() {
    given:
    def first = new InteractionMarkup('line 1', 'commonmark')
    def second = new InteractionMarkup('line 2', 'commonmark')

    when:
    def result = first.merge(second)

    then:
    result.markup == 'line 1\nline 2'
    result.markupType == 'commonmark'
  }

  def 'merge retains this markupType when merging different markup types'() {
    given:
    def first = new InteractionMarkup('line 1', 'commonmark')
    def second = new InteractionMarkup('line 2', 'html')

    when:
    def result = first.merge(second)

    then:
    result.markup == 'line 1\nline 2'
    result.markupType == 'commonmark'
  }

  def 'fromJson creates an InteractionMarkup from a valid JSON object'() {
    given:
    def json = new JsonValue.Object([
      markup: new JsonValue.StringValue('# Heading'),
      markupType: new JsonValue.StringValue('commonmark')
    ])

    when:
    def result = InteractionMarkup.fromJson(json)

    then:
    result.markup == '# Heading'
    result.markupType == 'commonmark'
  }

  def 'fromJson returns a default InteractionMarkup for non-object JSON values'() {
    expect:
    InteractionMarkup.fromJson(JsonValue.Null.INSTANCE) == new InteractionMarkup()
    InteractionMarkup.fromJson(new JsonValue.StringValue('invalid')) == new InteractionMarkup()
    InteractionMarkup.fromJson(new JsonValue.Array()) == new InteractionMarkup()
  }
}
