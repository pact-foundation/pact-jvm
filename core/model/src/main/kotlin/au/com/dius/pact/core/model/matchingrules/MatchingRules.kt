package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.generators.ArrayContainsGenerator
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.NullGenerator
import au.com.dius.pact.core.model.generators.lookupGenerator
import au.com.dius.pact.core.model.matchingrules.expressions.MatchingRuleDefinition
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.map
import mu.KLogging
import java.lang.RuntimeException

/**
 * Logic to use to combine rules
 */
enum class RuleLogic {
  AND, OR
}

/**
 * Matching rule
 */
interface MatchingRule {
  /**
   * Converts this rule into a Map that can be serialised to JSON
   */
  fun toMap(spec: PactSpecVersion): Map<String, Any?>

  /**
   * If this rule can be applied to the content type
   */
  fun canMatch(contentType: ContentType): Boolean = false

  /**
   * Validates this rule against the Pact specification version
   */
  fun validateForVersion(pactVersion: PactSpecVersion): List<String>

  /**
   * Any generators associated with this matching rule
   */
  fun buildGenerators(context: Map<String, Any>): List<Generator> = listOf()

  /**
   * If this matching rule has any associated generators
   */
  fun hasGenerators(): Boolean = false

  /**
   * Returns the type name of the matching rule
   */
  val name: String

  /**
   * Returns the attributes of the matching rule
   */
  val attributes: Map<String, JsonValue>

  companion object : KLogging() {
    private const val MATCH = "match"
    private const val MIN = "min"
    private const val MAX = "max"
    private const val REGEX = "regex"
    private const val TIMESTAMP = "timestamp"
    private const val TIME = "time"
    private const val DATE = "date"

    @JvmStatic
    fun fromJson(json: JsonValue): MatchingRule {
      return if (json.isObject) {
        val j: JsonValue.Object = json.downcast()
        when {
          j.has(MATCH) -> create(Json.toString(j[MATCH]), j)
          j.has(REGEX) -> RegexMatcher(j[REGEX].asString()!!)
          j.has(MIN) -> MinTypeMatcher(j[MIN].asNumber()!!.toInt())
          j.has(MAX) -> MaxTypeMatcher(j[MAX].asNumber()!!.toInt())
          j.has(TIMESTAMP) -> TimestampMatcher(j[TIMESTAMP].asString()!!)
          j.has(TIME) -> TimeMatcher(j[TIME].asString()!!)
          j.has(DATE) -> DateMatcher(j[DATE].asString()!!)
          else -> {
            MatchingRuleGroup.logger.warn { "Unrecognised matcher definition $j, defaulting to equality matching" }
            EqualsMatcher
          }
        }
      } else {
        MatchingRuleGroup.logger.warn { "Unrecognised matcher definition $json, defaulting to equality matching" }
        EqualsMatcher
      }
    }

    fun create(type: String, values: JsonValue): MatchingRule {
      logger.trace { "MatchingRule.create($type, $values)" }
      return when (type) {
        REGEX -> RegexMatcher(values[REGEX].asString()!!)
        "equality" -> EqualsMatcher
        "null" -> NullMatcher
        "include" -> IncludeMatcher(values["value"].toString())
        "type" -> ruleForType(values)
        "number" -> NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
        "integer" -> NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)
        "decimal" -> NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
        "real" -> {
          MatchingRuleGroup.logger.warn { "The 'real' type matcher is deprecated, use 'decimal' instead" }
          NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
        }
        MIN -> MinTypeMatcher(values[MIN].asNumber()!!.toInt())
        MAX -> MaxTypeMatcher(values[MAX].asNumber()!!.toInt())
        TIMESTAMP, "datetime" ->
          if (values.has("format")) TimestampMatcher(values["format"].toString())
          else if (values.has("timestamp")) TimestampMatcher(values["timestamp"].toString())
          else TimestampMatcher()
        TIME ->
          if (values.has("format")) TimeMatcher(values["format"].toString())
          else if (values.has("time")) TimestampMatcher(values["time"].toString())
          else TimeMatcher()
        DATE ->
          if (values.has("format")) DateMatcher(values["format"].toString())
          else if (values.has("date")) TimestampMatcher(values["date"].toString())
          else DateMatcher()
        "values" -> ValuesMatcher
        "ignore-order" -> ruleForIgnoreOrder(values)
        "contentType", "content-type" -> ContentTypeMatcher(values["value"].toString())
        "arrayContains", "array-contains" -> when (val variants = values["variants"]) {
          is JsonValue.Array -> ArrayContainsMatcher(variants.values.mapIndexed { index, variant ->
            when (variant) {
              is JsonValue.Object -> Triple(
                variant["index"].asNumber()!!.toInt(),
                MatchingRuleCategory("body").fromJson(variant["rules"]),
                variant["generators"].asObject()?.entries?.mapValues {
                  lookupGenerator(it.value) ?: NullGenerator
                } ?: emptyMap()
              )
              else ->
                throw InvalidMatcherJsonException("Array contains matchers: variant $index is incorrectly formed")
            }
          })
          else -> throw InvalidMatcherJsonException("Array contains matchers should have a list of variants")
        }
        "boolean" -> BooleanMatcher
        "statusCode", "status-code" -> if (values["status"].isArray) {
          val asArray = values["status"].asArray()!!
          StatusCodeMatcher(HttpStatus.StatusCodes, asArray.map {
            if (it.isNumber) {
              it.asNumber()!!.toInt()
            } else {
              throw InvalidMatcherJsonException(
                "Status code matcher of type StatusCodes must have an array of integers, got $it"
              )
            }
          })
        } else {
          StatusCodeMatcher(HttpStatus.fromJson(values["status"]))
        }
        "notEmpty", "not-empty" -> NotEmptyMatcher
        "semver" -> SemverMatcher
        "eachKey", "each-key" -> {
          val generator = if (values.has("generator")) {
            lookupGenerator(values["generator"])
          } else {
            null
          }

          EachKeyMatcher(MatchingRuleDefinition(Json.toString(values["value"]), values["rules"].asArray()!!.map {
            fromJson(it)
          }, generator))
        }
        "eachValue", "each-value" -> {
          val generator = if (values.has("generator")) {
            lookupGenerator(values["generator"])
          } else {
            null
          }

          EachValueMatcher(MatchingRuleDefinition(Json.toString(values["value"]), values["rules"].asArray()!!.map {
            fromJson(it)
          }, generator))
        }
        else -> {
          MatchingRuleGroup.logger.warn { "Unrecognised matcher ${values[MATCH]}, defaulting to equality matching" }
          EqualsMatcher
        }
      }
    }

    private fun ruleForType(map: JsonValue): MatchingRule {
      return if (map is JsonValue.Object) {
        if (map.has(MIN) && map.has(MAX)) {
          MinMaxTypeMatcher(map[MIN].asNumber()!!.toInt(), map[MAX].asNumber()!!.toInt())
        } else if (map.has(MIN)) {
          MinTypeMatcher(map[MIN].asNumber()!!.toInt())
        } else if (map.has(MAX)) {
          MaxTypeMatcher(map[MAX].asNumber()!!.toInt())
        } else {
          TypeMatcher
        }
      } else {
        TypeMatcher
      }
    }

    private fun ruleForIgnoreOrder(map: JsonValue): MatchingRule {
      return  if (map is JsonValue.Object) {
        if (map.has(MIN) && map.has(MAX)) {
          MinMaxEqualsIgnoreOrderMatcher(map[MIN].asNumber()!!.toInt(), map[MAX].asNumber()!!.toInt())
        } else if (map.has(MIN)) {
          MinEqualsIgnoreOrderMatcher(map[MIN].asNumber()!!.toInt())
        } else if (map.has(MAX)) {
          MaxEqualsIgnoreOrderMatcher(map[MAX].asNumber()!!.toInt())
        } else {
          EqualsIgnoreOrderMatcher
        }
      } else {
        EqualsIgnoreOrderMatcher
      }
    }
  }
}

/**
 * Matching Rule for dates
 */
data class DateMatcher @JvmOverloads constructor(val format: String = "yyyy-MM-dd") : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "date", "date" to format)
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Date matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "date"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("format" to JsonValue.StringValue(format))
}

/**
 * Matching rule for equality
 */
object EqualsMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "equality")
  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion) = emptyList<String>()

  override val name: String
    get() = "equality"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}

/**
 * Matcher for a substring in a string
 */
data class IncludeMatcher(val value: String) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "include", "value" to value)
  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Include matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "include"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("value" to JsonValue.StringValue(value))
}

/**
 * Type matching with a maximum size
 */
data class MaxTypeMatcher(val max: Int) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "type", "max" to max)
  override fun validateForVersion(pactVersion: PactSpecVersion) = emptyList<String>()

  override val name: String
    get() = "max-type"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("max" to JsonValue.Integer(max))
}

/**
 * Type matcher with a minimum size and maximum size
 */
data class MinMaxTypeMatcher(val min: Int, val max: Int) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "type", "min" to min, "max" to max)
  override fun validateForVersion(pactVersion: PactSpecVersion) = emptyList<String>()

  override val name: String
    get() = "min-max-type"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("min" to JsonValue.Integer(min), "max" to JsonValue.Integer(max))
}

/**
 * Type matcher with a minimum size
 */
data class MinTypeMatcher(val min: Int) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "type", "min" to min)
  override fun validateForVersion(pactVersion: PactSpecVersion) = emptyList<String>()

  override val name: String
    get() = "min-type"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("min" to JsonValue.Integer(min))
}

/**
 * Type matching for numbers
 */
data class NumberTypeMatcher(val numberType: NumberType) : MatchingRule {
  enum class NumberType {
    NUMBER,
    INTEGER,
    DECIMAL
  }

  override fun toMap(spec: PactSpecVersion) = if (spec >= PactSpecVersion.V3) {
    mapOf("match" to numberType.name.lowercase())
  } else {
    TypeMatcher.toMap(spec)
  }

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Number matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "number"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}

/**
 * Type matching for booleans
 */
object BooleanMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = when {
    spec == PactSpecVersion.V4 -> mapOf("match" to "boolean")
    else -> TypeMatcher.toMap(spec)
  }

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> = listOf()

  override val name: String
    get() = "boolean"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}

/**
 * Regular Expression Matcher
 */
data class RegexMatcher @JvmOverloads constructor (val regex: String, val example: String? = null) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "regex", "regex" to regex)
  override fun validateForVersion(pactVersion: PactSpecVersion) = emptyList<String>()

  override val name: String
    get() = "regex"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("regex" to JsonValue.StringValue(regex))
}

/**
 * Matcher for time values
 */
data class TimeMatcher @JvmOverloads constructor(val format: String = "HH:mm:ss") : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "time", "time" to format)
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Time matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "time"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("format" to JsonValue.StringValue(format))
}

/**
 * Matcher for time values
 */
data class TimestampMatcher @JvmOverloads constructor(val format: String = "yyyy-MM-dd HH:mm:ssZZZZZ") : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "timestamp", "timestamp" to format)
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("DateTime matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "datetime"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("format" to JsonValue.StringValue(format))
}

/**
 * Matcher for types
 */
object TypeMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "type")
  override fun validateForVersion(pactVersion: PactSpecVersion) = emptyList<String>()

  override val name: String
    get() = "type"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}

/**
 * Matcher for null values
 */
object NullMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "null")
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Null matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "null"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}

/**
 * Matcher for values in a map, ignoring the keys
 */
object ValuesMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "values")
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Values matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "values"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}

/**
 * Content type matcher. Matches the content type of binary data
 */
data class ContentTypeMatcher(val contentType: String) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "contentType", "value" to contentType)
  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Content Type matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "content-type"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("value" to JsonValue.StringValue(contentType))
}

/**
 * Matcher for ignoring order of elements in array.
 *
 * This matcher will default to equality matching for non-array items.
 */
object EqualsIgnoreOrderMatcher : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "ignore-order")
  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V4) {
      listOf("Ignore Order matchers can only be used with Pact specification versions >= V4")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "ignore-order"
  override val attributes: Map<String, JsonValue>
    get() = emptyMap()
}

/**
 * Ignore order matcher with a minimum size.
 *
 * This matcher will default to equality matching for non-array items.
 */
data class MinEqualsIgnoreOrderMatcher(val min: Int) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "ignore-order", "min" to min)
  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V4) {
      listOf("Ignore Order matchers can only be used with Pact specification versions >= V4")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "min-ignore-order"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("min" to JsonValue.Integer(min))
}

/**
 * Ignore order matching with a maximum size.
 *
 * This matcher will default to equality matching for non-array items.
 */
data class MaxEqualsIgnoreOrderMatcher(val max: Int) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "ignore-order", "max" to max)
  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V4) {
      listOf("Ignore Order matchers can only be used with Pact specification versions >= V4")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "max-ignore-order"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("max" to JsonValue.Integer(max))
}

/**
 * Ignore order matcher with a minimum size and maximum size.
 *
 * This matcher will default to equality matching for non-array items.
 */
data class MinMaxEqualsIgnoreOrderMatcher(val min: Int, val max: Int) : MatchingRule {
  override fun toMap(spec: PactSpecVersion) = mapOf("match" to "ignore-order", "min" to min, "max" to max)
  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V4) {
      listOf("Ignore Order matchers can only be used with Pact specification versions >= V4")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "min-max-ignore-order"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("min" to JsonValue.Integer(min), "max" to JsonValue.Integer(max))
}

/**
 * Match array items in any order against a list of variants
 */
data class ArrayContainsMatcher(
  val variants: List<Triple<Int, MatchingRuleCategory, Map<String, Generator>>>
) : MatchingRule {
  override fun toMap(spec: PactSpecVersion): Map<String, Any?> {
    return mapOf("match" to "arrayContains", "variants" to variants.map { (index, rules, generators) ->
      mapOf(
        "index" to index,
        "rules" to rules.toMap(spec),
        "generators" to generators.mapValues { it.value.toMap(spec) }
      )
    })
  }

  override fun canMatch(contentType: ContentType) = true
  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V3) {
      listOf("Array contains matchers can only be used with Pact specification versions >= V3")
    } else {
      listOf()
    }
  }

  override fun hasGenerators() = true

  override fun buildGenerators(context: Map<String, Any>): List<Generator> {
    return listOf(ArrayContainsGenerator(variants))
  }

  override val name: String
    get() = "array-contains"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("variants" to JsonValue.Array(variants.map { (variant, rules, gens) ->
      JsonValue.Array(mutableListOf(
        JsonValue.Integer(variant),
        Json.toJson(rules.toMap(PactSpecVersion.V4)),
        JsonValue.Object(gens.entries.associate {
          it.key to Json.toJson(it.value.toMap(PactSpecVersion.V4))
        }.toMutableMap())
      ))
    }.toMutableList()))
}


/**
 * Matcher for HTTP status codes
 */
data class StatusCodeMatcher(val statusType: HttpStatus, val values: List<Int> = emptyList()) : MatchingRule {
  override fun toMap(spec: PactSpecVersion): Map<String, Any?> {
    return mapOf("match" to "statusCode", "status" to statusType.toJson(values))
  }

  override fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return if (pactVersion < PactSpecVersion.V4) {
      listOf("Status code matchers can only be used with Pact specification versions >= V4")
    } else {
      listOf()
    }
  }

  override val name: String
    get() = "status-code"
  override val attributes: Map<String, JsonValue>
    get() = mapOf("status" to Json.toJson(statusType.toJson(values)))
}

data class MatchingRuleGroup @JvmOverloads constructor(
  val rules: MutableList<MatchingRule> = mutableListOf(),
  val ruleLogic: RuleLogic = RuleLogic.AND,
  val cascaded: Boolean = false
) {
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any?> {
    return if (pactSpecVersion < PactSpecVersion.V3) {
      rules.first().toMap(pactSpecVersion)
    } else {
      mapOf("matchers" to rules.map { it.toMap(pactSpecVersion) }, "combine" to ruleLogic.name)
    }
  }

  fun canMatch(contentType: ContentType) = rules.all { it.canMatch(contentType) }

  /**
   * Validates all the rules in this group against the Pact specification version
   */
  fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    return rules.flatMap { it.validateForVersion(pactVersion) }
  }

  companion object : KLogging() {
    @JvmStatic
    fun fromJson(json: JsonValue): MatchingRuleGroup {
      var ruleLogic = RuleLogic.AND
      val rules = mutableListOf<MatchingRule>()

      if (json.isObject) {
        val groupJson: JsonValue.Object = json.downcast()
        if (groupJson.has("combine")) {
          try {
            val value = groupJson["combine"].asString()
            if (value !=  null) {
              ruleLogic = RuleLogic.valueOf(value)
            }
          } catch (e: IllegalArgumentException) {
            logger.warn { "${groupJson["combine"]} is not a valid matcher rule logic value" }
          }
        }

        if (json.has("matchers")) {
          val matchers = json["matchers"]
          if (matchers is JsonValue.Array) {
            matchers.values.forEach {
              if (it.isObject) {
                rules.add(MatchingRule.fromJson(it))
              }
            }
          } else {
            logger.warn { "$json does not contain a list of matchers" }
          }
        }
      }

      return MatchingRuleGroup(rules, ruleLogic)
    }

    private const val MATCH = "match"
    private const val MIN = "min"
    private const val MAX = "max"
    private const val REGEX = "regex"
    private const val TIMESTAMP = "timestamp"
    private const val TIME = "time"
    private const val DATE = "date"

    private fun mapEntryToInt(map: Map<String, Any?>, field: String) =
      if (map[field] is Int) map[field] as Int
      else Integer.parseInt(map[field]!!.toString())
  }
}

class InvalidMatcherJsonException(message: String) : RuntimeException(message)

/**
 * Collection of all matching rules
 */
interface MatchingRules {
  /**
   * Get all the rules for a given category
   */
  fun rulesForCategory(category: String): MatchingRuleCategory

  /**
   * Adds a new category with the given name to the collection
   */
  fun addCategory(category: String): MatchingRuleCategory

  /**
   * Adds the category to the collection
   */
  fun addCategory(category: MatchingRuleCategory): MatchingRuleCategory

  /**
   * If the matching rules are empty
   */
  fun isEmpty(): Boolean

  /**
   * If the matching rules is not empty
   */
  fun isNotEmpty(): Boolean

  /**
   * If the matching rules has the named category
   */
  fun hasCategory(category: String): Boolean

  /**
   * Returns the set of all categories that rules are defined for
   */
  fun getCategories(): Set<String>

  /**
   * Converts these rules into a Map that can be serialised to JSON
   */
  fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any?>

  /**
   * Create a new copy of the matching rules
   */
  fun copy(): MatchingRules

  /** Validates the matching rules against the specification version */
  fun validateForVersion(pactVersion: PactSpecVersion): List<String>

  /** Creates a copy of the matching rules with a category renamed */
  fun rename(oldCategory: String, newCategory: String): MatchingRules
}
