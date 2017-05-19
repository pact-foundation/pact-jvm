package au.com.dius.pact.provider.junit.sysprops;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.START_EXPRESSION;

public class PactRunnerTagListExpressionParser {


    public static List<String> parseTagListExpressions(final List<String> values) {
        return parseTagListExpressions(values, new SystemPropertyResolver());
    }

    public static List<String> parseTagListExpressions(final List<String> values, ValueResolver valueResolver) {
        return values.stream()
                .flatMap(value -> substituteIfExpression(value, valueResolver))
                .collect(Collectors.toList());
    }

    private static Stream<String> substituteIfExpression(String value, ValueResolver valueResolver) {
        if (value.contains(START_EXPRESSION)) {
            String[] split = valueResolver.resolveValue(value).split(",");
            return Arrays.stream(split)
                    .filter(str -> ! Strings.isNullOrEmpty(str));
        }
        return Stream.of(value);

    }
}
