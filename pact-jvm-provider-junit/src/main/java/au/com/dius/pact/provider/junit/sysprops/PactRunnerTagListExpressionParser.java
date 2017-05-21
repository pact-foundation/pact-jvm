package au.com.dius.pact.provider.junit.sysprops;

import com.google.common.base.Strings;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static au.com.dius.pact.provider.junit.sysprops.PactRunnerExpressionParser.START_EXPRESSION;

public class PactRunnerTagListExpressionParser {


    public static List<String> parseTagListExpressions(final List<String> values) {
        return parseTagListExpressions(values, new SystemPropertyResolver());
    }

    public static List<String> parseTagListExpressions(final List<String> values, ValueResolver valueResolver) {
        List<String> list = new ArrayList<>();
        for (String value: values) {
          list.addAll(substituteIfExpression(value, valueResolver));
        }
        return list;
    }

    private static List<String> substituteIfExpression(String value, ValueResolver valueResolver) {
        if (value.contains(START_EXPRESSION)) {
            String[] split = valueResolver.resolveValue(value).split(",");
            return ListUtils.select(Arrays.asList(split), new Predicate<String>() {
              @Override
              public boolean evaluate(String str) {
                return ! Strings.isNullOrEmpty(str);
              }
            });
        }
        return Collections.singletonList(value);

    }
}
