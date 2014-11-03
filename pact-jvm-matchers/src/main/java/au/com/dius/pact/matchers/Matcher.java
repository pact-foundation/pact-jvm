package au.com.dius.pact.matchers;

import java.util.List;
import java.util.Map;

public abstract class Matcher {
    public abstract <Mismatch> List<Mismatch> domatch(Map<String, ?> matcherDef, String path,
                                                          Object expected, Object actual,
                                                          MismatchFactory<Mismatch> mismatchFactory);

    public static String valueOf(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        } else if (value == null) {
            return "null";
        } else {
            return value.toString();
        }
    }
}
