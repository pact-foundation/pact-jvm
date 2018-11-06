package au.com.dius.pact.core.support.expressions

import org.junit.Test

import static ExpressionParser.VALUES_SEPARATOR
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.nullValue

@SuppressWarnings('GStringExpressionWithinString')
class ExpressionParserTest {

  private final ValueResolver valueResolver = [
    resolveValue: { expression -> "[$expression]".toString() }
  ] as ValueResolver

  @Test
  void 'Does Not Modify Strings With No Expressions'() {
    assertThat(ExpressionParser.parseExpression(null), is(nullValue()))
    assertThat(ExpressionParser.parseExpression(''), is(equalTo('')))
    assertThat(ExpressionParser.parseExpression('hello world'), is(equalTo('hello world')))
    assertThat(ExpressionParser.parseExpression('looks like a $'), is(equalTo('looks like a $')))
  }

  @Test(expected = RuntimeException)
  void 'Throws An Exception On Unterminated Expressions'() {
    ExpressionParser.parseExpression('${value')
  }

  @Test
  void 'Replaces The Expression With System Properties'() {
    assertThat(ExpressionParser.parseExpression('${value}', valueResolver), is(equalTo('[value]')))
    assertThat(ExpressionParser.parseExpression(' ${value}', valueResolver), is(equalTo(' [value]')))
    assertThat(ExpressionParser.parseExpression('${value} ', valueResolver), is(equalTo('[value] ')))
    assertThat(ExpressionParser.parseExpression(' ${value} ', valueResolver), is(equalTo(' [value] ')))
    assertThat(ExpressionParser.parseExpression(' ${value} ${value2} ', valueResolver),
      is(equalTo(' [value] [value2] ')))
    assertThat(ExpressionParser.parseExpression('$${value}}', valueResolver), is(equalTo('$[value]}')))
  }

  @Test
  void 'Handles Empty Expression'() {
    assertThat(ExpressionParser.parseExpression('${}'), is(equalTo('')))
    assertThat(ExpressionParser.parseExpression('${} ${} ${}'), is(equalTo('  ')))
  }

  @Test
  void 'Handles single value as list'() {
    def values = ExpressionParser.parseListExpression('${value}', valueResolver)
    assertThat(values, hasSize(1))
    assertThat(values.first(), is(equalTo('[value]')))
  }

  @Test
  void 'Splits a compound expression value'() {
    List<String> expectedValues = ['one', 'two']
    ValueResolver valueResolver = [ resolveValue: { expectedValues.join(VALUES_SEPARATOR) } ] as ValueResolver
    def values = ExpressionParser.parseListExpression('${value}',  valueResolver)
    assertThat(values, hasSize(expectedValues.size()))
    assertThat(values, containsInAnyOrder(expectedValues.toArray()))
  }

  @Test
  void 'Splits several singular expression values'() {
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    def values = ExpressionParser.parseListExpression("\${one}$VALUES_SEPARATOR\${two}",  valueResolver)
    List<String> expectedValues = ['one', 'two']
    assertThat(values, hasSize(expectedValues.size()))
    assertThat(values, containsInAnyOrder(expectedValues.toArray()))
  }

  @Test
  void 'Ignores empty values during compound expression processing'() {
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    def values = ExpressionParser.parseListExpression("\${one}$VALUES_SEPARATOR",  valueResolver)
    String expectedValue = 'one'
    assertThat(values, hasSize(1))
    assertThat(values.first(), is(equalTo(expectedValue)))
  }
}
