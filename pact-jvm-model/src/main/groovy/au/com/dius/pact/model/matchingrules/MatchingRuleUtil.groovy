package au.com.dius.pact.model.matchingrules

import groovy.util.logging.Slf4j

/**
 * Utility class for matching rules
 */
@Slf4j
@SuppressWarnings(['CyclomaticComplexity', 'UnusedObject'])
class MatchingRuleUtil {

  private static final String MATCH = 'match'
  private static final String MIN = 'min'
  private static final String MAX = 'max'
  private static final String REGEX = 'regex'
  private static final String TIMESTAMP = 'timestamp'
  private static final String TIME = 'time'
  private static final String DATE = 'date'

  static MatchingRule fromMap(Map map) {
    if (!map) {
      log.warn('Matcher definition is empty, defaulting to equality matching')
      new EqualsMatcher()
    } else if (map.containsKey(MATCH)) {
      switch (map[MATCH]) {
        case REGEX:
          new RegexMatcher(map[REGEX] as String)
          break
        case 'equality':
          new EqualsMatcher()
          break
        case 'include':
          new IncludeMatcher(map['value'] as String)
          break
        case 'type':
          if (map.containsKey(MIN)) {
            new MinTypeMatcher(Integer.parseInt(map[MIN] as String))
          } else if (map.containsKey(MAX)) {
            new MaxTypeMatcher(Integer.parseInt(map[MAX] as String))
          } else {
            new TypeMatcher()
          }
          break
        case 'number':
          new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
          break
        case 'integer':
          new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)
          break
        case 'decimal':
          new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
          break
        case 'real':
          log.warn('The \'real\' type matcher is deprecated, use \'decimal\' instead')
          new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
          break
        case MIN:
          new MinTypeMatcher(Integer.parseInt(map[MIN] as String))
          break
        case MAX:
          new MaxTypeMatcher(Integer.parseInt(map[MAX] as String))
          break
        case TIMESTAMP:
          new TimestampMatcher(map[TIMESTAMP] as String)
          break
        case TIME:
          new TimeMatcher(map[TIME] as String)
          break
        case DATE:
          new DateMatcher(map[DATE] as String)
          break
        default:
          log.warn("Unrecognised matcher ${map[MATCH]}, defaulting to equality matching")
          new EqualsMatcher()
          break
      }
    } else if (map.containsKey(REGEX)) {
      new RegexMatcher(map[REGEX] as String)
    } else if (map.containsKey(MIN)) {
      new MinTypeMatcher(Integer.parseInt(map[MIN] as String))
    } else if (map.containsKey(MAX)) {
      new MaxTypeMatcher(Integer.parseInt(map[MAX] as String))
    } else if (map.containsKey(TIMESTAMP)) {
      new TimestampMatcher(map[TIMESTAMP] as String)
    } else if (map.containsKey(TIME)) {
      new TimeMatcher(map[TIME] as String)
    } else if (map.containsKey(DATE)) {
      new DateMatcher(map[DATE] as String)
    } else {
      log.warn("Unrecognised matcher definition $map, defaulting to equality matching")
      new EqualsMatcher()
    }
  }
}
