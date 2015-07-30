package au.com.dius.pact.consumer.groovy

import groovy.json.JsonBuilder
import java.util.regex.Pattern

/**
 * DSL Builder for constructing JSON bodies
 */
class PactBodyBuilder extends BaseBuilder {

  public static final String PATH_SEP = '.'
  public static final String START_LIST = '['
  public static final String END_LIST = ']'
  def bodyMap = [:]
  def matchers = [:]
  def path = '$.body'
  def bodyStack = []

  String getBody() {
    new JsonBuilder(bodyMap).toPrettyString()
  }

  def methodMissing(String name, args) {
    if (args.size() > 0) {
      addAttribute(name, args[0], args.size() > 1 ? args[1] : null)
    } else {
      bodyMap[name] = [:]
    }
  }

  def propertyMissing(String name) {
    switch (name) {
      case 'hexValue':
        hexValue()
        break
      case 'identifier':
        identifier()
        break
      case 'ipAddress':
        ipAddress()
        break
      case 'numeric':
        numeric()
        break
      case 'integer':
        integer()
        break
      case 'real':
        real()
        break
      case 'timestamp':
        timestamp()
        break
      case 'time':
        timestamp()
        break
      case 'date':
        timestamp()
        break
      case 'guid':
        guid()
        break
      default:
        throw new MissingPropertyException(name, this.class)
    }
  }

  def propertyMissing(String name, def value) {
    addAttribute(name, value)
  }

  private void addAttribute(String name, def value, def value2 = null) {
    if (value instanceof Pattern) {
      def matcher = regexp(value as Pattern, value2)
      bodyMap[name] = setMatcherAttribute(matcher, path + PATH_SEP + name)
    } else if (value instanceof LikeMatcher) {
      setMatcherAttribute(value, path + PATH_SEP + name)
      bodyMap[name] = [ invokeClosure(value.values.last(), PATH_SEP + name + '[*]') ]
    } else if (value instanceof Matcher) {
      bodyMap[name] = setMatcherAttribute(value, path + PATH_SEP + name)
    } else if (value instanceof List) {
      bodyMap[name] = []
      value.eachWithIndex { def entry, int i ->
        if (entry instanceof Matcher) {
          bodyMap[name] << setMatcherAttribute(entry, path + PATH_SEP + name + START_LIST + i + END_LIST)
        } else if (entry instanceof Closure) {
          bodyMap[name] << invokeClosure(entry, PATH_SEP + name + START_LIST + i + END_LIST)
        } else {
          bodyMap[name] << entry
        }
      }
    } else if (value instanceof Closure) {
      bodyMap[name] = invokeClosure(value, PATH_SEP + name)
    } else {
      bodyMap[name] = value
    }
  }

  private invokeClosure(Closure entry, String subPath) {
    def oldpath = path
    path += subPath
    entry.delegate = this
    entry.resolveStrategy = Closure.DELEGATE_FIRST
    bodyStack.push(bodyMap)
    bodyMap = [:]
    entry.call()
    path = oldpath
    def tmp = bodyMap
    bodyMap = bodyStack.pop()
    tmp
  }

  private setMatcherAttribute(Matcher value, String attributePath) {
    if (value.matcher) {
      matchers[attributePath] = value.matcher
    }
    value.value
  }

  /**
   * Array with maximum size and each element like the following object
   */
  def maxLike(Integer max, Closure closure) {
    new MaxLikeMatcher(values: [max, closure])
  }

  /**
   * Array with minimum size and each element like the following object
   */
  def minLike(Integer min, Closure closure) {
    new MinLikeMatcher(values: [min, closure])
  }

  /**
   * Array where each element is like the following object
   */
  def eachLike(Closure closure) {
    new EachLikeMatcher(values: [null,  closure])
  }

}
