package au.com.dius.pact.model.generators

import au.com.dius.pact.model.ContentType
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PathToken
import au.com.dius.pact.model.parsePath
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.math.RandomUtils
import java.util.*

enum class Category {
  METHOD, PATH, HEADER, QUERY, BODY, STATUS
}

interface ContentTypeHandler {
  fun processBody(value: String, fn: (QueryResult) -> Unit): OptionalBody
  fun applyKey(body: QueryResult, key: String, generator: Generator)
}

val contentTypeHandlers: Map<String, ContentTypeHandler> = mutableMapOf("application/json" to JsonContentTypeHandler)

data class QueryResult(var value: Any, val key: Any? = null, val parent: Any? = null)

object JsonContentTypeHandler : ContentTypeHandler {
  override fun processBody(value: String, fn: (QueryResult) -> Unit): OptionalBody {
    val bodyJson = QueryResult(JsonSlurper().parseText(value))
    fn.invoke(bodyJson)
    return OptionalBody.body(JsonOutput.toJson(bodyJson.value))
  }

  override fun applyKey(body: QueryResult, key: String, generator: Generator) {
    val pathExp = parsePath(key)
    queryObjectGraph(pathExp.iterator(), body) { (value, valueKey, parent) ->
      if (parent is MutableMap<*, *>) {
        (parent as MutableMap<String, Any>)[valueKey.toString()] = generator.generate(value)
      } else if (parent is MutableList<*>) {
        (parent as MutableList<Any>)[valueKey as Int] = generator.generate(value)
      } else {
        body.value = generator.generate(value)
      }
    }
  }

  private fun queryObjectGraph(pathExp: Iterator<PathToken>, body: QueryResult, fn: (QueryResult) -> Unit) {
    var stack = Stack<QueryResult>()
    var bodyCursor = body
    while (pathExp.hasNext()) {
      val token = pathExp.next()
      when (token) {
        is PathToken.Field -> {
          if (bodyCursor.value is Map<*, *> && (bodyCursor.value as Map<*, *>).containsKey(token.name)) {
            val map = bodyCursor.value as Map<*, *>
            stack.push(bodyCursor)
            bodyCursor = QueryResult(map[token.name]!!, token.name, bodyCursor.value)
          } else {
            return
          }
        }
        is PathToken.Index -> {
          if (bodyCursor.value is List<*> && (bodyCursor.value as List<*>).size > token.index) {
            val list = bodyCursor.value as List<*>
            stack.push(bodyCursor)
            bodyCursor = QueryResult(list[token.index]!!, token.index, bodyCursor.value)
          } else {
            return
          }
        }
      }
    }

    fn(bodyCursor)
  }

}

data class Generators(val categories: MutableMap<Category, MutableMap<String, Generator>> = HashMap()) {

  companion object {

    @JvmStatic fun fromMap(map: Map<String, Any>?): Generators {
      return Generators()
    }

  }

  @JvmOverloads
  fun addGenerator(category: Category, key: String? = "", generator: Generator): Generators {
    if (categories.containsKey(category) && categories[category] != null) {
      categories[category]?.put((key ?: ""), generator)
    } else {
      categories[category] = mutableMapOf((key ?: "") to generator)
    }
    return this
  }

  fun applyGenerator(category: Category, closure: (String, Generator?) -> Unit) {
    if (categories.containsKey(category) && categories[category] != null) {
      val categoryValues = categories[category]
      if (categoryValues != null) {
        for ((key, value) in categoryValues) {
          closure.invoke(key, value)
        }
      }
    }
  }

  fun applyBodyGenerators(body: OptionalBody, contentType: ContentType): OptionalBody {
    return when (body.state) {
      OptionalBody.State.EMPTY, OptionalBody.State.MISSING, OptionalBody.State.NULL -> body
      OptionalBody.State.PRESENT -> when {
        contentType.isJson() -> processBody(body.value!!, "application/json")
        contentType.isXml() -> processBody(body.value!!, "application/xml")
        else -> body
      }
    }
  }

  private fun processBody(value: String, contentType: String): OptionalBody {
    val handler = contentTypeHandlers[contentType]
    return handler?.processBody(value) { body: QueryResult ->
      applyGenerator(Category.BODY) { key: String, generator: Generator? ->
        if (generator != null) {
          handler.applyKey(body, key, generator)
        }
      }
    } ?: OptionalBody.body(value)
  }

}

interface Generator {
  fun generate(base: Any?): Any
}

data class RandomIntGenerator(val min: Int, val max: Int) : Generator {
  override fun generate(base: Any?): Any {
    return min + RandomUtils.nextInt(max - min)
  }
}

data class RandomStringGenerator(val size: Int = 20) : Generator {
  override fun generate(base: Any?): Any {
    return RandomStringUtils.randomAlphanumeric(size)
  }
}

class UuidGenerator : Generator {
  override fun generate(base: Any?): Any {
    return UUID.randomUUID().toString()
  }
}
