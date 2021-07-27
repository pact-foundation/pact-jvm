package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.InvalidMatcherException
import au.com.dius.pact.core.matchers.UrlMatcherSupport
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.MockServerURLGenerator
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.generators.RandomBooleanGenerator
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.expressions.DataType.Companion.from
import au.com.dius.pact.core.support.json.JsonValue
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * DSL to define a JSON array
 */
@Suppress("LargeClass", "TooManyFunctions", "SpreadOperator")
open class PactDslJsonArray : DslPart {
  final override var body: JsonValue
  private var wildCard: Boolean

  /**
   * Sets the number of example elements to generate for sample bodies
   */
  var numberExamples = 1

  /**
   * Construct an array as a child copied from an existing array
   *
   * @param rootPath Path to the child array
   * @param rootName Name to associate the child as
   * @param parent   Parent to attach the child to
   * @param array    Array to copy
   */
  constructor(rootPath: String, rootName: String, parent: DslPart?, array: PactDslJsonArray)
    : super(parent, rootPath, rootName) {
    body = array.body
    wildCard = array.wildCard
    matchers = array.matchers.copyWithUpdatedMatcherRootPrefix(rootPath)
    generators = array.generators
  }

  /**
   * Construct a array as a child
   *
   * @param rootPath Path to the child array
   * @param rootName Name to associate the child as
   * @param parent   Parent to attach the child to
   * @param wildCard If it should be matched as a wild card
   */
  @JvmOverloads
  constructor(rootPath: String = "", rootName: String = "", parent: DslPart? = null, wildCard: Boolean = false)
    : super(parent, rootPath, rootName) {
    this.wildCard = wildCard
    body = JsonValue.Array()
  }

  /**
   * Closes the current array
   */
  override fun closeArray(): DslPart? {
    if (parent != null) {
      parent.putArrayPrivate(this)
    } else {
      matchers.applyMatcherRootPrefix("$")
      generators.applyRootPrefix("$")
    }
    closed = true
    return parent
  }

  override fun eachLike(name: String): PactDslJsonBody {
    throw UnsupportedOperationException("use the eachLike() form")
  }

  override fun eachLike(name: String, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException("use the eachLike(DslPart object) form")
  }

  override fun eachLike(name: String, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the eachLike(numberExamples) form")
  }

  /**
   * Element that is an array where each item must match the following example
   */
  override fun eachLike(): PactDslJsonBody {
    return eachLike(1)
  }

  override fun eachLike(obj: DslPart): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), TypeMatcher)
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray()!!.asArray()
  }

  /**
   * Element that is an array where each item must match the following example
   *
   * @param numberExamples Number of examples to generate
   */
  override fun eachLike(numberExamples: Int): PactDslJsonBody {
    matchers.addRule(rootPath + appendArrayIndex(1), TypeMatcher)
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", "", parent)
  }

  override fun minArrayLike(name: String, size: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the minArrayLike(Integer size) form")
  }

  /**
   * Element that is an array with a minimum size where each item must match the following example
   *
   * @param size minimum size of the array
   */
  override fun minArrayLike(size: Int): PactDslJsonBody {
    return minArrayLike(size, size)
  }

  override fun minArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException("use the minArrayLike(Integer size, DslPart object) form")
  }

  override fun minArrayLike(size: Int, obj: DslPart): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(size))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = size
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray()!!.asArray()
  }

  override fun minArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the minArrayLike(Integer size, int numberExamples) form")
  }

  /**
   * Element that is an array with a minimum size where each item must match the following example
   *
   * @param size           minimum size of the array
   * @param numberExamples number of examples to generate
   */
  override fun minArrayLike(size: Int, numberExamples: Int): PactDslJsonBody {
    require(numberExamples >= size) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(size))
    val parent = PactDslJsonArray("", "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", "", parent)
  }

  override fun maxArrayLike(name: String, size: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the maxArrayLike(Integer size) form")
  }

  /**
   * Element that is an array with a maximum size where each item must match the following example
   *
   * @param size maximum size of the array
   */
  override fun maxArrayLike(size: Int): PactDslJsonBody {
    return maxArrayLike(size, 1)
  }

  override fun maxArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException("use the maxArrayLike(Integer size, DslPart object) form")
  }

  override fun maxArrayLike(size: Int, obj: DslPart): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), matchMax(size))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray()!!.asArray()
  }

  override fun maxArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the maxArrayLike(Integer size, int numberExamples) form")
  }

  /**
   * Element that is an array with a maximum size where each item must match the following example
   *
   * @param size           maximum size of the array
   * @param numberExamples number of examples to generate
   */
  override fun maxArrayLike(size: Int, numberExamples: Int): PactDslJsonBody {
    require(numberExamples <= size) {
      String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMax(size))
    val parent = PactDslJsonArray("", "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", "", parent)
  }

  override fun putObjectPrivate(obj: DslPart) {
    for (matcherName in obj.matchers.matchingRules.keys) {
      matchers.setRules(rootPath + appendArrayIndex(1) + matcherName,
        obj.matchers.matchingRules[matcherName]!!)
    }
    generators.addGenerators(obj.generators, rootPath + appendArrayIndex(1))
    for (i in 0 until numberExamples) {
      body.add(obj.body)
    }
  }

  override fun putArrayPrivate(obj: DslPart) {
    for (matcherName in obj.matchers.matchingRules.keys) {
      matchers.setRules(rootPath + appendArrayIndex(1) + matcherName,
        obj.matchers.matchingRules[matcherName]!!)
    }
    generators.addGenerators(obj.generators, rootPath + appendArrayIndex(1))
    for (i in 0 until numberExamples) {
      body.add(obj.body)
    }
  }

  /**
   * Element that must be the specified value
   *
   * @param value string value
   */
  fun stringValue(value: String?): PactDslJsonArray {
    if (value == null) {
      body.add(JsonValue.Null)
    } else {
      body.add(JsonValue.StringValue(value.toCharArray()))
    }
    return this
  }

  /**
   * Element that must be the specified value
   *
   * @param value string value
   */
  fun string(value: String?): PactDslJsonArray {
    return stringValue(value)
  }

  fun numberValue(value: Number): PactDslJsonArray {
    body.add(JsonValue.Decimal(value.toString().toCharArray()))
    return this
  }

  /**
   * Element that must be the specified value
   *
   * @param value number value
   */
  fun number(value: Number): PactDslJsonArray {
    return numberValue(value)
  }

  /**
   * Element that must be the specified value
   *
   * @param value boolean value
   */
  fun booleanValue(value: Boolean): PactDslJsonArray {
    body.add(if (value) JsonValue.True else JsonValue.False)
    return this
  }

  /**
   * Element that must be the same type as the example
   */
  fun like(example: Any?): PactDslJsonArray {
    body.add(toJson(example))
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }

  /**
   * Element that can be any string
   */
  fun stringType(): PactDslJsonArray {
    body.add(JsonValue.StringValue("string".toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), RandomStringGenerator(20))
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }

  /**
   * Element that can be any string
   *
   * @param example example value to use for generated bodies
   */
  fun stringType(example: String): PactDslJsonArray {
    body.add(JsonValue.StringValue(example.toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }

  /**
   * Element that can be any number
   */
  fun numberType(): PactDslJsonArray {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), RandomIntGenerator(0, Int.MAX_VALUE))
    return numberType(100)
  }

  /**
   * Element that can be any number
   *
   * @param number example number to use for generated bodies
   */
  fun numberType(number: Number): PactDslJsonArray {
    body.add(JsonValue.Decimal(number.toString().toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER))
    return this
  }

  /**
   * Element that must be an integer
   */
  fun integerType(): PactDslJsonArray {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), RandomIntGenerator(0, Int.MAX_VALUE))
    return integerType(100L)
  }

  /**
   * Element that must be an integer
   *
   * @param number example integer value to use for generated bodies
   */
  fun integerType(number: Long): PactDslJsonArray {
    body.add(JsonValue.Integer(number.toString().toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    return this
  }

  /**
   * Element that must be a decimal value
   */
  fun decimalType(): PactDslJsonArray {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), RandomDecimalGenerator(10))
    return decimalType(BigDecimal("100"))
  }

  /**
   * Element that must be a decimalType value
   *
   * @param number example decimalType value
   */
  fun decimalType(number: BigDecimal): PactDslJsonArray {
    body.add(JsonValue.Decimal(number.toString().toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    return this
  }

  /**
   * Attribute that must be a decimalType value
   *
   * @param number example decimalType value
   */
  fun decimalType(number: Double): PactDslJsonArray {
    body.add(JsonValue.Decimal(number.toString().toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    return this
  }

  /**
   * Element that must be a boolean
   */
  fun booleanType(): PactDslJsonArray {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), RandomBooleanGenerator)
    body.add(JsonValue.True)
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }

  /**
   * Element that must be a boolean
   *
   * @param example example boolean to use for generated bodies
   */
  fun booleanType(example: Boolean): PactDslJsonArray {
    body.add(if (example) JsonValue.True else JsonValue.False)
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }

  /**
   * Element that must match the regular expression
   *
   * @param regex regular expression
   * @param value example value to use for generated bodies
   */
  fun stringMatcher(regex: String, value: String): PactDslJsonArray {
    if (!value.matches(Regex(regex))) {
      throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$regex\"")
    }
    body.add(JsonValue.StringValue(value.toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), regexp(regex))
    return this
  }

  /**
   * Element that must be an ISO formatted timestamp
   */
  fun datetime(): PactDslJsonArray {
    val pattern = DateFormatUtils.ISO_DATETIME_FORMAT.pattern
    body.add(JsonValue.StringValue(
      DateFormatUtils.ISO_DATETIME_FORMAT.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), DateTimeGenerator(pattern))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(pattern))
    return this
  }

  /**
   * Element that must match the given timestamp format
   *
   * @param format timestamp format
   */
  fun datetime(format: String): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), DateTimeGenerator(format))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format))
    return this
  }

  /**
   * Element that must match the given timestamp format
   *
   * @param format  timestamp format
   * @param example example date and time to use for generated bodies
   */
  fun datetime(format: String, example: Date): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(example).toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format))
    return this
  }

  /**
   * Element that must match the given timestamp format
   *
   * @param format  timestamp format
   * @param example example date and time to use for generated bodies
   */
  fun datetime(format: String, example: Instant): PactDslJsonArray {
    val formatter = DateTimeFormatter.ofPattern(format)
    body.add(JsonValue.StringValue(formatter.format(example).toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format))
    return this
  }

  /**
   * Element that must be formatted as an ISO date
   */
  fun date(): PactDslJsonArray {
    val pattern = DateFormatUtils.ISO_DATE_FORMAT.pattern
    body.add(JsonValue.StringValue(DateFormatUtils.ISO_DATE_FORMAT.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), DateGenerator(pattern))
    matchers.addRule(rootPath + appendArrayIndex(0), matchDate(pattern))
    return this
  }

  /**
   * Element that must match the provided date format
   *
   * @param format date format to match
   */
  fun date(format: String): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), DateTimeGenerator(format))
    matchers.addRule(rootPath + appendArrayIndex(0), matchDate(format))
    return this
  }

  /**
   * Element that must match the provided date format
   *
   * @param format  date format to match
   * @param example example date to use for generated values
   */
  fun date(format: String, example: Date): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(example).toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), matchDate(format))
    return this
  }

  /**
   * Element that must be an ISO formatted time
   */
  fun time(): PactDslJsonArray {
    val pattern = DateFormatUtils.ISO_TIME_FORMAT.pattern
    body.add(JsonValue.StringValue(DateFormatUtils.ISO_TIME_FORMAT.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), TimeGenerator(pattern))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTime(pattern))
    return this
  }

  /**
   * Element that must match the given time format
   *
   * @param format time format to match
   */
  fun time(format: String): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), TimeGenerator(format))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTime(format))
    return this
  }

  /**
   * Element that must match the given time format
   *
   * @param format  time format to match
   * @param example example time to use for generated bodies
   */
  fun time(format: String, example: Date): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(example).toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTime(format))
    return this
  }

  /**
   * Element that must be an IP4 address
   */
  fun ipAddress(): PactDslJsonArray {
    body.add(JsonValue.StringValue("127.0.0.1".toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), regexp("(\\d{1,3}\\.)+\\d{1,3}"))
    return this
  }

  override fun `object`(name: String): PactDslJsonBody {
    throw UnsupportedOperationException("use the object() form")
  }

  /**
   * Element that is a JSON object
   */
  override fun `object`(): PactDslJsonBody {
    return PactDslJsonBody(".", "", this)
  }

  override fun closeObject(): DslPart? {
    throw UnsupportedOperationException("can't call closeObject on an Array")
  }

  override fun close(): DslPart? {
    var parentToReturn: DslPart? = this
    if (!closed) {
      var parent: DslPart? = closeArray()
      while (parent != null) {
        parentToReturn = parent
        parent = if (parent is PactDslJsonArray) {
          parent.closeArray()
        } else {
          parent.closeObject()
        }
      }
    }
    return parentToReturn
  }

  override fun arrayContaining(name: String): DslPart {
    throw UnsupportedOperationException(
      "arrayContaining is not currently supported for arrays")
  }

  override fun array(name: String): PactDslJsonArray {
    throw UnsupportedOperationException("use the array() form")
  }

  /**
   * Element that is a JSON array
   */
  override fun array(): PactDslJsonArray {
    return PactDslJsonArray("", "", this)
  }

  override fun unorderedArray(name: String): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedArray() form")
  }

  override fun unorderedArray(): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), EqualsIgnoreOrderMatcher)
    return this.array()
  }

  override fun unorderedMinArray(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedMinArray(int size) form")
  }

  override fun unorderedMinArray(size: Int): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), MinEqualsIgnoreOrderMatcher(size))
    return this.array()
  }

  override fun unorderedMaxArray(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedMaxArray(int size) form")
  }

  override fun unorderedMaxArray(size: Int): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), MaxEqualsIgnoreOrderMatcher(size))
    return this.array()
  }

  override fun unorderedMinMaxArray(name: String, minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedMinMaxArray(int minSize, int maxSize) form")
  }

  override fun unorderedMinMaxArray(minSize: Int, maxSize: Int): PactDslJsonArray {
    require(minSize <= maxSize) {
      String.format("The minimum size of %d is greater than the maximum of %d",
        minSize, maxSize)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize))
    return this.array()
  }

  /**
   * Matches rule for all elements in array
   *
   * @param rule Matching rule to apply across array
   */
  fun wildcardArrayMatcher(rule: MatchingRule): PactDslJsonArray {
    wildCard = true
    matchers.addRule(rootPath + appendArrayIndex(1), rule)
    return this
  }

  /**
   * Element that must be a numeric identifier
   */
  fun id(): PactDslJsonArray {
    body.add(JsonValue.Integer("100".toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), RandomIntGenerator(0, Int.MAX_VALUE))
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }

  /**
   * Element that must be a numeric identifier
   *
   * @param id example id to use for generated bodies
   */
  fun id(id: Long): PactDslJsonArray {
    body.add(JsonValue.Integer(id.toString().toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }

  /**
   * Element that must be encoded as a hexadecimal value
   */
  fun hexValue(): PactDslJsonArray {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), RandomHexadecimalGenerator(10))
    return hexValue("1234a")
  }

  /**
   * Element that must be encoded as a hexadecimal value
   *
   * @param hexValue example value to use for generated bodies
   */
  fun hexValue(hexValue: String): PactDslJsonArray {
    if (!hexValue.matches(HEXADECIMAL)) {
      throw InvalidMatcherException("Example \"$hexValue\" is not a hexadecimal value")
    }
    body.add(JsonValue.StringValue(hexValue.toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), regexp("[0-9a-fA-F]+"))
    return this
  }

  /**
   * Element that must be encoded as an UUID
   */
  fun uuid(): PactDslJsonArray {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), UuidGenerator())
    return uuid("e2490de5-5bd3-43d5-b7c4-526e33f71304")
  }

  /**
   * Element that must be encoded as an UUID
   *
   * @param uuid example UUID to use for generated bodies
   */
  fun uuid(uuid: String): PactDslJsonArray {
    if (!uuid.matches(UUID_REGEX)) {
      throw InvalidMatcherException("Example \"$uuid\" is not an UUID")
    }
    body.add(JsonValue.StringValue(uuid.toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), regexp(UUID_REGEX.pattern))
    return this
  }

  /**
   * Adds the template object to the array
   *
   * @param template template object
   */
  fun template(template: DslPart): PactDslJsonArray {
    putObjectPrivate(template)
    return this
  }

  /**
   * Adds a number of template objects to the array
   *
   * @param template    template object
   * @param occurrences number to add
   */
  fun template(template: DslPart, occurrences: Int): PactDslJsonArray {
    for (i in 0 until occurrences) {
      template(template)
    }
    return this
  }

  override fun toString(): String {
    return body.toString()
  }

  private fun appendArrayIndex(offset: Int): String {
    var index = "*"
    if (!wildCard) {
      index = (body.size() - 1 + offset).toString()
    }
    return "[$index]"
  }

  /**
   * Adds a null value to the list
   */
  fun nullValue(): PactDslJsonArray {
    body.add(JsonValue.Null)
    return this
  }

  override fun eachArrayLike(name: String): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayLike() form")
  }

  override fun eachArrayLike(name: String, numberExamples: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayLike(numberExamples) form")
  }

  override fun eachArrayLike(): PactDslJsonArray {
    return eachArrayLike(1)
  }

  override fun eachArrayLike(numberExamples: Int): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), TypeMatcher)
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }

  override fun eachArrayWithMaxLike(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMaxLike() form")
  }

  override fun eachArrayWithMaxLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMaxLike(numberExamples) form")
  }

  override fun eachArrayWithMaxLike(size: Int): PactDslJsonArray {
    return eachArrayWithMaxLike(1, size)
  }

  override fun eachArrayWithMaxLike(numberExamples: Int, size: Int): PactDslJsonArray {
    require(numberExamples <= size) {
      String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMax(size))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }

  override fun eachArrayWithMinLike(name: String, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMinLike() form")
  }

  override fun eachArrayWithMinLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMinLike(numberExamples) form")
  }

  override fun eachArrayWithMinLike(size: Int): PactDslJsonArray {
    return eachArrayWithMinLike(size, size)
  }

  override fun eachArrayWithMinLike(numberExamples: Int, size: Int): PactDslJsonArray {
    require(numberExamples >= size) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(size))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }
  /**
   * Array of values that are not objects where each item must match the provided example
   *
   * @param value          Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  /**
   * Array of values that are not objects where each item must match the provided example
   *
   * @param value Value to use to match each item
   */
  @JvmOverloads
  fun eachLike(value: PactDslJsonRootValue?, numberExamples: Int = 1): PactDslJsonArray {
    require(numberExamples != 0) {
      "Testing Zero examples is unsafe. Please make sure to provide at least one " +
        "example in the Pact provider implementation. See https://github.com/DiUS/pact-jvm/issues/546"
    }
    matchers.addRule(rootPath + appendArrayIndex(1), TypeMatcher)
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    parent.putObjectPrivate(value!!)
    return parent.closeArray() as PactDslJsonArray
  }

  /**
   * Array of values with a minimum size that are not objects where each item must match the provided example
   *
   * @param size           minimum size of the array
   * @param value          Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  @JvmOverloads
  fun minArrayLike(size: Int, value: PactDslJsonRootValue?, numberExamples: Int = size): PactDslJsonArray {
    require(numberExamples >= size) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(size))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    parent.putObjectPrivate(value!!)
    return parent.closeArray() as PactDslJsonArray
  }

  /**
   * Array of values with a maximum size that are not objects where each item must match the provided example
   *
   * @param size           maximum size of the array
   * @param value          Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  @JvmOverloads
  fun maxArrayLike(size: Int, value: PactDslJsonRootValue?, numberExamples: Int = 1): PactDslJsonArray {
    require(numberExamples <= size) {
      String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMax(size))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    parent.putObjectPrivate(value!!)
    return parent.closeArray() as PactDslJsonArray
  }

  /**
   * List item that must include the provided string
   *
   * @param value Value that must be included
   */
  fun includesStr(value: String): PactDslJsonArray {
    body.add(JsonValue.StringValue(value.toCharArray()))
    matchers.addRule(rootPath + appendArrayIndex(0), includesMatcher(value))
    return this
  }

  /**
   * Attribute that must be equal to the provided value.
   *
   * @param value Value that will be used for comparisons
   */
  fun equalsTo(value: Any?): PactDslJsonArray {
    body.add(toJson(value))
    matchers.addRule(rootPath + appendArrayIndex(0), EqualsMatcher)
    return this
  }

  /**
   * Combine all the matchers using AND
   *
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  fun and(value: Any?, vararg rules: MatchingRule): PactDslJsonArray {
    body.add(toJson(value))
    matchers.setRules(rootPath + appendArrayIndex(0), MatchingRuleGroup(mutableListOf(*rules), RuleLogic.AND))
    return this
  }

  /**
   * Combine all the matchers using OR
   *
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  fun or(value: Any?, vararg rules: MatchingRule): PactDslJsonArray {
    body.add(toJson(value))
    matchers.setRules(rootPath + appendArrayIndex(0), MatchingRuleGroup(mutableListOf(*rules), RuleLogic.OR))
    return this
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   *
   * @param basePath      The base path for the URL (like "http://localhost:8080/") which will be
   * excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  override fun matchUrl(basePath: String?, vararg pathFragments: Any): PactDslJsonArray {
    val urlMatcher = UrlMatcherSupport(basePath, listOf(*pathFragments))
    val exampleValue = urlMatcher.getExampleValue()
    body.add(JsonValue.StringValue(exampleValue.toCharArray()))
    val regexExpression = urlMatcher.getRegexExpression()
    matchers.addRule(rootPath + appendArrayIndex(0), regexp(regexExpression))
    if (StringUtils.isEmpty(basePath)) {
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0),
        MockServerURLGenerator(exampleValue, regexExpression))
    }
    return this
  }

  override fun matchUrl(name: String, basePath: String?, vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException(
      "URL matcher with an attribute name is not supported for arrays. " +
        "Use matchUrl(String base, Object... fragments)")
  }

  override fun matchUrl2(name: String, vararg pathFragments: Any): PactDslJsonBody {
    throw UnsupportedOperationException(
      "URL matcher with an attribute name is not supported for arrays. " +
        "Use matchUrl2(Object... pathFragments)")
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. Base path from the mock server
   * will be used.
   *
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  override fun matchUrl2(vararg pathFragments: Any): DslPart {
    return matchUrl(null, *pathFragments)
  }

  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the minMaxArrayLike(minSize, maxSize) form")
  }

  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonBody {
    throw UnsupportedOperationException("use the minMaxArrayLike(minSize, maxSize, object) form")
  }

  override fun minMaxArrayLike(minSize: Int, maxSize: Int): PactDslJsonBody {
    return minMaxArrayLike(minSize, maxSize, minSize)
  }

  override fun minMaxArrayLike(minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonArray {
    matchers.addRule(rootPath + appendArrayIndex(1), matchMinMax(minSize, maxSize))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = minSize
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray()!!.asArray()
  }

  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the minMaxArrayLike(minSize, maxSize, numberExamples) form")
  }

  override fun minMaxArrayLike(minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody {
    require(minSize <= maxSize) {
      String.format("The minimum size of %d is greater than the maximum of %d",
        minSize, maxSize)
    }
    require(numberExamples >= minSize) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize)
    }
    require(numberExamples <= maxSize) {
      String.format("Number of example %d is greater than the maximum size of %d",
        numberExamples, maxSize)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMinMax(minSize, maxSize))
    val parent = PactDslJsonArray("", "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", "", parent)
  }

  override fun eachArrayWithMinMaxLike(name: String, minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMinMaxLike(minSize, maxSize) form")
  }

  override fun eachArrayWithMinMaxLike(minSize: Int, maxSize: Int): PactDslJsonArray {
    return eachArrayWithMinMaxLike(minSize, minSize, maxSize)
  }

  override fun eachArrayWithMinMaxLike(
    name: String,
    numberExamples: Int,
    minSize: Int,
    maxSize: Int
  ): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMinMaxLike(numberExamples, minSize, maxSize) form")
  }

  override fun eachArrayWithMinMaxLike(numberExamples: Int, minSize: Int, maxSize: Int): PactDslJsonArray {
    require(minSize <= maxSize) {
      String.format("The minimum size of %d is greater than the maximum of %d",
        minSize, maxSize)
    }
    require(numberExamples >= minSize) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize)
    }
    require(numberExamples <= maxSize) {
      String.format("Number of example %d is greater than the maximum size of %d",
        numberExamples, maxSize)
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMinMax(minSize, maxSize))
    val parent = PactDslJsonArray(rootPath, "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }

  /**
   * Adds an element that will have it's value injected from the provider state
   *
   * @param expression Expression to be evaluated from the provider state
   * @param example    Example value to be used in the consumer test
   */
  fun valueFromProviderState(expression: String?, example: Any?): PactDslJsonArray {
    body.add(toJson(example))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0),
      ProviderStateGenerator(expression!!, from(example!!)))
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher)
    return this
  }
  /**
   * Adds a date value with the value generated by the date expression
   *
   * @param expression Date expression to use to generate the values
   * @param format     Date format to use
   */
  @JvmOverloads
  fun dateExpression(expression: String, format: String = DateFormatUtils.ISO_DATE_FORMAT.pattern): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), DateGenerator(format, expression))
    matchers.addRule(rootPath + appendArrayIndex(0), matchDate(format))
    return this
  }

  /**
   * Adds a time value with the value generated by the time expression
   *
   * @param expression Time expression to use to generate the values
   * @param format     Time format to use
   */
  @JvmOverloads
  fun timeExpression(
    expression: String,
    format: String = DateFormatUtils.ISO_TIME_NO_T_FORMAT.pattern
  ): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), TimeGenerator(format, expression))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTime(format))
    return this
  }

  /**
   * Adds a datetime value with the value generated by the expression
   *
   * @param expression Datetime expression to use to generate the values
   * @param format     Datetime format to use
   */
  @JvmOverloads
  fun datetimeExpression(
    expression: String,
    format: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern
  ): PactDslJsonArray {
    val instance = FastDateFormat.getInstance(format)
    body.add(JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), DateTimeGenerator(format, expression))
    matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format))
    return this
  }

  companion object {
    /**
     * Array where each item must match the following example
     *
     * @param numberExamples Number of examples to generate
     */
    @JvmOverloads
    @JvmStatic
    fun arrayEachLike(numberExamples: Int = 1): PactDslJsonBody {
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", TypeMatcher)
      return PactDslJsonBody(".", "", parent)
    }

    /**
     * Root level array where each item must match the provided matcher
     */
    @JvmStatic
    fun arrayEachLike(rootValue: PactDslJsonRootValue): PactDslJsonArray {
      return arrayEachLike(1, rootValue)
    }

    /**
     * Root level array where each item must match the provided matcher
     *
     * @param numberExamples Number of examples to generate
     */
    @JvmStatic
    fun arrayEachLike(numberExamples: Int, value: PactDslJsonRootValue): PactDslJsonArray {
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", TypeMatcher)
      parent.putObjectPrivate(value)
      return parent
    }

    /**
     * Array with a minimum size where each item must match the following example
     *
     * @param minSize        minimum size
     * @param numberExamples Number of examples to generate
     */
    @JvmOverloads
    @JvmStatic
    fun arrayMinLike(minSize: Int, numberExamples: Int = minSize): PactDslJsonBody {
      require(numberExamples >= minSize) {
        String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, minSize)
      }
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", parent.matchMin(minSize))
      return PactDslJsonBody(".", "", parent)
    }

    /**
     * Root level array with minimum size where each item must match the provided matcher
     *
     * @param minSize minimum size
     */
    @JvmStatic
    fun arrayMinLike(minSize: Int, value: PactDslJsonRootValue): PactDslJsonArray {
      return arrayMinLike(minSize, minSize, value)
    }

    /**
     * Root level array with minimum size where each item must match the provided matcher
     *
     * @param minSize        minimum size
     * @param numberExamples Number of examples to generate
     */
    @JvmStatic
    fun arrayMinLike(minSize: Int, numberExamples: Int, value: PactDslJsonRootValue): PactDslJsonArray {
      require(numberExamples >= minSize) {
        String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, minSize)
      }
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", parent.matchMin(minSize))
      parent.putObjectPrivate(value)
      return parent
    }

    /**
     * Array with a maximum size where each item must match the following example
     *
     * @param maxSize        maximum size
     * @param numberExamples Number of examples to generate
     */
    @JvmOverloads
    @JvmStatic
    fun arrayMaxLike(maxSize: Int, numberExamples: Int = 1): PactDslJsonBody {
      require(numberExamples <= maxSize) {
        String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, maxSize)
      }
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", parent.matchMax(maxSize))
      return PactDslJsonBody(".", "", parent)
    }

    /**
     * Root level array with maximum size where each item must match the provided matcher
     *
     * @param maxSize maximum size
     */
    @JvmStatic
    fun arrayMaxLike(maxSize: Int, value: PactDslJsonRootValue): PactDslJsonArray {
      return arrayMaxLike(maxSize, 1, value)
    }

    /**
     * Root level array with maximum size where each item must match the provided matcher
     *
     * @param maxSize        maximum size
     * @param numberExamples Number of examples to generate
     */
    @JvmStatic
    fun arrayMaxLike(maxSize: Int, numberExamples: Int, value: PactDslJsonRootValue): PactDslJsonArray {
      require(numberExamples <= maxSize) {
        String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, maxSize)
      }
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", parent.matchMax(maxSize))
      parent.putObjectPrivate(value)
      return parent
    }

    /**
     * Array with a minimum and maximum size where each item must match the following example
     *
     * @param minSize        minimum size
     * @param maxSize        maximum size
     * @param numberExamples Number of examples to generate
     */
    @JvmOverloads
    @JvmStatic
    fun arrayMinMaxLike(minSize: Int, maxSize: Int, numberExamples: Int = minSize): PactDslJsonBody {
      require(numberExamples >= minSize) {
        String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, minSize)
      }
      require(numberExamples <= maxSize) {
        String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, maxSize)
      }
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", parent.matchMinMax(minSize, maxSize))
      return PactDslJsonBody(".", "", parent)
    }

    /**
     * Root level array with minimum and maximum size where each item must match the provided matcher
     *
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    @JvmStatic
    fun arrayMinMaxLike(minSize: Int, maxSize: Int, value: PactDslJsonRootValue): PactDslJsonArray {
      return arrayMinMaxLike(minSize, maxSize, minSize, value)
    }

    /**
     * Root level array with minimum and maximum size where each item must match the provided matcher
     *
     * @param minSize        minimum size
     * @param maxSize        maximum size
     * @param numberExamples Number of examples to generate
     */
    @JvmStatic
    fun arrayMinMaxLike(
      minSize: Int,
      maxSize: Int,
      numberExamples: Int,
      value: PactDslJsonRootValue
    ): PactDslJsonArray {
      require(numberExamples >= minSize) {
        String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, minSize)
      }
      require(numberExamples <= maxSize) {
        String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, maxSize)
      }
      val parent = PactDslJsonArray("", "", null, true)
      parent.numberExamples = numberExamples
      parent.matchers.addRule("", parent.matchMinMax(minSize, maxSize))
      parent.putObjectPrivate(value)
      return parent
    }

    /**
     * Root level array where order is ignored
     */
    @JvmStatic
    fun newUnorderedArray(): PactDslJsonArray {
      val root = PactDslJsonArray()
      root.matchers.addRule(root.rootPath, EqualsIgnoreOrderMatcher)
      return root
    }

    /**
     * Root level array of min size where order is ignored
     *
     * @param size minimum size
     */
    @JvmStatic
    fun newUnorderedMinArray(size: Int): PactDslJsonArray {
      val root = PactDslJsonArray()
      root.matchers.addRule(root.rootPath, MinEqualsIgnoreOrderMatcher(size))
      return root
    }

    /**
     * Root level array of max size where order is ignored
     *
     * @param size maximum size
     */
    @JvmStatic
    fun newUnorderedMaxArray(size: Int): PactDslJsonArray {
      val root = PactDslJsonArray()
      root.matchers.addRule(root.rootPath, MaxEqualsIgnoreOrderMatcher(size))
      return root
    }

    /**
     * Root level array of min and max size where order is ignored
     *
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    @JvmStatic
    fun newUnorderedMinMaxArray(minSize: Int, maxSize: Int): PactDslJsonArray {
      require(minSize <= maxSize) {
        String.format("The minimum size of %d is greater than the maximum of %d",
          minSize, maxSize)
      }
      val root = PactDslJsonArray()
      root.matchers.addRule(root.rootPath, MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize))
      return root
    }
  }
}
