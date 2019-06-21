package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.PathToken
import au.com.dius.pact.core.model.parsePath
import au.com.dius.pact.core.support.Json
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.forEach
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import mu.KLogging
import org.apache.commons.collections4.IteratorUtils

enum class Category {
  METHOD, PATH, HEADER, QUERY, BODY, STATUS, METADATA
}

interface ContentTypeHandler {
  fun processBody(value: String, fn: (QueryResult) -> Unit): OptionalBody
  fun applyKey(body: QueryResult, key: String, generator: Generator, context: Map<String, Any?>)
}

val contentTypeHandlers: MutableMap<String, ContentTypeHandler> = mutableMapOf(
  "application/json" to JsonContentTypeHandler
)

fun setupDefaultContentTypeHandlers() {
  contentTypeHandlers.clear()
  contentTypeHandlers["application/json"] = JsonContentTypeHandler
}

data class QueryResult(var value: JsonElement?, val key: Any? = null, val parent: JsonElement? = null)

object JsonContentTypeHandler : ContentTypeHandler {
  override fun processBody(value: String, fn: (QueryResult) -> Unit): OptionalBody {
    val bodyJson = QueryResult(JsonParser().parse(value))
    fn.invoke(bodyJson)
    return OptionalBody.body(Json.gson.toJson(bodyJson.value).toByteArray(), ContentType.JSON)
  }

  override fun applyKey(body: QueryResult, key: String, generator: Generator, context: Map<String, Any?>) {
    val pathExp = parsePath(key)
    queryObjectGraph(pathExp.iterator(), body) { (_, valueKey, parent) ->
      @Suppress("UNCHECKED_CAST")
      when {
        parent != null && parent.isJsonObject -> parent.obj[valueKey.toString()] = Json.toJson(generator.generate(context))
        parent != null && parent.isJsonArray -> parent[valueKey as Int] = Json.toJson(generator.generate(context))
        else -> body.value = Json.toJson(generator.generate(context))
      }
    }
  }

  private fun queryObjectGraph(pathExp: Iterator<PathToken>, body: QueryResult, fn: (QueryResult) -> Unit) {
    var bodyCursor = body
    while (pathExp.hasNext()) {
      val cursorValue = bodyCursor.value
      when (val token = pathExp.next()) {
        is PathToken.Field -> if (cursorValue != null && cursorValue.isJsonObject && cursorValue.obj.has(token.name)) {
          bodyCursor = QueryResult(cursorValue.obj[token.name], token.name, bodyCursor.value)
        } else {
          return
        }
        is PathToken.Index -> if (cursorValue != null && cursorValue.isJsonArray && cursorValue.array.size() > token.index) {
          val list = cursorValue.array
          bodyCursor = QueryResult(list[token.index]!!, token.index, bodyCursor.value)
        } else {
          return
        }
        is PathToken.Star -> if (cursorValue != null && cursorValue.isJsonObject) {
          val map = cursorValue.obj
          val pathIterator = IteratorUtils.toList(pathExp)
          map.forEach { key, value ->
            queryObjectGraph(pathIterator.iterator(), QueryResult(value, key, map), fn)
          }
          return
        } else {
          return
        }
        is PathToken.StarIndex -> if (cursorValue != null && cursorValue.isJsonArray) {
          val list = cursorValue.array
          val pathIterator = IteratorUtils.toList(pathExp)
          list.forEachIndexed { index, item ->
            queryObjectGraph(pathIterator.iterator(), QueryResult(item!!, index, list), fn)
          }
          return
        } else {
          return
        }
      }
    }

    fn(bodyCursor)
  }
}

enum class GeneratorTestMode {
  Consumer, Provider
}

data class Generators(val categories: MutableMap<Category, MutableMap<String, Generator>> = mutableMapOf()) {

  companion object : KLogging() {

    @JvmStatic fun fromJson(json: JsonElement?): Generators {
      val generators = Generators()

      if (json != null && json.isJsonObject) {
        json.obj.forEach { key, generatorJson ->
          try {
            when (val category = Category.valueOf(key.toUpperCase())) {
              Category.STATUS, Category.PATH, Category.METHOD -> if (generatorJson.obj.has("type")) {
                val generator = lookupGenerator(generatorJson.obj)
                if (generator != null) {
                  generators.addGenerator(category, generator = generator)
                } else {
                  logger.warn { "Ignoring invalid generator config '$generatorJson'" }
                }
              } else {
                logger.warn { "Ignoring invalid generator config '$generatorJson.obj'" }
              }
              else -> generatorJson.obj.forEach { generatorKey, generatorValue ->
                if (generatorValue.isJsonObject && generatorValue.obj.has("type")) {
                  val generator = lookupGenerator(generatorValue.obj)
                  if (generator != null) {
                    generators.addGenerator(category, generatorKey, generator)
                  } else {
                    logger.warn { "Ignoring invalid generator config '$generatorValue'" }
                  }
                } else {
                  logger.warn { "Ignoring invalid generator config '$generatorKey -> $generatorValue'" }
                }
              }
            }
          } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Ignoring generator with invalid category '$key'" }
          }
        }
      }

      return generators
    }
  }

  @JvmOverloads
  fun addGenerator(category: Category, key: String? = "", generator: Generator): Generators {
    if (categories.containsKey(category) && categories[category] != null) {
      categories[category]?.put(key ?: "", generator)
    } else {
      categories[category] = mutableMapOf((key ?: "") to generator)
    }
    return this
  }

  @JvmOverloads
  fun addGenerators(generators: Generators, keyPrefix: String = ""): Generators {
    generators.categories.forEach { (category, map) ->
      map.forEach { (key, generator) ->
        addGenerator(category, keyPrefix + key, generator)
      }
    }
    return this
  }

  fun addCategory(category: Category): Generators {
    if (!categories.containsKey(category)) {
      categories[category] = mutableMapOf()
    }
    return this
  }

  fun applyGenerator(category: Category, mode: GeneratorTestMode, closure: (String, Generator) -> Unit) {
    if (categories.containsKey(category) && categories[category] != null) {
      val categoryValues = categories[category]
      if (categoryValues != null) {
        for ((key, generator) in categoryValues) {
          if (generator.correspondsToMode(mode)) {
            closure.invoke(key, generator)
          }
        }
      }
    }
  }

  fun applyBodyGenerators(
    body: OptionalBody,
    contentType: ContentType,
    context: Map<String, Any?>,
    mode: GeneratorTestMode
  ): OptionalBody {
    return when (body.state) {
      OptionalBody.State.EMPTY, OptionalBody.State.MISSING, OptionalBody.State.NULL -> body
      OptionalBody.State.PRESENT -> when {
        contentType.isJson() -> processBody(body.valueAsString(), "application/json", context, mode)
        contentType.isXml() -> processBody(body.valueAsString(), "application/xml", context, mode)
        else -> body
      }
    }
  }

  private fun processBody(value: String, contentType: String, context: Map<String, Any?>, mode: GeneratorTestMode):
    OptionalBody {
    val handler = contentTypeHandlers[contentType]
    return handler?.processBody(value) { body: QueryResult ->
      applyGenerator(Category.BODY, mode) { key: String, generator: Generator? ->
        if (generator != null) {
          handler.applyKey(body, key, generator, context)
        }
      }
    } ?: OptionalBody.body(value.toByteArray(), ContentType(contentType))
  }

  /**
   * If there are no generators
   */
  fun isEmpty() = categories.isEmpty()

  /**
   * If there are generators
   */
  fun isNotEmpty() = categories.isNotEmpty()

  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    if (pactSpecVersion < PactSpecVersion.V3) {
      throw InvalidPactException("Generators are only supported with pact specification version 3+")
    }
    return categories.entries.associate { (key, value) ->
      when (key) {
        Category.METHOD, Category.PATH, Category.STATUS -> key.name.toLowerCase() to value[""]!!.toMap(pactSpecVersion)
        else -> key.name.toLowerCase() to value.entries.associate { (genKey, generator) ->
          genKey to generator.toMap(pactSpecVersion)
        }
      }
    }
  }

  fun applyRootPrefix(prefix: String) {
    categories.keys.forEach { category ->
      categories[category] = categories[category]!!.mapKeys { entry ->
        when {
          entry.key.startsWith(prefix) -> entry.key
          entry.key.startsWith("$") -> prefix + entry.key.substring(1)
          else -> prefix + entry.key
        }
      }.toMutableMap()
    }
  }

  fun copyWithUpdatedMatcherRootPrefix(rootPath: String): Generators {
    val generators = this.copy(categories = this.categories.toMutableMap())
    generators.applyRootPrefix(rootPath)
    return generators
  }
}
