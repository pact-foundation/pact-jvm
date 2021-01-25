package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Abstract base class to support Object and Array JSON DSL builders
 */
@Suppress("TooManyFunctions")
abstract class DslPart {
  /**
   * Returns the parent of this part (object or array)
   * @return parent, or null if it is the root
   */
  val parent: DslPart?
  val rootPath: String
  val rootName: String
  var matchers = MatchingRuleCategory("body")
  var generators = Generators()
  protected var closed = false

  constructor(parent: DslPart?, rootPath: String, rootName: String) {
    this.parent = parent
    this.rootPath = rootPath
    this.rootName = rootName
  }

  constructor(rootPath: String, rootName: String) {
    parent = null
    this.rootPath = rootPath
    this.rootName = rootName
  }

  abstract fun putObjectPrivate(obj: DslPart)
  abstract fun putArrayPrivate(obj: DslPart)
  abstract var body: JsonValue

  /**
   * Field which is an array
   * @param name field name
   */
  abstract fun array(name: String): PactDslJsonArray

  /**
   * Element as an array
   */
  abstract fun array(): PactDslJsonArray

  /**
   * Array field where order is ignored
   * @param name field name
   */
  abstract fun unorderedArray(name: String): PactDslJsonArray

  /**
   * Array element where order is ignored
   */
  abstract fun unorderedArray(): PactDslJsonArray

  /**
   * Array field of min size where order is ignored
   * @param name field name
   * @param size minimum size
   */
  abstract fun unorderedMinArray(name: String, size: Int): PactDslJsonArray

  /**
   * Array element of min size where order is ignored
   * @param size minimum size
   */
  abstract fun unorderedMinArray(size: Int): PactDslJsonArray

  /**
   * Array field of max size where order is ignored
   * @param name field name
   * @param size maximum size
   */
  abstract fun unorderedMaxArray(name: String, size: Int): PactDslJsonArray

  /**
   * Array element of max size where order is ignored
   * @param size maximum size
   */
  abstract fun unorderedMaxArray(size: Int): PactDslJsonArray

  /**
   * Array field of min and max size where order is ignored
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun unorderedMinMaxArray(name: String, minSize: Int, maxSize: Int): PactDslJsonArray

  /**
   * Array element of min and max size where order is ignored
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun unorderedMinMaxArray(minSize: Int, maxSize: Int): PactDslJsonArray

  /**
   * Close of the previous array element
   */
  abstract fun closeArray(): DslPart?

  /**
   * Array field where each element must match the following object
   * @param name field name
   */
  abstract fun eachLike(name: String): PactDslJsonBody

  /**
   * Array field where each element must match the following object
   * @param name field name
   */
  abstract fun eachLike(name: String, obj: DslPart): PactDslJsonBody

  /**
   * Array element where each element of the array must match the following object
   */
  abstract fun eachLike(): PactDslJsonBody

  /**
   * Array element where each element of the array must match the provided object
   */
  abstract fun eachLike(obj: DslPart): PactDslJsonArray

  /**
   * Array field where each element must match the following object
   * @param name field name
   * @param numberExamples number of examples to generate
   */
  abstract fun eachLike(name: String, numberExamples: Int): PactDslJsonBody

  /**
   * Array element where each element of the array must match the following object
   * @param numberExamples number of examples to generate
   */
  abstract fun eachLike(numberExamples: Int): PactDslJsonBody

  /**
   * Array field with a minimum size and each element must match the following object
   * @param name field name
   * @param size minimum size
   */
  abstract fun minArrayLike(name: String, size: Int): PactDslJsonBody

  /**
   * Array element with a minimum size and each element of the array must match the following object
   * @param size minimum size
   */
  abstract fun minArrayLike(size: Int): PactDslJsonBody

  /**
   * Array field with a minimum size and each element must match the provided object
   * @param name field name
   * @param size minimum size
   */
  abstract fun minArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody

  /**
   * Array element with a minumum size and each element of the array must match the provided object
   * @param size minimum size
   */
  abstract fun minArrayLike(size: Int, obj: DslPart): PactDslJsonArray

  /**
   * Array field with a minumum size and each element must match the following object
   * @param name field name
   * @param size minimum size
   * @param numberExamples number of examples to generate
   */
  abstract fun minArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody

  /**
   * Array element with a minimum size and each element of the array must match the following object
   * @param size minimum size
   * @param numberExamples number of examples to generate
   */
  abstract fun minArrayLike(size: Int, numberExamples: Int): PactDslJsonBody

  /**
   * Array field with a maximum size and each element must match the following object
   * @param name field name
   * @param size maximum size
   */
  abstract fun maxArrayLike(name: String, size: Int): PactDslJsonBody

  /**
   * Array element with a maximum size and each element of the array must match the following object
   * @param size maximum size
   */
  abstract fun maxArrayLike(size: Int): PactDslJsonBody

  /**
   * Array field with a maximum size and each element must match the provided object
   * @param name field name
   * @param size maximum size
   */
  abstract fun maxArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody

  /**
   * Array element with a maximum size and each element of the array must match the provided object
   * @param size minimum size
   */
  abstract fun maxArrayLike(size: Int, obj: DslPart): PactDslJsonArray

  /**
   * Array field with a maximum size and each element must match the following object
   * @param name field name
   * @param size maximum size
   * @param numberExamples number of examples to generate
   */
  abstract fun maxArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody

  /**
   * Array element with a maximum size and each element of the array must match the following object
   * @param size maximum size
   * @param numberExamples number of examples to generate
   */
  abstract fun maxArrayLike(size: Int, numberExamples: Int): PactDslJsonBody

  /**
   * Array field with a minimum and maximum size and each element must match the following object
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int): PactDslJsonBody

  /**
   * Array field with a minimum and maximum size and each element must match the provided object
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonBody

  /**
   * Array element with a minimum and maximum size and each element of the array must match the following object
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun minMaxArrayLike(minSize: Int, maxSize: Int): PactDslJsonBody

  /**
   * Array element with a minimum and maximum size and each element of the array must match the provided object
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun minMaxArrayLike(minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonArray

  /**
   * Array field with a minimum and maximum size and each element must match the following object
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   * @param numberExamples number of examples to generate
   */
  abstract fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody

  /**
   * Array element with a minimum and maximum size and each element of the array must match the following object
   * @param minSize minimum size
   * @param maxSize maximum size
   * @param numberExamples number of examples to generate
   */
  abstract fun minMaxArrayLike(minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   */
  abstract fun eachArrayLike(name: String): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   */
  abstract fun eachArrayLike(): PactDslJsonArray

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param numberExamples number of examples to generate
   */
  abstract fun eachArrayLike(name: String, numberExamples: Int): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param numberExamples number of examples to generate
   */
  abstract fun eachArrayLike(numberExamples: Int): PactDslJsonArray

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param size Maximum size of the outer array
   */
  abstract fun eachArrayWithMaxLike(name: String, size: Int): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param size Maximum size of the outer array
   */
  abstract fun eachArrayWithMaxLike(size: Int): PactDslJsonArray

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param numberExamples number of examples to generate
   * @param size Maximum size of the outer array
   */
  abstract fun eachArrayWithMaxLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param numberExamples number of examples to generate
   * @param size Maximum size of the outer array
   */
  abstract fun eachArrayWithMaxLike(numberExamples: Int, size: Int): PactDslJsonArray

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param size Minimum size of the outer array
   */
  abstract fun eachArrayWithMinLike(name: String, size: Int): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param size Minimum size of the outer array
   */
  abstract fun eachArrayWithMinLike(size: Int): PactDslJsonArray

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param numberExamples number of examples to generate
   * @param size Minimum size of the outer array
   */
  abstract fun eachArrayWithMinLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param numberExamples number of examples to generate
   * @param size Minimum size of the outer array
   */
  abstract fun eachArrayWithMinLike(numberExamples: Int, size: Int): PactDslJsonArray

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun eachArrayWithMinMaxLike(name: String, minSize: Int, maxSize: Int): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun eachArrayWithMinMaxLike(minSize: Int, maxSize: Int): PactDslJsonArray

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param numberExamples number of examples to generate
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun eachArrayWithMinMaxLike(name: String, numberExamples: Int, minSize: Int,
                                       maxSize: Int): PactDslJsonArray

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param numberExamples number of examples to generate
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  abstract fun eachArrayWithMinMaxLike(numberExamples: Int, minSize: Int, maxSize: Int): PactDslJsonArray

  /**
   * Object field
   * @param name field name
   */
  abstract fun `object`(name: String): PactDslJsonBody

  /**
   * Object element
   */
  abstract fun `object`(): PactDslJsonBody

  /**
   * Close off the previous object
   * @return
   */
  abstract fun closeObject(): DslPart?

  protected fun regexp(regex: String): RegexMatcher {
    return RegexMatcher(regex)
  }

  protected fun matchTimestamp(format: String): TimestampMatcher {
    return TimestampMatcher(format)
  }

  protected fun matchDate(format: String): DateMatcher {
    return DateMatcher(format)
  }

  protected fun matchTime(format: String): TimeMatcher {
    return TimeMatcher(format)
  }

  protected fun matchMin(min: Int): MinTypeMatcher {
    return MinTypeMatcher(min)
  }

  protected fun matchMax(max: Int): MaxTypeMatcher {
    return MaxTypeMatcher(max)
  }

  protected fun matchMinMax(minSize: Int, maxSize: Int): MinMaxTypeMatcher {
    return MinMaxTypeMatcher(minSize, maxSize)
  }

  protected fun matchIgnoreOrder(): EqualsIgnoreOrderMatcher {
    return EqualsIgnoreOrderMatcher
  }

  protected fun matchMinIgnoreOrder(min: Int): MinEqualsIgnoreOrderMatcher {
    return MinEqualsIgnoreOrderMatcher(min)
  }

  protected fun matchMaxIgnoreOrder(max: Int): MaxEqualsIgnoreOrderMatcher {
    return MaxEqualsIgnoreOrderMatcher(max)
  }

  protected fun matchMinMaxIgnoreOrder(minSize: Int, maxSize: Int): MinMaxEqualsIgnoreOrderMatcher {
    return MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize)
  }

  protected fun includesMatcher(value: Any): IncludeMatcher {
    return IncludeMatcher(value.toString())
  }

  fun asBody(): PactDslJsonBody {
    return this as PactDslJsonBody
  }

  fun asArray(): PactDslJsonArray {
    return this as PactDslJsonArray
  }

  /**
   * This closes off the object graph build from the DSL in case any close[Object|Array] methods have not been called.
   * @return The root object of the object graph
   */
  abstract fun close(): DslPart?

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param name Attribute name
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  abstract fun matchUrl(name: String, basePath: String?, vararg pathFragments: Any): DslPart

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  abstract fun matchUrl(basePath: String?, vararg pathFragments: Any): DslPart

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. Base path from the mock server
   * will be used.
   * @param name Attribute name
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  abstract fun matchUrl2(name: String, vararg pathFragments: Any): DslPart

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. Base path from the mock server
   * * will be used.
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  abstract fun matchUrl2(vararg pathFragments: Any): DslPart

  /**
   * Matches the items in an array against a number of variants. Matching is successful if each variant
   * occurs once in the array. Variants may be objects containing matching rules.
   * @param name Attribute name
   */
  abstract fun arrayContaining(name: String): DslPart

  companion object {
    val HEXADECIMAL = Regex("[0-9a-fA-F]+")
    val IP_ADDRESS = Regex("(\\d{1,3}\\.)+\\d{1,3}")
    val UUID_REGEX = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    const val DATE_2000 = 949323600000L

    /**
     * Returns a regular expression matcher
     * @param regex Regex to match with
     * @param example Example value to use
     * @return
     */
    @JvmStatic
    fun regex(regex: String, example: String): RegexMatcher {
      return RegexMatcher(regex, example)
    }

    /**
     * Returns a regular expression matcher. Will generate random examples from the regex.
     * @param regex Regex to match with
     * @return
     */
    @JvmStatic
    fun regex(regex: String): RegexMatcher {
      return RegexMatcher(regex)
    }
  }
}
