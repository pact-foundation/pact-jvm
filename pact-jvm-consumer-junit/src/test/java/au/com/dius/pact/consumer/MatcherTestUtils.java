package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;
import org.junit.Assert;
import scala.collection.JavaConversions$;
import scala.collection.immutable.Map;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class MatcherTestUtils {

    public static Set<String> asSet(String... strings) {
        return new TreeSet<String>(Arrays.asList(strings));
    }

    public static void assertResponseMatcherKeysEqualTo(PactFragment fragment, String... matcherKeys) {
        Map<String, Map<String, String>> scalaMap = fragment.interactions().head().response().matchers().get();
        java.util.Map<String, Map<String, String>> matchers = JavaConversions$.MODULE$.mapAsJavaMap(scalaMap);
        Assert.assertEquals(asSet(matcherKeys), new TreeSet<String>(matchers.keySet()));
    }
}
