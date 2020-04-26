package au.com.dius.pact.consumer;

import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.generators.Generators;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup;
import au.com.dius.pact.core.model.matchingrules.MatchingRules;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class MatcherTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatcherTestUtils.class);

    private MatcherTestUtils() {}

    public static Set<String> asSet(String... strings) {
        return new TreeSet<>(Arrays.asList(strings));
    }

    public static void assertResponseMatcherKeysEqualTo(RequestResponsePact pact, String category, String... matcherKeys) {
      MatchingRules matchingRules = pact.getInteractions().get(0).getResponse().getMatchingRules();
      Map<String, MatchingRuleGroup> matchers = matchingRules.rulesForCategory(category).getMatchingRules();
      assertEquals(asSet(matcherKeys), new TreeSet<>(matchers.keySet()));
    }

    @SuppressWarnings("unchecked")
    public static void assertResponseGeneratorKeysEqualTo(RequestResponsePact pact, String category, String... matcherKeys) {
        Generators generators = pact.getInteractions().get(0).getResponse().getGenerators();
        Map<String, Object> categoryMap = (Map<String, Object>) generators.toMap(PactSpecVersion.V3).get(category);
        assertEquals(asSet(matcherKeys), new TreeSet<>(categoryMap.keySet()));
    }

    public static void assertResponseKeysEqualTo(RequestResponsePact pact, String... keys) {
      String body = pact.getInteractions().get(0).getResponse().getBody().valueAsString();
      Map hashMap = null;
      try {
        hashMap = new ObjectMapper().readValue(body, HashMap.class);
      } catch (IOException e) {
        LOGGER.error("Failed to parse JSON", e);
        Assert.fail(e.getMessage());
      }
      List<String> list = Arrays.asList(keys);
      Collections.sort(list);
      assertEquals(list, extractKeys(hashMap));
    }

    public static void assertMessageMatcherKeysEqualTo(MessagePact messagePact, String category, String... matcherKeys) {
      MatchingRules matchingRules = messagePact.getMessages().get(0).getMatchingRules();
      Map<String, MatchingRuleGroup> matchers = matchingRules.rulesForCategory(category).getMatchingRules();
      assertEquals(asSet(matcherKeys), new TreeSet<String>(matchers.keySet()));
    }

    private static List<String> extractKeys(Map hashMap) {
        List<String> list = new ArrayList<String>();
        walkGraph(hashMap, list, "/");
        Collections.sort(list);
        return list;
    }

    private static void walkGraph(Map hashMap, List<String> list, String path) {
        for (Object o : hashMap.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            list.add(path + e.getKey());
            if (e.getValue() instanceof Map) {
                walkGraph((Map) e.getValue(), list, path + e.getKey() + "/");
            } else if (e.getValue() instanceof List) {
                walkList((List) e.getValue(), list, path + e.getKey() + "/");
            }
        }
    }

    private static void walkList(List value, List<String> list, String path) {
        for (int i = 0; i < value.size(); i++) {
            Object v = value.get(i);
            if (v instanceof Map) {
                walkGraph((Map) v, list, path + i + "/");
            } else if (v instanceof List) {
                walkList((List) v, list, path + i + "/");
            } else {
                list.add(path + v);
            }
        }
    }
}
