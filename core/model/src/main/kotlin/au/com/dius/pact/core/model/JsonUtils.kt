package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.JsonQueryResult
import au.com.dius.pact.core.support.json.JsonValue
import org.apache.commons.collections4.IteratorUtils

/**
 * Utility functions for JSON
 */
object JsonUtils {
  /**
   * Fetches an element from the JSON given the path in Pact matching rule form
   */
  fun fetchPath(json: JsonValue?, path: String): JsonValue? {
    val pathExp = parsePath(path)
    return if (json != null) {
      val bodyJson = JsonQueryResult(json)
      queryObjectGraph(pathExp.iterator(), bodyJson) {
        it.jsonValue
      }
    } else null
  }

  @Suppress("ReturnCount")
  fun <T> queryObjectGraph(pathExp: Iterator<PathToken>, body: JsonQueryResult, fn: (JsonQueryResult) -> T?): T? {
    var bodyCursor = body
    while (pathExp.hasNext()) {
      val cursorValue = bodyCursor.value
      when (val token = pathExp.next()) {
        is PathToken.Field -> if (cursorValue is JsonValue.Object && cursorValue.has(token.name)) {
          bodyCursor = JsonQueryResult(cursorValue[token.name], token.name, bodyCursor.jsonValue)
        } else {
          return null
        }
        is PathToken.Index -> if (cursorValue is JsonValue.Array && cursorValue.values.size > token.index) {
          bodyCursor = JsonQueryResult(cursorValue[token.index], token.index, bodyCursor.jsonValue)
        } else {
          return null
        }
        is PathToken.Star -> if (cursorValue is JsonValue.Object) {
          val pathIterator = IteratorUtils.toList(pathExp)
          cursorValue.entries.forEach { (key, value) ->
            queryObjectGraph(pathIterator.iterator(), JsonQueryResult(value, key, cursorValue), fn)
          }
          return null
        } else {
          return null
        }
        is PathToken.StarIndex -> if (cursorValue is JsonValue.Array) {
          val pathIterator = IteratorUtils.toList(pathExp)
          cursorValue.values.forEachIndexed { index, item ->
            queryObjectGraph(pathIterator.iterator(), JsonQueryResult(item, index, cursorValue), fn)
          }
          return null
        } else {
          return null
        }
        else -> {}
      }
    }

    return fn(bodyCursor)
  }
}
