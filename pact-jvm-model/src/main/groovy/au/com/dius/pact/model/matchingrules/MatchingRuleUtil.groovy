package au.com.dius.pact.model.matchingrules

import groovy.util.logging.Slf4j

/**
 * Utility class for matching rules
 */
@Slf4j
class MatchingRuleUtil {
  static MatchingRule fromMap(Map map) {
    if (!map) {
      log.warn('Matcher definition is empty, defaulting to equality matching')
      new EqualsMatcher()
    } else if (map.containsKey('match')) {
      switch (map['match']) {
        case 'regex':
          new RegexMatcher(map['regex'] as String)
          break
        case 'equality':
          new EqualsMatcher()
          break
        case 'include':
          new IncludeMatcher(map['value'] as String)
          break
        case 'type':
          if (map.containsKey('min')) {
            new MinTypeMatcher(Integer.parseInt(map['min'] as String))
          } else if (map.containsKey('max')) {
            new MaxTypeMatcher(Integer.parseInt(map['max'] as String))
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
        case 'min':
          new MinTypeMatcher(Integer.parseInt(map['min'] as String))
          break
        case 'max':
          new MaxTypeMatcher(Integer.parseInt(map['max'] as String))
          break
        case 'timestamp':
          new TimestampMatcher(map['timestamp'] as String)
          break
        case 'time':
          new TimeMatcher(map['time'] as String)
          break
        case 'date':
          new DateMatcher(map['date'] as String)
          break
        default:
          log.warn("Unrecognised matcher ${map['match']}, defaulting to equality matching")
          new EqualsMatcher()
          break
      }
    } else if (map.containsKey('regex')) {
      new RegexMatcher(map['regex'] as String)
    } else if (map.containsKey('min')) {
      new MinTypeMatcher(Integer.parseInt(map['min'] as String))
    } else if (map.containsKey('max')) {
      new MaxTypeMatcher(Integer.parseInt(map['max'] as String))
    } else if (map.containsKey('timestamp')) {
      new TimestampMatcher(map['timestamp'] as String)
    } else if (map.containsKey('time')) {
      new TimeMatcher(map['time'] as String)
    } else if (map.containsKey('date')) {
      new DateMatcher(map['date'] as String)
    } else {
      log.warn("Unrecognised matcher definition $map, defaulting to equality matching")
      new EqualsMatcher()
    }
  }
}
