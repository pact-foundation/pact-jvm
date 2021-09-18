package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.InvalidMatcherException
import au.com.dius.pact.core.matchers.UrlMatcherSupport
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.MockServerURLGenerator
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.RegexGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.expressions.DataType.Companion.from
import au.com.dius.pact.core.support.json.JsonValue
import com.mifmif.common.regex.Generex
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@Suppress("TooManyFunctions", "SpreadOperator")
open class PactDslJsonRootValue : DslPart("", "") {
  var value: Any? = null

  override fun putObjectPrivate(obj: DslPart) {
    throw UnsupportedOperationException()
  }

  override fun putArrayPrivate(obj: DslPart) {
    throw UnsupportedOperationException()
  }

  override var body: JsonValue
    get() = toJson(value)
    set(body) {
      value = body
    }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun array(name: String): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun array(): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun closeArray(): DslPart? {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachLike(name: String): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachLike(name: String, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachLike(numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachLike(name: String, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachLike(): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachLike(`object`: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minArrayLike(name: String, size: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minArrayLike(size: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minArrayLike(size: Int, obj: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minArrayLike(size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun maxArrayLike(name: String, size: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun maxArrayLike(size: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun maxArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun maxArrayLike(size: Int, obj: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun maxArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun maxArrayLike(size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minMaxArrayLike(minSize: Int, maxSize: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minMaxArrayLike(minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun minMaxArrayLike(minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonBody for objects")
  override fun `object`(name: String): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS)
  }

  @Deprecated("Use PactDslJsonBody for objects")
  override fun `object`(): PactDslJsonBody {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS)
  }

  @Deprecated("Use PactDslJsonBody for objects")
  override fun closeObject(): DslPart? {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS)
  }

  override fun close(): DslPart {
    matchers.applyMatcherRootPrefix("$")
    generators.applyRootPrefix("$")
    return this
  }

  fun setMatcher(matcher: MatchingRule) {
    matchers.addRule(matcher)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayLike(name: String): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayLike(numberExamples: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMaxLike(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMaxLike(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMaxLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMaxLike(numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinLike(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinLike(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinLike(numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinMaxLike(name: String, minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinMaxLike(minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinMaxLike(
    name: String,
    numberExamples: Int,
    minSize: Int,
    maxSize: Int
  ): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayWithMinMaxLike(numberExamples: Int, minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayLike(name: String, numberExamples: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun eachArrayLike(): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedArray(name: String): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedArray(): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedMinArray(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedMinArray(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedMaxArray(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedMaxArray(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedMinMaxArray(name: String, minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  @Deprecated("Use PactDslJsonArray for arrays")
  override fun unorderedMinMaxArray(minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS)
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  override fun matchUrl(basePath: String?, vararg pathFragments: Any): PactDslJsonRootValue {
    val urlMatcher = UrlMatcherSupport(basePath, listOf(*pathFragments))
    val value = PactDslJsonRootValue()
    val exampleValue = urlMatcher.getExampleValue()
    value.value = exampleValue
    val regexExpression = urlMatcher.getRegexExpression()
    value.setMatcher(value.regexp(regexExpression))
    if (StringUtils.isEmpty(basePath)) {
      value.generators.addGenerator(Category.BODY, "", MockServerURLGenerator(exampleValue, regexExpression))
    }
    return value
  }

  override fun matchUrl(name: String, basePath: String?, vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException(
      "URL matcher with an attribute name is not supported. " +
        "Use matchUrl(String basePath, Object... pathFragments)")
  }

  override fun matchUrl2(name: String, vararg pathFragments: Any): PactDslJsonBody {
    throw UnsupportedOperationException(
      "URL matcher with an attribute name is not supported. " +
        "Use matchUrl2(Object... pathFragments)")
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. Base path from the mock server
   * will be used.
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  override fun matchUrl2(vararg pathFragments: Any): DslPart {
    return matchUrl(null, *pathFragments)
  }

  override fun arrayContaining(name: String): DslPart {
    throw UnsupportedOperationException("arrayContaining is not supported for PactDslJsonRootValue")
  }

  override fun toString(): String {
    return this.body.serialise()
  }

  companion object {
    private const val USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS = "Use PactDslJsonArray for arrays"
    private const val USE_PACT_DSL_JSON_BODY_FOR_OBJECTS = "Use PactDslJsonBody for objects"

    /**
     * Value that can be any string
     */
    @JvmStatic
    fun stringType(): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomStringGenerator(20))
      value.value = "string"
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that can be any string
     *
     * @param example example value to use for generated bodies
     */
    @JvmStatic
    fun stringType(example: String): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = example
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that can be any number
     */
    @JvmStatic
    fun numberType(): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomIntGenerator(0, Int.MAX_VALUE))
      value.value = 100
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that can be any number
     * @param number example number to use for generated bodies
     */
    @JvmStatic
    fun numberType(number: Number): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = number
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that must be an integer
     */
    @JvmStatic
    fun integerType(): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomIntGenerator(0, Int.MAX_VALUE))
      value.value = 100
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
      return value
    }

    /**
     * Value that must be an integer
     * @param number example integer value to use for generated bodies
     */
    @JvmStatic
    fun integerType(number: Long): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = number
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
      return value
    }

    /**
     * Value that must be an integer
     * @param number example integer value to use for generated bodies
     */
    @JvmStatic
    fun integerType(number: Int): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = number
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
      return value
    }

    /**
     * Value that must be a decimal value
     */
    @JvmStatic
    fun decimalType(): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomDecimalGenerator(10))
      value.value = 100
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
      return value
    }

    /**
     * Value that must be a decimalType value
     * @param number example decimalType value
     */
    @JvmStatic
    fun decimalType(number: BigDecimal): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = number
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
      return value
    }

    /**
     * Value that must be a decimalType value
     * @param number example decimalType value
     */
    @JvmStatic
    fun decimalType(number: Double): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = number
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
      return value
    }

    /**
     * Value that must be a boolean
     * @param example example boolean to use for generated bodies
     */
    @JvmOverloads
    @JvmStatic
    fun booleanType(example: Boolean = true): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = example
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that must match the regular expression
     * @param regex regular expression
     * @param value example value to use for generated bodies
     */
    @JvmStatic
    fun stringMatcher(regex: String, value: String): PactDslJsonRootValue {
      if (!value.matches(Regex(regex))) {
        throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$regex\"")
      }
      val rootValue = PactDslJsonRootValue()
      rootValue.value = value
      rootValue.setMatcher(RegexMatcher(regex, value))
      return rootValue
    }

    /**
     * Value that must match the regular expression
     * @param regex regular expression
     */
    @Deprecated("Use the version that takes an example value")
    @JvmStatic
    fun stringMatcher(regex: String): PactDslJsonRootValue {
      val rootValue = PactDslJsonRootValue()
      rootValue.generators.addGenerator(Category.BODY, "", RegexGenerator(regex))
      rootValue.value = Generex(regex).random()
      rootValue.setMatcher(rootValue.regexp(regex))
      return rootValue
    }

    /**
     * Value that must match the given timestamp format
     * @param format timestamp format
     */
    @JvmOverloads
    @Deprecated("use datetime")
    @JvmStatic
    fun timestamp(format: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", DateTimeGenerator(format))
      val instance = FastDateFormat.getInstance(format)
      value.value = instance.format(Date(DATE_2000))
      value.setMatcher(value.matchTimestamp(format))
      return value
    }

    /**
     * Value that must match the given timestamp format
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     */
    @Deprecated("use datetime")
    @JvmStatic
    fun timestamp(format: String, example: Date): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.value = instance.format(example)
      value.setMatcher(value.matchTimestamp(format))
      return value
    }

    /**
     * Value that must match the given timestamp format
     * @param format timestamp format
     */
    @JvmOverloads
    @JvmStatic
    fun datetime(format: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", DateTimeGenerator(format))
      val instance = FastDateFormat.getInstance(format)
      value.value = instance.format(Date(DATE_2000))
      value.setMatcher(value.matchTimestamp(format))
      return value
    }

    /**
     * Value that must match the given timestamp format
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     */
    @JvmStatic
    fun datetime(format: String, example: Date): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.value = instance.format(example)
      value.setMatcher(value.matchTimestamp(format))
      return value
    }

    /**
     * Value that must match the provided date format
     * @param format date format to match
     */
    @JvmOverloads
    @JvmStatic
    fun date(format: String = DateFormatUtils.ISO_DATE_FORMAT.pattern): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", DateGenerator(format))
      value.value = instance.format(Date(DATE_2000))
      value.setMatcher(value.matchDate(format))
      return value
    }

    /**
     * Value that must match the provided date format
     * @param format date format to match
     * @param example example date to use for generated values
     */
    @JvmStatic
    fun date(format: String, example: Date): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.value = instance.format(example)
      value.setMatcher(value.matchDate(format))
      return value
    }

    /**
     * Value that must match the given time format
     * @param format time format to match
     */
    @JvmOverloads
    @JvmStatic
    fun time(format: String = DateFormatUtils.ISO_TIME_FORMAT.pattern): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", TimeGenerator(format))
      value.value = instance.format(Date(DATE_2000))
      value.setMatcher(value.matchTime(format))
      return value
    }

    /**
     * Value that must match the given time format
     * @param format time format to match
     * @param example example time to use for generated bodies
     */
    @JvmStatic
    fun time(format: String, example: Date): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.value = instance.format(example)
      value.setMatcher(value.matchTime(format))
      return value
    }

    /**
     * Value that must be an IP4 address
     */
    @JvmStatic
    fun ipAddress(): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.value = "127.0.0.1"
      value.setMatcher(value.regexp("(\\d{1,3}\\.)+\\d{1,3}"))
      return value
    }

    /**
     * Value that must be a numeric identifier
     */
    @JvmStatic
    fun id(): PactDslJsonRootValue {
      return numberType()
    }

    /**
     * Value that must be a numeric identifier
     * @param id example id to use for generated bodies
     */
    @JvmStatic
    fun id(id: Long): PactDslJsonRootValue {
      return numberType(id)
    }

    /**
     * Value that must be encoded as a hexadecimal value
     */
    @JvmStatic
    fun hexValue(): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomHexadecimalGenerator(10))
      value.value = "1234a"
      value.setMatcher(value.regexp("[0-9a-fA-F]+"))
      return value
    }

    /**
     * Value that must be encoded as a hexadecimal value
     * @param hexValue example value to use for generated bodies
     */
    @JvmStatic
    fun hexValue(hexValue: String): PactDslJsonRootValue {
      if (!hexValue.matches(HEXADECIMAL)) {
        throw InvalidMatcherException("Example \"$hexValue\" is not a hexadecimal value")
      }
      val value = PactDslJsonRootValue()
      value.value = hexValue
      value.setMatcher(value.regexp("[0-9a-fA-F]+"))
      return value
    }

    /**
     * Value that must be encoded as an UUID
     */
    @JvmStatic
    fun uuid(): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", UuidGenerator())
      value.value = "e2490de5-5bd3-43d5-b7c4-526e33f71304"
      value.setMatcher(value.regexp(UUID_REGEX.pattern))
      return value
    }

    /**
     * Value that must be encoded as an UUID
     * @param uuid example UUID to use for generated bodies
     */
    @JvmStatic
    fun uuid(uuid: UUID): PactDslJsonRootValue {
      return uuid(uuid.toString())
    }

    /**
     * Value that must be encoded as an UUID
     * @param uuid example UUID to use for generated bodies
     */
    @JvmStatic
    fun uuid(uuid: String): PactDslJsonRootValue {
      if (!uuid.matches(UUID_REGEX)) {
        throw InvalidMatcherException("Example \"$uuid\" is not an UUID")
      }
      val value = PactDslJsonRootValue()
      value.value = uuid
      value.setMatcher(value.regexp(UUID_REGEX.pattern))
      return value
    }

    /**
     * Combine all the matchers using AND
     * @param example Attribute example value
     * @param rules Matching rules to apply
     */
    @JvmStatic
    fun and(example: Any?, vararg rules: MatchingRule): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      if (example != null) {
        value.value = example
      } else {
        value.value = JSONObject.NULL
      }
      value.matchers.setRules("", MatchingRuleGroup(mutableListOf(*rules), RuleLogic.AND))
      return value
    }

    /**
     * Combine all the matchers using OR
     * @param example Attribute name
     * @param rules Matching rules to apply
     */
    @JvmStatic
    fun or(example: Any?, vararg rules: MatchingRule): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      if (example != null) {
        value.value = example
      } else {
        value.value = JSONObject.NULL
      }
      value.matchers.setRules("", MatchingRuleGroup(mutableListOf(*rules), RuleLogic.OR))
      return value
    }

    /**
     * Adds a value that will have it's value injected from the provider state
     * @param expression Expression to be evaluated from the provider state
     * @param example Example value to be used in the consumer test
     */
    @JvmStatic
    fun valueFromProviderState(expression: String, example: Any?): PactDslJsonRootValue {
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", ProviderStateGenerator(expression, from(example)))
      value.value = example
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Date value generated from an expression.
     * @param expression Date expression
     * @param format Date format to use
     */
    @JvmOverloads
    @JvmStatic
    fun dateExpression(
      expression: String,
      format: String = DateFormatUtils.ISO_DATE_FORMAT.pattern
    ): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", DateGenerator(format, expression))
      value.value = instance.format(Date(DATE_2000))
      value.setMatcher(value.matchDate(format))
      return value
    }

    /**
     * Time value generated from an expression.
     * @param expression Time expression
     * @param format Time format to use
     */
    @JvmOverloads
    @JvmStatic
    fun timeExpression(
      expression: String,
      format: String = DateFormatUtils.ISO_TIME_NO_T_FORMAT.pattern
    ): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", TimeGenerator(format, expression))
      value.value = instance.format(Date(DATE_2000))
      value.setMatcher(value.matchTime(format))
      return value
    }

    /**
     * Datetime value generated from an expression.
     * @param expression Datetime expression
     * @param format Datetime format to use
     */
    @JvmOverloads
    @JvmStatic
    fun datetimeExpression(
      expression: String,
      format: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern
    ): PactDslJsonRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslJsonRootValue()
      value.generators.addGenerator(Category.BODY, "", DateTimeGenerator(format, expression))
      value.value = instance.format(Date(DATE_2000))
      value.setMatcher(value.matchTimestamp(format))
      return value
    }
  }
}
