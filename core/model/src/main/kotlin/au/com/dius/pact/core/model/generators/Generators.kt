package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.InvalidPactException
import au.com.dius.pact.core.model.JsonUtils.queryObjectGraph
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.lessThan
import au.com.dius.pact.core.model.parsePath
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.orNull
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Locale

private val logger = KotlinLogging.logger {}

enum class Category {
  METHOD, PATH, HEADER, QUERY, BODY, STATUS, METADATA, CONTENT
}

interface ContentTypeHandler {
  fun processBody(value: OptionalBody, fn: (QueryResult) -> Unit): OptionalBody
  fun applyKey(body: QueryResult, key: String, generator: Generator, context: MutableMap<String, Any>)
}

val contentTypeHandlers: MutableMap<String, ContentTypeHandler> = mutableMapOf(
  "application/json" to JsonContentTypeHandler,
  "application/x-www-form-urlencoded" to FormUrlEncodedContentTypeHandler
)

fun setupDefaultContentTypeHandlers() {
  contentTypeHandlers.clear()
  contentTypeHandlers["application/json"] = JsonContentTypeHandler
  contentTypeHandlers["application/x-www-form-urlencoded"] = FormUrlEncodedContentTypeHandler
}

interface QueryResult {
  var value: Any?
  val key: Any?
  val path: List<String>
}
data class JsonQueryResult(
  var jsonValue: JsonValue?,
  override val key: Any? = null,
  val parent: JsonValue? = null,
  override val path: List<String> = emptyList()
): QueryResult {
  override var value: Any?
    get() = jsonValue
    set(value) { jsonValue = Json.toJson(value) }
}

object JsonContentTypeHandler : ContentTypeHandler {
  override fun processBody(value: OptionalBody, fn: (QueryResult) -> Unit): OptionalBody {
    val bodyJson = JsonQueryResult(JsonParser.parseString(value.valueAsString()))
    fn.invoke(bodyJson)
    return OptionalBody.body(bodyJson.jsonValue.orNull().serialise()
      .toByteArray(value.contentType.asCharset()), ContentType.JSON)
  }

  override fun applyKey(body: QueryResult, key: String, generator: Generator, context: MutableMap<String, Any>) {
    val pathExp = parsePath(key)
    queryObjectGraph(pathExp.iterator(), body as JsonQueryResult) { (_, valueKey, parent) ->
      when (parent) {
        is JsonValue.Object ->
          parent[valueKey.toString()] = Json.toJson(generator.generate(context, parent[valueKey.toString()]))
        is JsonValue.Array ->
          parent[valueKey as Int] = Json.toJson(generator.generate(context, parent[valueKey]))
        else -> body.value = Json.toJson(generator.generate(context, body.value))
      }
    }
  }
}

enum class GeneratorTestMode {
  Consumer, Provider
}

data class Generators(val categories: MutableMap<Category, MutableMap<String, Generator>> = mutableMapOf()) {

  companion object {

    @JvmStatic fun fromJson(json: JsonValue?): Generators {
      val generators = Generators()

      if (json is JsonValue.Object) {
        json.entries.forEach { (key, generatorJson) ->
          try {
            when (val category = Category.valueOf(key.uppercase(Locale.getDefault()))) {
              Category.STATUS, Category.PATH, Category.METHOD -> if (generatorJson.has("type")) {
                val generator = lookupGenerator(generatorJson.asObject())
                if (generator != null) {
                  generators.addGenerator(category, generator = generator)
                } else {
                  logger.warn { "Ignoring invalid generator config '$generatorJson'" }
                }
              } else {
                logger.warn { "Ignoring invalid generator config '$generatorJson.obj'" }
              }
              else -> generatorJson.asObject()?.entries?.forEach { (generatorKey, generatorValue) ->
                if (generatorValue is JsonValue.Object && generatorValue.has("type")) {
                  val generator = lookupGenerator(generatorValue)
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

    fun applyGenerators(
      generators: Map<String, Generator>,
      mode: GeneratorTestMode,
      closure: (String, Generator) -> Unit
    ) {
      for ((key, generator) in generators) {
        if (generator.correspondsToMode(mode)) {
          closure.invoke(key, generator)
        }
      }
    }

    fun applyBodyGenerators(
      generators: Map<String, Generator>,
      body: OptionalBody,
      contentType: ContentType,
      context: MutableMap<String, Any>,
      mode: GeneratorTestMode
    ): OptionalBody {
      val handler = findContentTypeHandler(contentType)
      return handler?.processBody(body) { bodyResult: QueryResult ->
        for ((key, generator) in generators) {
          if (generator.correspondsToMode(mode)) {
            handler.applyKey(bodyResult, key, generator, context)
          }
        }
      } ?: body
    }

    private fun findContentTypeHandler(contentType: ContentType): ContentTypeHandler? {
      val updatedContentType = getUpdatedContentType(contentType)
      val typeHandler = contentTypeHandlers[updatedContentType.getBaseType()]
      return if (typeHandler != null) {
        typeHandler
      } else {
        val supertype = updatedContentType.getSupertype()
        if (supertype != null) {
          findContentTypeHandler(supertype)
        } else {
          null
        }
      }
    }

    private fun getUpdatedContentType(contentType: ContentType): ContentType {
      if (contentType.isJson())
        return ContentType.JSON
      return contentType
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

  fun categoryFor(category: Category) = categories[category]

  fun applyGenerator(category: Category, mode: GeneratorTestMode, closure: (String, Generator) -> Unit) {
    if (categories.containsKey(category) && categories[category] != null) {
      val categoryValues = categories[category]
      if (categoryValues != null) {
        applyGenerators(categoryValues, mode, closure)
      }
    }
  }

  fun applyBodyGenerators(
    body: OptionalBody,
    contentType: ContentType,
    context: MutableMap<String, Any>,
    mode: GeneratorTestMode
  ): OptionalBody {
    return when (body.state) {
      OptionalBody.State.EMPTY, OptionalBody.State.MISSING, OptionalBody.State.NULL -> body
      OptionalBody.State.PRESENT -> if (categories[Category.BODY] != null) {
        applyBodyGenerators(categories[Category.BODY]!!, body, contentType, context, mode)
      } else {
        body
      }
    }
  }

  /**
   * If there are no generators
   */
  fun isEmpty() = categories.isEmpty() || categories.all { it.value.isEmpty() }

  /**
   * If there are generators
   */
  fun isNotEmpty() = categories.isNotEmpty() && categories.any { it.value.isNotEmpty() }

  fun toMap(pactSpecVersion: PactSpecVersion?): Map<String, Any> {
    if (pactSpecVersion.lessThan(PactSpecVersion.V3)) {
      throw InvalidPactException("Generators are only supported with pact specification version 3+")
    }
    return categories.entries.associate { (key, value) ->
      when (key) {
        Category.METHOD, Category.PATH, Category.STATUS ->
          key.name.lowercase() to value[""]!!.toMap(pactSpecVersion)
        else -> key.name.lowercase() to value.entries.associate { (genKey, generator) ->
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

  fun validateForVersion(pactVersion: PactSpecVersion?): List<String> {
    return if (pactVersion.lessThan(PactSpecVersion.V3) && categories.any { it.value.isNotEmpty() }) {
      listOf("Generators can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  fun addGenerators(category: Category, generators: Map<String, Generator>) {
    categories[category] = generators.toMutableMap()
  }
}
