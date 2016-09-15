package au.com.dius.pact.consumer.groovy

import groovy.json.JsonBuilder
@SuppressWarnings('UnusedImport')
import io.gatling.jsonpath.Parser$

import java.util.regex.Pattern

/**
 * DSL Builder for constructing JSON bodies
 */
class PactBodyBuilder extends BaseBuilder {

  public static final String PATH_SEP = '.'
  public static final String START_LIST = '['
  public static final String END_LIST = ']'
  public static final String ALL_LIST_ITEMS = '[*]'
  public static final int TWO = 2
  public static final String STAR = '*'

  def matchers = [:]
  def mimetype = null
  Boolean prettyPrintBody = null

  private bodyRepresentation = [:]
  private path = '$.body'
  private final bodyStack = []

  String getBody() {
    if (shouldPrettyPrint()) {
      new JsonBuilder(bodyRepresentation).toPrettyString()
    } else {
      new JsonBuilder(bodyRepresentation).toString()
    }
  }

  private boolean shouldPrettyPrint() {
    prettyPrintBody == null && !compactMimeType() || prettyPrintBody
  }

  private boolean compactMimeType() {
    mimetype in COMPACT_MIME_TYPES
  }

  def methodMissing(String name, args) {
    if (name == 'keyLike') {
      addAttribute(args[0], STAR, args[1], args.size() > TWO ? args[TWO] : null)
    } else if (args.size() > 0) {
      addAttribute(name, name, args[0], args.size() > 1 ? args[1] : null)
    } else {
      bodyRepresentation[name] = [:]
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
        decimal()
        break
      case 'decimal':
        decimal()
        break
      case 'timestamp':
        timestamp()
        break
      case 'time':
        time()
        break
      case 'date':
        date()
        break
      case 'guid':
      case 'uuid':
        uuid()
        break
      default:
        throw new MissingPropertyException(name, this.class)
    }
  }

  def propertyMissing(String name, def value) {
    addAttribute(name, name, value)
  }

  private void addAttribute(String name, String matcherName, def value, def value2 = null) {
    if (value instanceof Pattern) {
      def matcher = regexp(value as Pattern, value2)
      bodyRepresentation[name] = setMatcherAttribute(matcher, path + buildPath(matcherName))
    } else if (value instanceof LikeMatcher) {
      setMatcherAttribute(value, path + buildPath(matcherName))
      bodyRepresentation[name] = []
      value.numberExamples.times {
        def exampleValue = value.values.last()
        if (exampleValue instanceof Closure) {
          bodyRepresentation[name] << invokeClosure(exampleValue, buildPath(matcherName, ALL_LIST_ITEMS))
        } else if (exampleValue instanceof Matcher) {
          bodyRepresentation[name] << setMatcherAttribute(exampleValue, path + buildPath(matcherName, ALL_LIST_ITEMS))
        } else if (exampleValue instanceof Pattern) {
          def matcher = regexp(exampleValue as Pattern, null)
          bodyRepresentation[name] << setMatcherAttribute(matcher, path + buildPath(matcherName, ALL_LIST_ITEMS))
        } else {
          bodyRepresentation[name] << exampleValue
        }
      }
    } else if (value instanceof Matcher) {
      bodyRepresentation[name] = setMatcherAttribute(value, path + buildPath(matcherName))
    } else if (value instanceof List) {
      bodyRepresentation[name] = []
      value.eachWithIndex { entry, i ->
        if (entry instanceof Matcher) {
          bodyRepresentation[name] << setMatcherAttribute(entry, path + buildPath(matcherName,
            START_LIST + i + END_LIST))
        } else if (entry instanceof Closure) {
          bodyRepresentation[name] << invokeClosure(entry, buildPath(matcherName, START_LIST + i + END_LIST))
        } else {
          bodyRepresentation[name] << entry
        }
      }
    } else if (value instanceof Closure) {
      if (matcherName == STAR) {
        setMatcherAttribute(new TypeMatcher(values: [TYPE, null]), path + buildPath(matcherName))
      }
      bodyRepresentation[name] = invokeClosure(value, buildPath(matcherName))
    } else {
      bodyRepresentation[name] = value
    }
  }

  private String buildPath(String name, String children = '') {
    def key = PATH_SEP + name
    if (name != STAR && !(name ==~ Parser$.MODULE$.FieldRegex().toString())) {
      key = "['" + name + "']"
    }
    key + children
  }

  private invokeClosure(Closure entry, String subPath) {
    def oldpath = path
    path += subPath
    entry.delegate = this
    entry.resolveStrategy = Closure.DELEGATE_FIRST
    bodyStack.push(bodyRepresentation)
    bodyRepresentation = [:]
    entry.call()
    path = oldpath
    def tmp = bodyRepresentation
    bodyRepresentation = bodyStack.pop()
    tmp
  }

  private setMatcherAttribute(Matcher value, String attributePath) {
    if (value.matcher) {
      matchers[attributePath] = value.matcher
    }
    value.value
  }

  def build(List array) {
    def index = 0
    bodyRepresentation = array.collect {
      if (it instanceof Closure) {
        invokeClosure(it, START_LIST + (index++) + END_LIST)
      } else {
        index++
        it
      }
    }
    this
  }

  def build(LikeMatcher matcher) {
    setMatcherAttribute(matcher, path)
    bodyRepresentation = [ invokeClosure(matcher.values.last(), ALL_LIST_ITEMS) ]
    this
  }

}
