package au.com.dius.pact.core.support.expressions

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static ExpressionParser.VALUES_SEPARATOR

@SuppressWarnings('GStringExpressionWithinString')
class ExpressionParserSpec extends Specification {

  private ValueResolver valueResolver
  private ExpressionParser expressionParser

  def setup() {
    valueResolver = [
      resolveValue: { expression -> "[$expression]".toString() }
    ] as ValueResolver
    expressionParser = new ExpressionParser()
  }

  def 'Does Not Modify Strings With No Expressions'() {
    expect:
    expressionParser.parseExpression(null, DataType.RAW) == null
    expressionParser.parseExpression('', DataType.RAW) == ''
    expressionParser.parseExpression('hello world', DataType.RAW) == 'hello world'
    expressionParser.parseExpression('looks like a $', DataType.RAW) == 'looks like a $'
  }

  def 'Throws An Exception On Unterminated Expressions'() {
    when:
    expressionParser.parseExpression('${value', DataType.RAW)

    then:
    thrown(RuntimeException)
  }

  @Unroll
  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'Replaces The Expression With System Properties'() {
    expect:
    expressionParser.parseExpression(expression, DataType.RAW, valueResolver) == result

    where:

    expression             || result
    '${value}'             || '[value]'
    ' ${value}'            || ' [value]'
    '${value} '            || '[value] '
    ' ${value} '           || ' [value] '
    ' ${value} ${value2} ' || ' [value] [value2] '
    '$${value}}'           || '$[value]}'
  }

  def 'with overridden expression markers'() {
    given:
    expressionParser = new ExpressionParser('<<', '>>')

    expect:
    expressionParser.parseExpression('  <<value>>  ', DataType.RAW, valueResolver) == '  [value]  '
  }

  def 'Handles Empty Expression'() {
    expect:
    expressionParser.parseExpression('${}', DataType.RAW) == ''
    expressionParser.parseExpression('${} ${} ${}', DataType.RAW) == '  '
  }

  def 'Handles single value as list'() {
    when:
    def values = expressionParser.parseListExpression('${value}', valueResolver)

    then:
    values.size() == 1
    values.first() == '[value]'
  }

  def 'parseListExpression - Splits a compound expression value'() {
    given:
    List<String> expectedValues = ['one', 'two']
    ValueResolver valueResolver = [ resolveValue: { expectedValues.join(VALUES_SEPARATOR) } ] as ValueResolver

    when:
    def values = expressionParser.parseListExpression('${value}',  valueResolver)

    then:
    values == expectedValues
  }

  def 'parseListExpression - Splits several singular expression values'() {
    given:
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    List<String> expectedValues = ['one', 'two']

    when:
    def values = expressionParser.parseListExpression("\${one}$VALUES_SEPARATOR\${two}",  valueResolver)

    then:
    values == expectedValues
  }

  def 'parseListExpression - Ignores empty values during compound expression processing'() {
    given:
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    String expectedValue = 'one'

    when:
    def values = expressionParser.parseListExpression("\${one}$VALUES_SEPARATOR",  valueResolver)

    then:
    values == [expectedValue]
  }

  @Unroll
  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'with a defined type, converts the expression into the correct type'() {
    expect:
    expressionParser.parseExpression('${expression}', type, [
      resolveValue: { value.toString() }
    ] as ValueResolver) == result

    where:

    value    | type             || result
    'string' | DataType.RAW     || 'string'
    'string' | DataType.STRING  || 'string'
    '100'    | DataType.RAW     || '100'
    '100'    | DataType.STRING  || '100'
    '100'    | DataType.INTEGER || 100L
    '100'    | DataType.FLOAT   || 100.0f
    '100'    | DataType.DECIMAL || 100.0
    100      | DataType.RAW     || '100'
    100      | DataType.STRING  || '100'
    100      | DataType.INTEGER || 100L
    100      | DataType.FLOAT   || 100.0f
    100      | DataType.DECIMAL || 100.0
  }

  @Issue('#1262')
  def 'parseListExpression - trims whitespace from list items'() {
    given:
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    List<String> expectedValues = ['one', 'two']

    when:
    def values = expressionParser.parseListExpression("\${one}$VALUES_SEPARATOR \${two}",  valueResolver)

    then:
    values == expectedValues
  }

  @RestoreSystemProperties
  def 'supports overridden expression markers with sys prop'() {
    given:
    System.setProperty('pact.expressions.start', '<<')
    System.setProperty('pact.expressions.end', '>>')

    when:
    def value = expressionParser.parseExpression(' <<value>> ', DataType.RAW, valueResolver, true)

    then:
    value == ' [value] '
  }

  def 'toDefaultExpressions does nothing if the expression markers are not overridden'() {
    expect:
    expressionParser.toDefaultExpressions('${1} ${2} ${3}') == '${1} ${2} ${3}'
  }

  @RestoreSystemProperties
  def 'toDefaultExpressions restores the start marker if overridden with sys prop'() {
    given:
    System.setProperty('pact.expressions.start', '->')

    expect:
    expressionParser.toDefaultExpressions('->1} ${2} ->3}') == '${1} ${2} ${3}'
  }

  @RestoreSystemProperties
  def 'toDefaultExpressions restores the end marker if overridden with sys prop'() {
    given:
    System.setProperty('pact.expressions.end', '<-')

    expect:
    expressionParser.toDefaultExpressions('${1<- ${2} ${3<-') == '${1} ${2} ${3}'
  }

  @RestoreSystemProperties
  def 'toDefaultExpressions restores the markers if overridden with sys prop'() {
    given:
    System.setProperty('pact.expressions.start', '->')
    System.setProperty('pact.expressions.end', '<-')

    expect:
    expressionParser.toDefaultExpressions('->1<- ${2} ->3<-') == '${1} ${2} ${3}'
  }

  def 'correctExpressionMarkers does nothing if the expression markers are not overridden'() {
    expect:
    expressionParser.correctExpressionMarkers('${1} ${2} ${3}') == '${1} ${2} ${3}'
  }

  @RestoreSystemProperties
  def 'correctExpressionMarkers updates the start marker if overridden with sys prop'() {
    given:
    System.setProperty('pact.expressions.start', 'xx')

    expect:
    expressionParser.correctExpressionMarkers('${1} ${2} ${3}') == 'xx1} xx2} xx3}'
  }

  @RestoreSystemProperties
  def 'correctExpressionMarkers updates the end marker if overridden with sys prop'() {
    given:
    System.setProperty('pact.expressions.end', 'xx')

    expect:
    expressionParser.correctExpressionMarkers('${1} ${2} ${3}') == '${1xx ${2xx ${3xx'
  }

  @RestoreSystemProperties
  def 'correctExpressionMarkers updates the markers if overridden with sys prop'() {
    given:
    System.setProperty('pact.expressions.start', 'xx')
    System.setProperty('pact.expressions.end', 'yy')

    expect:
    expressionParser.correctExpressionMarkers('${1} ${2} ${3}') == 'xx1yy xx2yy xx3yy'
  }
}
