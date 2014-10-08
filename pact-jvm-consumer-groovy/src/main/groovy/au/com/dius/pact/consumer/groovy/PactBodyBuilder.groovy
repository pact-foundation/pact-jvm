package au.com.dius.pact.consumer.groovy

import groovy.json.JsonBuilder
import org.apache.commons.lang3.RandomStringUtils

import java.util.regex.Pattern

class PactBodyBuilder {

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

  def regexp(Pattern re, String value = null) {
    regexp(re.toString(), value)
  }

  def regexp(String regexp, String value = null) {
    new RegexpMatcher(values: [regexp, value])
  }

  def hexValue(String value = null) {
    new RegexpMatcher(values: ['[0-9a-fA-F]+', RandomStringUtils.random(10, "0123456789abcdef")])
  }

  def identifier(def value = null) {
    new TypeMatcher(values: value)
  }

  def ipAddress(String value = null) {
    new RegexpMatcher(values: ['\\d{1,3}\\.)+\\d{1,3}', value ?: '127.0.0.1'])
  }

  def numeric(Number value = null) {
    new TypeMatcher(values: value ?: RandomStringUtils.randomNumeric(10) as Long)
  }

  def timestamp(def value = null) {
    new TimestampMatcher(values: value)
  }

  def guid(String value = null) {
    new RegexpMatcher(values: ['[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', value ?: UUID.randomUUID().toString()])
  }

  def methodMissing(String name, args) {
    if (args.size() > 0) {
      if (args[0] instanceof Pattern) {
        def matcher = regexp(args[0], args.size() > 1 ? args[1] : null)
        matchers[path + '.' + name] = matcher.matcher
        bodyMap[name] = matcher.value
      } else if (args[0] instanceof Matcher) {
        matchers[path + '.' + name] = args[0].matcher
        bodyMap[name] = args[0].value
      } else if (args[0] instanceof List) {
        bodyMap[name] = []
        args[0].eachWithIndex { def entry, int i ->
          if (entry instanceof Matcher) {
            matchers[path + '.' + name + '.' + i] = entry.matcher
            bodyMap[name] << entry.value
          } else if (entry instanceof Closure) {
            def oldpath = path
            path += '.' + name + '.' + i
            entry.delegate = this
            entry.resolveStrategy = Closure.DELEGATE_FIRST
            bodyStack.push(bodyMap)
            bodyMap = [:]
            entry.call()
            path = oldpath
            def tmp = bodyMap
            bodyMap = bodyStack.pop()
            bodyMap[name] << tmp
          } else {
            bodyMap[name] << entry
          }
        }
      } else if (args[0] instanceof Closure) {
        def oldpath = path
        path += '.' + name
        args[0].delegate = this
        args[0].resolveStrategy = Closure.DELEGATE_FIRST
        bodyStack.push(bodyMap)
        bodyMap = [:]
        args[0].call()
        path = oldpath
        def tmp = bodyMap
        bodyMap = bodyStack.pop()
        bodyMap[name] = tmp
      } else {
        bodyMap[name] = args.size() == 1 ? args[0] : args
      }
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
      case 'timestamp':
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
    methodMissing(name, [value])
  }

}
