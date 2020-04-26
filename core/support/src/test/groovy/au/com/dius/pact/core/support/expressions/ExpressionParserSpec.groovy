package au.com.dius.pact.core.support.expressions

import spock.lang.Specification
import spock.lang.Unroll

import static ExpressionParser.VALUES_SEPARATOR

@SuppressWarnings('GStringExpressionWithinString')
class ExpressionParserSpec extends Specification {

  private ValueResolver valueResolver

  def setup() {
    valueResolver = [
      resolveValue: { expression -> "[$expression]".toString() }
    ] as ValueResolver
  }

  def 'Does Not Modify Strings With No Expressions'() {
    expect:
    ExpressionParser.parseExpression(null, DataType.RAW) == null
    ExpressionParser.parseExpression('', DataType.RAW) == ''
    ExpressionParser.parseExpression('hello world', DataType.RAW) == 'hello world'
    ExpressionParser.parseExpression('looks like a $', DataType.RAW) == 'looks like a $'
  }

  def 'Throws An Exception On Unterminated Expressions'() {
    when:
    ExpressionParser.parseExpression('${value', DataType.RAW)

    then:
    thrown(RuntimeException)
  }

  @Unroll
  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'Replaces The Expression With System Properties'() {
    expect:
    ExpressionParser.parseExpression(expression, DataType.RAW, valueResolver) == result

    where:

    expression             || result
    '${value}'             || '[value]'
    ' ${value}'            || ' [value]'
    '${value} '            || '[value] '
    ' ${value} '           || ' [value] '
    ' ${value} ${value2} ' || ' [value] [value2] '
    '$${value}}'           || '$[value]}'
  }

  def 'Handles Empty Expression'() {
    expect:
    ExpressionParser.parseExpression('${}', DataType.RAW) == ''
    ExpressionParser.parseExpression('${} ${} ${}', DataType.RAW) == '  '
  }

  def 'Handles single value as list'() {
    when:
    def values = ExpressionParser.parseListExpression('${value}', valueResolver)

    then:
    values.size() == 1
    values.first() == '[value]'
  }

  def 'parseListExpression - Splits a compound expression value'() {
    given:
    List<String> expectedValues = ['one', 'two']
    ValueResolver valueResolver = [ resolveValue: { expectedValues.join(VALUES_SEPARATOR) } ] as ValueResolver

    when:
    def values = ExpressionParser.parseListExpression('${value}',  valueResolver)

    then:
    values == expectedValues
  }

  def 'parseListExpression - Splits several singular expression values'() {
    given:
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    List<String> expectedValues = ['one', 'two']

    when:
    def values = ExpressionParser.parseListExpression("\${one}$VALUES_SEPARATOR\${two}",  valueResolver)

    then:
    values == expectedValues
  }

  def 'parseListExpression - Ignores empty values during compound expression processing'() {
    given:
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    String expectedValue = 'one'

    when:
    def values = ExpressionParser.parseListExpression("\${one}$VALUES_SEPARATOR",  valueResolver)

    then:
    values == [expectedValue]
  }

  @Unroll
  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'with a defined type, converts the expression into the correct type'() {
    expect:
    ExpressionParser.parseExpression('${expression}', type, [
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
}
