package au.com.dius.pact.provider.broker

import au.com.dius.pact.pactbroker.BiFunction
import au.com.dius.pact.pactbroker.HalClientBase
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import groovy.transform.Canonical
import groovy.util.logging.Slf4j

/**
 * HAL client for navigating the HAL links
 */
@Slf4j
@Canonical
@SuppressWarnings('DuplicateStringLiteral')
class HalClient extends HalClientBase {

  /**
   * @deprecated Use httpClient from the base class
   */
  @Deprecated
  def http

  HalClient(
    String baseUrl,
    Map<String, ?> options) {
    super(baseUrl, options)
  }

  HalClient(String baseUrl) {
    super(baseUrl)
  }

  def methodMissing(String name, args) {
    super.initPathInfo()
    JsonElement matchingLink = super.pathInfo['_links'][name]
    if (matchingLink != null) {
      if (args && args.last() instanceof Closure) {
        if (matchingLink.isJsonArray()) {
          return matchingLink.each(args.last() as Closure)
        }
        return args.last().call(matchingLink)
      }
      return matchingLink
    }
    throw new MissingMethodException(name, this.class, args)
  }

  @Override
  void forAll(String linkName, org.apache.commons.collections4.Closure<Map<String, Object>> just) {
    super.initPathInfo()
    JsonElement matchingLink = pathInfo['_links'][linkName]
    if (matchingLink != null) {
      if (matchingLink.isJsonArray()) {
        matchingLink.asJsonArray.each { just.execute(fromJson(it)) }
      } else {
        just.execute(fromJson(matchingLink.asJsonObject))
      }
    }
  }

  static Map<String, Object> asMap(JsonObject jsonObject) {
    jsonObject.entrySet().collectEntries { Map.Entry<String, JsonElement> entry ->
      [entry.key, fromJson(entry.value)]
    }
  }

  static fromJson(JsonElement jsonValue) {
    if (jsonValue.jsonObject) {
      asMap(jsonValue.asJsonObject)
    } else if (jsonValue.jsonArray) {
      jsonValue.asJsonArray.collect { fromJson(it) }
    } else if (jsonValue.jsonNull) {
      null
    } else {
      JsonPrimitive primitive = jsonValue.asJsonPrimitive
      if (primitive.isBoolean()) {
        primitive.asBoolean
      } else if (primitive.isNumber()) {
        primitive.asBigDecimal
      } else {
        primitive.asString
      }
    }
  }

  String linkUrl(String name) {
    pathInfo.'_links'[name].href
  }
}
