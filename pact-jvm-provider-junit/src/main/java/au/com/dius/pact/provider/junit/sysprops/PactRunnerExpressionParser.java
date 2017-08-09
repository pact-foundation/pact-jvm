package au.com.dius.pact.provider.junit.sysprops;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

public class PactRunnerExpressionParser {

  public static final String VALUES_SEPARATOR = ",";
  public static final String START_EXPRESSION = "${";
  public static final char END_EXPRESSION = '}';

  private PactRunnerExpressionParser() {}

  public static String parseExpression(final String value) {
    return parseExpression(value, new SystemPropertyResolver());
  }

  public static List<String> parseListExpression(final String value) {
    return parseListExpression(value, new SystemPropertyResolver());
  }

  public static List<String> parseListExpression(final String value, final ValueResolver valueResolver) {
    String[] values = replaceExpressions(value, valueResolver).split(VALUES_SEPARATOR);
    List<String> result =new ArrayList<>();
    for (String str: values) {
      if (!isNullOrEmpty(str)) {
        result.add(str);
      }
    }
    return result;
  }

  public static String parseExpression(final String value, final ValueResolver valueResolver) {
    if (value.contains(START_EXPRESSION)) {
      return replaceExpressions(value, valueResolver);
    }
    return value;
  }

  private static String replaceExpressions(final String value, final ValueResolver valueResolver) {
    StringBuilder joiner = new StringBuilder();

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
