package au.com.dius.pact.provider.junit.sysprops;

import java.lang.StringBuffer;

public class PactRunnerExpressionParser {

  public static final String START_EXPRESSION = "${";
  public static final char END_EXPRESSION = '}';

  public static String parseExpressions(final String value) {
    return parseExpressions(value, new SystemPropertyResolver());
  }

  public static String parseExpressions(final String value, final ValueResolver valueResolver) {
    if (value.contains(START_EXPRESSION)) {
      return replaceExpressions(value, valueResolver);
    }
    return value;
  }

  private static String replaceExpressions(final String value, final ValueResolver valueResolver) {
    StringBuffer joiner = new StringBuffer();

    String buffer = value;
    int position = buffer.indexOf(START_EXPRESSION);
    while (position >= 0) {
      if (position > 0) {
        joiner.append(buffer.substring(0, position));
      }
      int endPosition = buffer.indexOf(END_EXPRESSION, position);
      if (endPosition < 0) {
        throw new RuntimeException("Missing closing brace in expression string \"" + value + "]\"");
      }
      String expression = "";
      if (endPosition - position > 2) {
        expression = valueResolver.resolveValue(buffer.substring(position + 2, endPosition));
      }
      joiner.append(expression);
      buffer = buffer.substring(endPosition + 1);
      position = buffer.indexOf(START_EXPRESSION);
    }
    joiner.append(buffer);

    return joiner.toString();
  }

}
