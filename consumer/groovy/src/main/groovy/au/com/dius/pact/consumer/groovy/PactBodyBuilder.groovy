package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.Feature
import au.com.dius.pact.core.model.FeatureToggles
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.Category
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.support.expressions.DataType
import groovy.json.JsonBuilder
import org.apache.commons.lang3.StringUtils

import java.util.regex.Pattern

import static au.com.dius.pact.core.model.PathExpressionsKt.PATH_SPECIAL_CHARS

/**
 * DSL Builder for constructing JSON bodies
 */
class PactBodyBuilder extends GroovyBuilder {

  public static final String PATH_SEP = '.'
  public static final String START_LIST = '['
  public static final String END_LIST = ']'
  public static final String ALL_LIST_ITEMS = '[*]'
  public static final int TWO = 2
  public static final String STAR = '*'

  def matchers = new Category('body')
  def generators = new Generators().addCategory(au.com.dius.pact.core.model.generators.Category.BODY)
  def mimetype = null
  Boolean prettyPrintBody = null

  private bodyRepresentation = [:]
  private path = '$'
  private final bodyStack = []

  String getBody() {
    if (shouldPrettyPrint()) {
      new JsonBuilder(bodyRepresentation).toPrettyString()
    } else {
      new JsonBuilder(bodyRepresentation).toString()
    }
  }

  private boolean shouldPrettyPrint() {
    prettyPrintBody == null && (mimetype != null && !isCompactMimeType(mimetype) || mimetype == null) || prettyPrintBody
  }

  def methodMissing(String name, args) {
    if (args.size() > 0) {
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
      setupLikeMatcherAttribute(value, matcherName, name)
    } else if (value instanceof OrMatcher) {
      bodyRepresentation[name] = value.value
      matchers.setRules(path + buildPath(matcherName), new MatchingRuleGroup(value.matchers*.matcher, RuleLogic.OR))
    } else if (value instanceof AndMatcher) {
      bodyRepresentation[name] = value.value
      matchers.setRules(path + buildPath(matcherName), new MatchingRuleGroup(value.matchers*.matcher, RuleLogic.AND))
    } else if (value instanceof Matcher) {
      bodyRepresentation[name] = setMatcherAttribute(value, path + buildPath(matcherName))
    } else if (value instanceof List) {
      setupListAttribute(name, value, matcherName)
    } else if (value instanceof Closure) {
      if (matcherName == STAR) {
        setMatcherAttribute(new TypeMatcher(), path + buildPath(matcherName))
      }
      bodyRepresentation[name] = invokeClosure(value, buildPath(matcherName))
    } else if (value instanceof GeneratedValue) {
      bodyRepresentation[name] = value.exampleValue
      this.generators.addGenerator(au.com.dius.pact.core.model.generators.Category.BODY, path + buildPath(name),
        new ProviderStateGenerator(value.expression, DataType.from(value.exampleValue)))
      setMatcherAttribute(new TypeMatcher(), path + buildPath(matcherName))
    } else {
      bodyRepresentation[name] = value
    }
  }

  private void setupListAttribute(String name, List value, String matcherName) {
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
  }

  private void setupLikeMatcherAttribute(LikeMatcher value, String matcherName, String name) {
    setMatcherAttribute(value, path + buildPath(matcherName))
    bodyRepresentation[name] = []
    value.numberExamples.times { index ->
      def exampleValue = value.value
      if (exampleValue instanceof Closure) {
        bodyRepresentation[name] << invokeClosure(exampleValue, buildPath(matcherName, ALL_LIST_ITEMS))
      } else if (exampleValue instanceof LikeMatcher) {
        bodyRepresentation[name] << invoke(exampleValue, buildPath(matcherName, ALL_LIST_ITEMS))
      } else if (exampleValue instanceof Matcher) {
        bodyRepresentation[name] << setMatcherAttribute(exampleValue, path + buildPath(matcherName, ALL_LIST_ITEMS))
      } else if (exampleValue instanceof Pattern) {
        def matcher = regexp(exampleValue as Pattern, null)
        bodyRepresentation[name] << setMatcherAttribute(matcher, path + buildPath(matcherName, ALL_LIST_ITEMS))
      } else if (exampleValue instanceof List) {
        def list = []
        exampleValue.eachWithIndex { entry, i ->
          if (entry instanceof Matcher) {
            list << setMatcherAttribute(entry, path + buildPath(matcherName, START_LIST + i + END_LIST))
          } else if (entry instanceof Closure) {
            list << invokeClosure(entry, buildPath(matcherName, START_LIST + i + END_LIST))
          } else {
            list << entry
          }
        }
        bodyRepresentation[name] << list
      } else {
        bodyRepresentation[name] << exampleValue
      }
    }
  }

  private String buildPath(String name, String children = '') {
    def key = PATH_SEP + name
    if (name != STAR && StringUtils.containsAny(name, PATH_SPECIAL_CHARS)) {
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
    def result = entry.call()
    if (result instanceof Matcher) {
      throw new InvalidMatcherException('Detected an invalid use of the matchers. ' +
        'If you are using matchers like "eachLike" they need to be assigned to something. For instance:\n' +
        '  `fruits eachLike(1)` or `id = integer()`'
      )
    }
    path = oldpath
    def tmp = bodyRepresentation
    bodyRepresentation = bodyStack.pop()
    tmp
  }

  private invoke(LikeMatcher matcher, String subPath) {
    def oldpath = path
    path += subPath
    bodyStack.push(bodyRepresentation)
    bodyRepresentation = []
    def value = setMatcherAttribute(matcher, path)
    matcher.numberExamples.times { index ->
      if (value instanceof List) {
        bodyRepresentation << build(value as List, path)
      } else if (value instanceof Closure) {
        bodyRepresentation << invokeClosure(value, ALL_LIST_ITEMS)
      } else if (value instanceof Matcher) {
        bodyRepresentation << setMatcherAttribute(value, path + START_LIST + STAR + END_LIST)
      } else {
        bodyRepresentation << matcher.value
      }
    }
    path = oldpath
    def tmp = bodyRepresentation
    bodyRepresentation = bodyStack.pop()
    tmp
  }

  private setMatcherAttribute(Matcher value, String attributePath) {
    if (value.matcher) {
      matchers.setRule(attributePath, value.matcher)
    }
    if (value.generator) {
      generators.addGenerator(au.com.dius.pact.core.model.generators.Category.BODY, attributePath, value.generator)
    }
    value.value
  }

  def build(List array, String path = '') {
    def index = 0
    array.collect {
      if (it instanceof Closure) {
        invokeClosure(it, START_LIST + (index++) + END_LIST)
      } else if (it instanceof Matcher) {
        setMatcherAttribute(it, path + START_LIST + (index++) + END_LIST)
      } else {
        index++
        it
      }
    }
  }

  def build(LikeMatcher matcher) {
    setMatcherAttribute(matcher, path)

    def example = matcher.value
    if (matcher.value instanceof List) {
      example = build(matcher.value as List, path)
    } else if (matcher.value instanceof Closure) {
      example = invokeClosure(matcher.value, ALL_LIST_ITEMS)
    } else if (matcher.value instanceof Matcher) {
      example = setMatcherAttribute(matcher.value, path + START_LIST + STAR + END_LIST)
    }

    def value = []
    matcher.numberExamples.times {
      value << example
    }
    value
  }

  /**
   * Matches the values of the map ignoring the keys. Note: this needs the Java system property
   * "pact.matching.wildcard" set to value "true" when the pact file is verified.
   */
  def keyLike(String key, def value) {
    if (FeatureToggles.isFeatureSet(Feature.UseMatchValuesMatcher)) {
      setMatcherAttribute(new ValuesMatcher(), path)
      if (value instanceof Closure) {
        bodyRepresentation[key] = invokeClosure(value, buildPath(STAR))
      } else {
        addAttribute(key, STAR, value)
      }
    } else {
      addAttribute(key, STAR, value)
    }
  }

  /**
   * Marks a item as to be injected from the provider state
   * @param expression Expression to lookup in the provider state context
   * @param exampleValue Example value to use in the consumer test
   * @return example value
   */
  def fromProviderState(String expression, def exampleValue) {
    new GeneratedValue(expression, exampleValue)
  }

  @Override
  @SuppressWarnings('UnnecessaryOverridingMethod')
  def call(@DelegatesTo(value = PactBodyBuilder, strategy = Closure.DELEGATE_FIRST) Closure closure) {
    super.build(closure)
  }

  @Override
  @SuppressWarnings('UnnecessaryOverridingMethod')
  def build(@DelegatesTo(value = PactBodyBuilder, strategy = Closure.DELEGATE_FIRST) Closure closure) {
    super.build(closure)
  }
}
