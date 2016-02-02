package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser;
import au.com.dius.pact.provider.junit.sysprops.ValueResolver;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PactRunnerExpressionParserTest {

  private ValueResolver valueResolver = expression -> "[" + expression + "]";

  @Test
  public void doesNotModifyStringsWithNoExpressions() {
    assertThat(PactRunnerExpressionParser.parseExpressions(""), is(equalTo("")));
    assertThat(PactRunnerExpressionParser.parseExpressions("hello world"), is(equalTo("hello world")));
    assertThat(PactRunnerExpressionParser.parseExpressions("looks like a $"), is(equalTo("looks like a $")));
  }

  @Test(expected = RuntimeException.class)
  public void throwsAnExceptionOnUnterminatedExpressions() {
    PactRunnerExpressionParser.parseExpressions("${value");
  }

  @Test
  public void replacesTheExpressionWithSystemProperties() {
    assertThat(PactRunnerExpressionParser.parseExpressions("${value}", valueResolver), is(equalTo("[value]")));
    assertThat(PactRunnerExpressionParser.parseExpressions(" ${value}", valueResolver), is(equalTo(" [value]")));
    assertThat(PactRunnerExpressionParser.parseExpressions("${value} ", valueResolver), is(equalTo("[value] ")));
    assertThat(PactRunnerExpressionParser.parseExpressions(" ${value} ", valueResolver), is(equalTo(" [value] ")));
    assertThat(PactRunnerExpressionParser.parseExpressions(" ${value} ${value2} ", valueResolver), is(equalTo(" [value] [value2] ")));
    assertThat(PactRunnerExpressionParser.parseExpressions("$${value}}", valueResolver), is(equalTo("$[value]}")));
  }

  @Test
  public void handlesEmptyExpression() {
    assertThat(PactRunnerExpressionParser.parseExpressions("${}"), is(equalTo("")));
    assertThat(PactRunnerExpressionParser.parseExpressions("${} ${} ${}"), is(equalTo("  ")));
  }

}
