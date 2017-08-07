package au.com.dius.pact.provider.junit.sysprops

import org.junit.Test

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.VALUES_SEPARATOR
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize

@SuppressWarnings('GStringExpressionWithinString')
class PactRunnerExpressionParserTest {

  private final ValueResolver valueResolver = [
    resolveValue: { expression -> '[' + expression + ']' }
  ] as ValueResolver

  @Test
  void 'Does Not Modify Strings With No Expressions'() {
    assertThat(PactRunnerExpressionParser.parseExpression(''), is(equalTo('')))
    assertThat(PactRunnerExpressionParser.parseExpression('hello world'), is(equalTo('hello world')))
    assertThat(PactRunnerExpressionParser.parseExpression('looks like a $'), is(equalTo('looks like a $')))
  }

  @Test(expected = RuntimeException)
  void 'Throws An Exception On Unterminated Expressions'() {
    PactRunnerExpressionParser.parseExpression('${value')
  }

  @Test
  void 'Replaces The Expression With System Properties'() {
    assertThat(PactRunnerExpressionParser.parseExpression('${value}', valueResolver), is(equalTo('[value]')))
    assertThat(PactRunnerExpressionParser.parseExpression(' ${value}', valueResolver), is(equalTo(' [value]')))
    assertThat(PactRunnerExpressionParser.parseExpression('${value} ', valueResolver), is(equalTo('[value] ')))
    assertThat(PactRunnerExpressionParser.parseExpression(' ${value} ', valueResolver), is(equalTo(' [value] ')))
    assertThat(PactRunnerExpressionParser.parseExpression(' ${value} ${value2} ', valueResolver),
      is(equalTo(' [value] [value2] ')))
    assertThat(PactRunnerExpressionParser.parseExpression('$${value}}', valueResolver), is(equalTo('$[value]}')))
  }

  @Test
  void 'Handles Empty Expression'() {
    assertThat(PactRunnerExpressionParser.parseExpression('${}'), is(equalTo('')))
    assertThat(PactRunnerExpressionParser.parseExpression('${} ${} ${}'), is(equalTo('  ')))
  }

  @Test
  void 'Handles single value as list'() {
    def values = PactRunnerExpressionParser.parseListExpression('${value}', valueResolver)
    assertThat(values, hasSize(1))
    assertThat(values.first(), is(equalTo('[value]')))
  }

  @Test
  void 'Splits a compound expression value'() {
    List<String> expectedValues = ['one', 'two']
    ValueResolver valueResolver = [ resolveValue: { expectedValues.join(VALUES_SEPARATOR) } ] as ValueResolver
    def values = PactRunnerExpressionParser.parseListExpression('${value}',  valueResolver)
    assertThat(values, hasSize(expectedValues.size()))
    assertThat(values, containsInAnyOrder(expectedValues.toArray()))
  }

  @Test
  void 'Splits several singular expression values'() {
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    def values = PactRunnerExpressionParser.parseListExpression("\${one}$VALUES_SEPARATOR\${two}",  valueResolver)
    List<String> expectedValues = ['one', 'two']
    assertThat(values, hasSize(expectedValues.size()))
    assertThat(values, containsInAnyOrder(expectedValues.toArray()))
  }

  @Test
  void 'Ignores empty values during compound expression processing'() {
    ValueResolver valueResolver = [ resolveValue: { it } ] as ValueResolver
    def values = PactRunnerExpressionParser.parseListExpression("\${one}$VALUES_SEPARATOR",  valueResolver)
    String expectedValue = 'one'
    assertThat(values, hasSize(1))
    assertThat(values.first(), is(equalTo(expectedValue)))
  }
}
