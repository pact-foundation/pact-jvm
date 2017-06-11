package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.Category
import au.com.dius.pact.model.matchingrules.DateMatcher
import au.com.dius.pact.model.matchingrules.IncludeMatcher
import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.model.matchingrules.RegexMatcher
import au.com.dius.pact.model.matchingrules.RuleLogic
import au.com.dius.pact.model.matchingrules.TimeMatcher
import au.com.dius.pact.model.matchingrules.TimestampMatcher
import au.com.dius.pact.model.matchingrules.TypeMatcher
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.time.DateUtils
import scala.xml.Elem

import java.text.ParseException

import static au.com.dius.pact.matchers.MatcherExecutorKt.matchInclude
import static au.com.dius.pact.matchers.MatcherExecutorKt.valueOf
import static au.com.dius.pact.matchers.MatcherExecutorKt.safeToString

/**
 * Executor for matchers
 */
@Slf4j
class MatcherExecutor {
  static <Mismatch> List<Mismatch> domatch(Category matchers, List<String> path, def expected, def actual,
                                           MismatchFactory<Mismatch> mismatchFn) {
    def result = matchers.allMatchingRules().collect { matchingRule ->
      domatch(matchingRule, path, expected, actual, mismatchFn)
    }

    if (matchers.ruleLogic == RuleLogic.AND) {
      result.flatten() as List<Mismatch>
    } else {
      if (result.any { it.empty }) {
        []
      } else {
        result.flatten() as List<Mismatch>
      }
    }
  }

  static <Mismatch> List<Mismatch> domatch(MatchingRule matcher, List<String> path, def expected, def actual,
                                           MismatchFactory<Mismatch> mismatchFn) {
    if (matcher instanceof RegexMatcher) {
      matchRegex(matcher.regex, path, expected, actual, mismatchFn)
    } else if (matcher instanceof TypeMatcher) {
      matchType(path, expected, actual, mismatchFn)
    } else if (matcher instanceof NumberTypeMatcher) {
      matchNumber(matcher.numberType, path, expected, actual, mismatchFn)
    } else if (matcher instanceof DateMatcher) {
      matchDate(matcher.format, path, expected, actual, mismatchFn)
    } else if (matcher instanceof TimeMatcher) {
      matchTime(matcher.format, path, expected, actual, mismatchFn)
    } else if (matcher instanceof TimestampMatcher) {
      matchTimestamp(matcher.format, path, expected, actual, mismatchFn)
    } else if (matcher instanceof MinTypeMatcher) {
      matchMinType(matcher.min, path, expected, actual, mismatchFn)
    } else if (matcher instanceof MaxTypeMatcher) {
      matchMaxType(matcher.max, path, expected, actual, mismatchFn)
    } else if (matcher instanceof IncludeMatcher) {
      matchInclude(matcher.value, path, expected, actual, mismatchFn)
    } else {
      matchEquality(path, expected, actual, mismatchFn)
    }
  }

  static <Mismatch> List<Mismatch> matchEquality(List<String> path, Object expected, Object actual,
                                                 MismatchFactory<Mismatch> mismatchFactory) {
    def matches = actual == expected
    log.debug("comparing ${valueOf(actual)} to ${valueOf(expected)} at $path -> $matches")
    if (matches) {
      []
    } else {
      [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to equal ${valueOf(actual)}", path) ]
    }
  }

  static <Mismatch> List<Mismatch> matchRegex(String regex, List<String> path, Object expected, Object actual,
                                              MismatchFactory<Mismatch> mismatchFactory) {
    def matches = safeToString(actual).matches(regex)
    log.debug("comparing ${valueOf(actual)} with regexp $regex at $path -> $matches")
    if (matches
      || expected instanceof List && actual instanceof List
      || expected instanceof scala.collection.immutable.List && actual instanceof scala.collection.immutable.List
      || expected instanceof Map && actual instanceof Map
      || expected instanceof scala.collection.Map && actual instanceof scala.collection.Map) {
      []
    } else {
      [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to match '$regex'", path) ]
    }
  }

  static <Mismatch> List<Mismatch> matchType(List<String> path, Object expected, Object actual,
                                             MismatchFactory<Mismatch> mismatchFactory) {
    log.debug("comparing type of ${valueOf(actual)} to ${valueOf(expected)} at $path")
    if (expected instanceof String && actual instanceof String
      || expected instanceof Number && actual instanceof Number
      || expected instanceof Boolean && actual instanceof Boolean
      || expected instanceof List && actual instanceof List
      || expected instanceof scala.collection.immutable.List && actual instanceof scala.collection.immutable.List
      || expected instanceof Map && actual instanceof Map
      || expected instanceof scala.collection.Map && actual instanceof scala.collection.Map
      || expected instanceof Elem && actual instanceof Elem && actual.label == expected.label) {
      []
    } else if (expected == null) {
      if (actual == null) {
        []
      } else {
        [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be null", path) ]
      }
    } else {
      [ mismatchFactory.create(expected, actual,
        "Expected ${valueOf(actual)} to be the same type as ${valueOf(expected)}", path) ]
    }
  }

  static def <Mismatch, Mismatch> List<Mismatch> matchNumber(NumberTypeMatcher.NumberType numberType, List<String> path,
                                                             def expected, def actual,
                                                             MismatchFactory<Mismatch> mismatchFactory) {
    if (expected == null && actual != null) {
      return [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be null", path) ]
    }
    switch (numberType) {
      case NumberTypeMatcher.NumberType.NUMBER:
        log.debug("comparing type of ${valueOf(actual)} to a number at $path")
        if (!(actual instanceof Number)) {
          return [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be a number", path) ]
        }
        break
      case NumberTypeMatcher.NumberType.INTEGER:
        log.debug("comparing type of ${valueOf(actual)} to an integer at $path")
        if (!(actual instanceof Integer) && !(actual instanceof Long) && !(actual instanceof BigInteger)) {
          return [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be an integer", path) ]
        }
        break
      case NumberTypeMatcher.NumberType.DECIMAL:
        log.debug("comparing type of ${valueOf(actual)} to a decimal at $path")
        if (!(actual instanceof Float) && !(actual instanceof Double) && !(actual instanceof BigDecimal)) {
          return [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be a decimal number",
            path) ]
        }
        break
    }
    []
  }

  static <Mismatch> List<Mismatch> matchDate(String format, List<String> path, Object expected, Object actual,
                                             MismatchFactory<Mismatch> mismatchFactory) {
    def pattern = format ?: 'yyyy-MM-dd'
    log.debug("comparing ${valueOf(actual)} to date pattern $pattern at $path")
    try {
      DateUtils.parseDate(safeToString(actual), pattern)
      []
    } catch (ParseException e) {
      [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to match a date of '$pattern': " +
        "${e.message}", path) ]
    }
  }

  static <Mismatch> List<Mismatch> matchTime(String format, List<String> path, Object expected, Object actual,
                                             MismatchFactory<Mismatch> mismatchFactory) {
    def pattern = format ?: 'HH:mm:ss'
    log.debug("comparing ${valueOf(actual)} to time pattern $pattern at $path")
    try {
      DateUtils.parseDate(safeToString(actual), pattern)
      []
    } catch (ParseException e) {
      [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to match a time of '$pattern': " +
        "${e.message}", path) ]
    }
  }

  static <Mismatch> List<Mismatch> matchTimestamp(String format, List<String> path, Object expected, Object actual,
                                             MismatchFactory<Mismatch> mismatchFactory) {
    def pattern = format ?: 'yyyy-MM-dd HH:mm:ssZZZ'
    log.debug("comparing ${valueOf(actual)} to timestamp pattern $pattern at $path")
    try {
      DateUtils.parseDate(safeToString(actual), pattern)
      []
    } catch (ParseException e) {
      [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to match a timestamp of '$pattern': " +
        "${e.message}", path) ]
    }
  }

  static <Mismatch> List<Mismatch> matchMinType(Integer min, List<String> path, Object expected, Object actual,
                                                MismatchFactory<Mismatch> mismatchFactory) {
    log.debug("comparing ${valueOf(actual)} with minimum $min at $path")
    if (actual instanceof List) {
      if (actual.size() < min) {
        [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path) ]
      } else {
        []
      }
    } else if (actual instanceof scala.collection.immutable.List) {
      if (actual.size() < min) {
        [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path) ]
      } else {
        []
      }
    } else if (actual instanceof Elem) {
      if (actual.child().size() < min) {
        [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path) ]
      } else {
        []
      }
    } else {
      matchType(path, expected, actual, mismatchFactory)
    }
  }

  static <Mismatch> List<Mismatch> matchMaxType(Integer max, List<String> path, Object expected, Object actual,
                                                MismatchFactory<Mismatch> mismatchFactory) {
    log.debug("comparing ${valueOf(actual)} with maximum $max at $path")
    if (actual instanceof List) {
      if (actual.size() > max) {
        [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path) ]
      } else {
        []
      }
    } else if (actual instanceof scala.collection.immutable.List) {
      if (actual.size() > max) {
        [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path) ]
      } else {
        []
      }
    } else if (actual instanceof Elem) {
      if (actual.child().size() > max) {
        [ mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path) ]
      } else {
        []
      }
    } else {
      matchType(path, expected, actual, mismatchFactory)
    }
  }

}
