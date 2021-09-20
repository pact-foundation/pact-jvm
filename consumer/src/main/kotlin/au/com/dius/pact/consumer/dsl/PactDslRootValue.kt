package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.InvalidMatcherException
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.expressions.DataType.Companion.from
import au.com.dius.pact.core.support.json.JsonValue
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

/**
 * Matcher to create a plain root matching strategy. Used with text/plain to match regex responses
 */
@Suppress("TooManyFunctions", "SpreadOperator")
open class PactDslRootValue : DslPart("", "") {
  override var body: JsonValue = JsonValue.Null

  override fun putObjectPrivate(obj: DslPart) {
    throw UnsupportedOperationException()
  }

  override fun putArrayPrivate(obj: DslPart) {
    throw UnsupportedOperationException()
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
  override fun eachLike(obj: DslPart): PactDslJsonArray {
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

  fun getValue(): String {
    return when (val body = this.body) {
      is JsonValue.StringValue -> body.toString()
      else -> body.serialise()
    }
  }

  fun setValue(value: Any?) {
    body = toJson(value)
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

  override fun matchUrl(name: String, basePath: String?, vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException("matchUrl is not currently supported for PactDslRootValue")
  }

  override fun matchUrl(basePath: String?, vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException("matchUrl is not currently supported for PactDslRootValue")
  }

  override fun matchUrl2(name: String, vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException("matchUrl2 is not currently supported for PactDslRootValue")
  }

  override fun matchUrl2(vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException("matchUrl2 is not currently supported for PactDslRootValue")
  }

  override fun arrayContaining(name: String): DslPart {
    throw UnsupportedOperationException("arrayContaining is not currently supported for PactDslRootValue")
  }

  override fun toString(): String {
    return getValue()
  }

  companion object {
    private const val USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS = "Use PactDslJsonArray for arrays"
    private const val USE_PACT_DSL_JSON_BODY_FOR_OBJECTS = "Use PactDslJsonBody for objects"

    /**
     * Value that can be any string
     */
    @JvmStatic
    fun stringType(): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomStringGenerator(20))
      value.setValue("string")
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that can be any string
     *
     * @param example example value to use for generated bodies
     */
    @JvmStatic
    fun stringType(example: String): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue(example)
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that can be any number
     */
    @JvmStatic
    fun numberType(): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomIntGenerator(0, Int.MAX_VALUE))
      value.setValue(100)
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that can be any number
     * @param number example number to use for generated bodies
     */
    @JvmStatic
    fun numberType(number: Number): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue(number)
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that must be an integer
     */
    @JvmStatic
    fun integerType(): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomIntGenerator(0, Int.MAX_VALUE))
      value.setValue(100)
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
      return value
    }

    /**
     * Value that must be an integer
     * @param number example integer value to use for generated bodies
     */
    @JvmStatic
    fun integerType(number: Long): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue(number)
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
      return value
    }

    /**
     * Value that must be an integer
     * @param number example integer value to use for generated bodies
     */
    @JvmStatic
    fun integerType(number: Int): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue(number)
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
      return value
    }

    /**
     * Value that must be a decimal value
     */
    @JvmStatic
    fun decimalType(): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomDecimalGenerator(10))
      value.setValue(100)
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
      return value
    }

    /**
     * Value that must be a decimalType value
     * @param number example decimalType value
     */
    @JvmStatic
    fun decimalType(number: BigDecimal): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue(number)
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
      return value
    }

    /**
     * Value that must be a decimalType value
     * @param number example decimalType value
     */
    @JvmStatic
    fun decimalType(number: Double): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue(number)
      value.setMatcher(NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
      return value
    }

    /**
     * Value that must be a boolean
     * @param example example boolean to use for generated bodies
     */
    @JvmOverloads
    @JvmStatic
    fun booleanType(example: Boolean = true): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue(example)
      value.setMatcher(TypeMatcher)
      return value
    }

    /**
     * Value that must match the regular expression
     * @param regex regular expression
     * @param value example value to use for generated bodies
     */
    @JvmStatic
    fun stringMatcher(regex: String, value: String): PactDslRootValue {
      if (!value.matches(Regex(regex))) {
        throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$regex\"")
      }
      val rootValue = PactDslRootValue()
      rootValue.setValue(value)
      rootValue.setMatcher(rootValue.regexp(regex))
      return rootValue
    }

    /**
     * Value that must match the given timestamp format
     * @param format timestamp format
     */
    @JvmOverloads
    @JvmStatic
    fun timestamp(format: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", DateTimeGenerator(format))
      val instance = FastDateFormat.getInstance(format)
      value.setValue(instance.format(Date(DATE_2000)))
      value.setMatcher(value.matchTimestamp(format))
      return value
    }

    /**
     * Value that must match the given timestamp format
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     */
    @JvmStatic
    fun timestamp(format: String, example: Date): PactDslRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslRootValue()
      value.setValue(instance.format(example))
      value.setMatcher(value.matchTimestamp(format))
      return value
    }

    /**
     * Value that must match the provided date format
     * @param format date format to match
     */
    @JvmOverloads
    @JvmStatic
    fun date(format: String = DateFormatUtils.ISO_DATE_FORMAT.pattern): PactDslRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", DateGenerator(format))
      value.setValue(instance.format(Date(DATE_2000)))
      value.setMatcher(value.matchDate(format))
      return value
    }

    /**
     * Value that must match the provided date format
     * @param format date format to match
     * @param example example date to use for generated values
     */
    @JvmStatic
    fun date(format: String, example: Date): PactDslRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslRootValue()
      value.setValue(instance.format(example))
      value.setMatcher(value.matchDate(format))
      return value
    }

    /**
     * Value that must match the given time format
     * @param format time format to match
     */
    @JvmOverloads
    @JvmStatic
    fun time(format: String = DateFormatUtils.ISO_TIME_FORMAT.pattern): PactDslRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", TimeGenerator(format))
      value.setValue(instance.format(Date(DATE_2000)))
      value.setMatcher(value.matchTime(format))
      return value
    }

    /**
     * Value that must match the given time format
     * @param format time format to match
     * @param example example time to use for generated bodies
     */
    @JvmStatic
    fun time(format: String, example: Date): PactDslRootValue {
      val instance = FastDateFormat.getInstance(format)
      val value = PactDslRootValue()
      value.setValue(instance.format(example))
      value.setMatcher(value.matchTime(format))
      return value
    }

    /**
     * Value that must be an IP4 address
     */
    @JvmStatic
    fun ipAddress(): PactDslRootValue {
      val value = PactDslRootValue()
      value.setValue("127.0.0.1")
      value.setMatcher(value.regexp("(\\d{1,3}\\.)+\\d{1,3}"))
      return value
    }

    /**
     * Value that must be a numeric identifier
     */
    @JvmStatic
    fun id(): PactDslRootValue {
      return numberType()
    }

    /**
     * Value that must be a numeric identifier
     * @param id example id to use for generated bodies
     */
    @JvmStatic
    fun id(id: Long): PactDslRootValue {
      return numberType(id)
    }

    /**
     * Value that must be encoded as a hexadecimal value
     */
    @JvmStatic
    fun hexValue(): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", RandomHexadecimalGenerator(10))
      value.setValue("1234a")
      value.setMatcher(value.regexp("[0-9a-fA-F]+"))
      return value
    }

    /**
     * Value that must be encoded as a hexadecimal value
     * @param hexValue example value to use for generated bodies
     */
    @JvmStatic
    fun hexValue(hexValue: String): PactDslRootValue {
      if (!hexValue.matches(HEXADECIMAL)) {
        throw InvalidMatcherException("Example \"$hexValue\" is not a hexadecimal value")
      }
      val value = PactDslRootValue()
      value.setValue(hexValue)
      value.setMatcher(value.regexp("[0-9a-fA-F]+"))
      return value
    }

    /**
     * Value that must be encoded as an UUID
     */
    @JvmStatic
    fun uuid(): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", UuidGenerator())
      value.setValue("e2490de5-5bd3-43d5-b7c4-526e33f71304")
      value.setMatcher(value.regexp(UUID_REGEX.pattern))
      return value
    }

    /**
     * Value that must be encoded as an UUID
     * @param uuid example UUID to use for generated bodies
     */
    @JvmStatic
    fun uuid(uuid: UUID): PactDslRootValue {
      return uuid(uuid.toString())
    }

    /**
     * Value that must be encoded as an UUID
     * @param uuid example UUID to use for generated bodies
     */
    @JvmStatic
    fun uuid(uuid: String): PactDslRootValue {
      if (!uuid.matches(UUID_REGEX)) {
        throw InvalidMatcherException("Example \"$uuid\" is not an UUID")
      }
      val value = PactDslRootValue()
      value.setValue(uuid)
      value.setMatcher(value.regexp(UUID_REGEX.pattern))
      return value
    }

    /**
     * Combine all the matchers using AND
     * @param example Attribute example value
     * @param rules Matching rules to apply
     */
    @JvmStatic
    fun and(example: Any?, vararg rules: MatchingRule): PactDslRootValue {
      val value = PactDslRootValue()
      if (example != null) {
        value.setValue(example)
      } else {
        value.setValue(JSONObject.NULL)
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
    fun or(example: Any?, vararg rules: MatchingRule): PactDslRootValue {
      val value = PactDslRootValue()
      if (example != null) {
        value.setValue(example)
      } else {
        value.setValue(JSONObject.NULL)
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
    fun valueFromProviderState(expression: String, example: Any?): PactDslRootValue {
      val value = PactDslRootValue()
      value.generators.addGenerator(Category.BODY, "", ProviderStateGenerator(expression, from(example)))
      value.setValue(example)
      return value
    }
  }
}
