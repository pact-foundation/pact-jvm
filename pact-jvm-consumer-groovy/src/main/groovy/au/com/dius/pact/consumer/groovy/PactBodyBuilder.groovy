package au.com.dius.pact.consumer.groovy

import groovy.json.JsonBuilder
import java.util.regex.Pattern

class PactBodyBuilder extends Matchers {

  def bodyMap = [:]
  def matchers = [:]
  def path = '$.body'
  def bodyStack = []

  def call(Closure closure) {
    build(closure)
  }

  def build(Closure closure) {
    closure.delegate = this
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
  }

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
        break;
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
      bodyMap[name] = setMatcherAttribute(matcher, path + '.' + name)
    } else if (value instanceof Matcher) {
      bodyMap[name] = setMatcherAttribute(value, path + '.' + name)
    } else if (value instanceof List) {
      bodyMap[name] = []
      value.eachWithIndex { def entry, int i ->
        if (entry instanceof Matcher) {
          bodyMap[name] << setMatcherAttribute(entry, path + '.' + name + '.' + (i + 1))
        } else if (entry instanceof Closure) {
          bodyMap[name] << invokeClosure(entry, '.' + name + '.' + (i + 1))
        } else {
          bodyMap[name] << entry
        }
      }
    } else if (value instanceof Closure) {
      bodyMap[name] = invokeClosure(value, '.' + name)
    } else {
      bodyMap[name] = value
    }
  }

  private def invokeClosure(Closure entry, String subPath) {
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

  private def setMatcherAttribute(Matcher value, String attributePath) {
    matchers[attributePath] = value.matcher
    value.value
  }

}
