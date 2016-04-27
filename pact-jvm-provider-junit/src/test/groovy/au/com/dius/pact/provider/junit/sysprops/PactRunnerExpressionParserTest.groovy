package au.com.dius.pact.provider.junit.sysprops

import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat

@SuppressWarnings('GStringExpressionWithinString')
class PactRunnerExpressionParserTest {

  private final ValueResolver valueResolver = [
    resolveValue: { expression -> '[' + expression + ']' }
  ] as ValueResolver

  @Test
  void 'Does Not Modify Strings With No Expressions'() {
    assertThat(PactRunnerExpressionParser.parseExpressions(''), is(equalTo('')))
    assertThat(PactRunnerExpressionParser.parseExpressions('hello world'), is(equalTo('hello world')))
    assertThat(PactRunnerExpressionParser.parseExpressions('looks like a $'), is(equalTo('looks like a $')))
  }

  @Test(expected = RuntimeException)
  void 'Throws An Exception On Unterminated Expressions'() {
    PactRunnerExpressionParser.parseExpressions('${value')
  }

  @Test
  void 'Replaces The Expression With System Properties'() {
    assertThat(PactRunnerExpressionParser.parseExpressions('${value}', valueResolver), is(equalTo('[value]')))
    assertThat(PactRunnerExpressionParser.parseExpressions(' ${value}', valueResolver), is(equalTo(' [value]')))
    assertThat(PactRunnerExpressionParser.parseExpressions('${value} ', valueResolver), is(equalTo('[value] ')))
    assertThat(PactRunnerExpressionParser.parseExpressions(' ${value} ', valueResolver), is(equalTo(' [value] ')))
    assertThat(PactRunnerExpressionParser.parseExpressions(' ${value} ${value2} ', valueResolver),
      is(equalTo(' [value] [value2] ')))
    assertThat(PactRunnerExpressionParser.parseExpressions('$${value}}', valueResolver), is(equalTo('$[value]}')))
  }

  @Test
  void 'Handles Empty Expression'() {
    assertThat(PactRunnerExpressionParser.parseExpressions('${}'), is(equalTo('')))
    assertThat(PactRunnerExpressionParser.parseExpressions('${} ${} ${}'), is(equalTo('  ')))
  }

}
