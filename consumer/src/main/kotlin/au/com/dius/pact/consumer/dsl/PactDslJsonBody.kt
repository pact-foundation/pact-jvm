package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.InvalidMatcherException
import au.com.dius.pact.consumer.dsl.Dsl.matcherKey
import au.com.dius.pact.core.matchers.UrlMatcherSupport
import au.com.dius.pact.core.model.Feature
import au.com.dius.pact.core.model.FeatureToggles.isFeatureSet
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
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import au.com.dius.pact.core.support.Json.toJson
import au.com.dius.pact.core.support.expressions.DataType.Companion.from
import au.com.dius.pact.core.support.json.JsonValue
import com.mifmif.common.regex.Generex
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import java.util.regex.Pattern

/**
 * DSL to define a JSON Object
 */
@Suppress("LargeClass", "TooManyFunctions", "SpreadOperator")
open class PactDslJsonBody : DslPart {
  override var body: JsonValue

  /**
   * Constructs a new body as a root
   */
  constructor() : super(".", "") {
    body = JsonValue.Object()
  }

  /**
   * Constructs a new body as a child
   * @param rootPath Path to prefix to this child
   * @param rootName Name to associate this object as in the parent
   * @param parent Parent to attach to
   */
  constructor(rootPath: String, rootName: String, parent: DslPart?) : super(parent, rootPath, rootName) {
    body = JsonValue.Object()
  }

  /**
   * Constructs a new body as a child as a copy of an existing one
   * @param rootPath Path to prefix to this child
   * @param rootName Name to associate this object as in the parent
   * @param parent Parent to attach to
   * @param body Body to copy values from
   */
  constructor(rootPath: String, rootName: String, parent: DslPart?, body: PactDslJsonBody)
    : super(parent, rootPath, rootName) {
    this.body = body.body
    matchers = body.matchers.copyWithUpdatedMatcherRootPrefix(rootPath)
    generators = body.generators.copyWithUpdatedMatcherRootPrefix(rootPath)
  }

  override fun toString(): String {
    return body.toString()
  }

  override fun putObjectPrivate(obj: DslPart) {
    for (matcherName in obj.matchers.matchingRules.keys) {
      matchers.setRules(matcherName, obj.matchers.matchingRules[matcherName]!!)
    }
    generators.addGenerators(obj.generators)
    val elementBase = StringUtils.difference(rootPath, obj.rootPath)
    val body = body as JsonValue.Object
    if (StringUtils.isNotEmpty(obj.rootName)) {
      body.add(obj.rootName, obj.body)
    } else {
      val name = StringUtils.strip(elementBase, ".")
      val p = Pattern.compile("\\['(.+)'\\]")
      val matcher = p.matcher(name)
      if (matcher.matches()) {
        body.add(matcher.group(1), obj.body)
      } else {
        body.add(name, obj.body)
      }
    }
  }

  override fun putArrayPrivate(obj: DslPart) {
    for (matcherName in obj.matchers.matchingRules.keys) {
      matchers.setRules(matcherName, obj.matchers.matchingRules[matcherName]!!)
    }
    generators.addGenerators(obj.generators)
    val body = body as JsonValue.Object
    if (StringUtils.isNotEmpty(obj.rootName)) {
      body.add(obj.rootName, obj.body)
    } else {
      body.add(StringUtils.difference(rootPath, obj.rootPath), obj.body)
    }
  }

  /**
   * Attribute that must be the specified value
   * @param name attribute name
   * @param value string value
   */
  fun stringValue(name: String, value: String?): PactDslJsonBody {
    val body = body as JsonValue.Object
    if (value == null) {
      body.add(name, JsonValue.Null)
    } else {
      body.add(name, JsonValue.StringValue(value.toCharArray()))
    }
    return this
  }

  /**
   * Attribute that must be the specified number
   * @param name attribute name
   * @param value number value
   */
  fun numberValue(name: String, value: Number): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Decimal(value.toString().toCharArray()))
    return this
  }

  /**
   * Attribute that must be the specified boolean
   * @param name attribute name
   * @param value boolean value
   */
  fun booleanValue(name: String, value: Boolean): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, if (value) JsonValue.True else JsonValue.False)
    return this
  }

  /**
   * Attribute that must be the same type as the example
   * @param name attribute name
   */
  open fun like(name: String, example: Any?): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, toJson(example))
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    return this
  }

  /**
   * Attribute that can be any string
   * @param name attribute name
   */
  fun stringType(name: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name!!, rootPath), RandomStringGenerator(20))
    return stringType(name, "string")
  }

  /**
   * Attributes that can be any string
   * @param names attribute names
   */
  fun stringType(vararg names: String): PactDslJsonBody {
    for (name in names) {
      stringType(name)
    }
    return this
  }

  /**
   * Attribute that can be any string
   * @param name attribute name
   * @param example example value to use for generated bodies
   */
  fun stringType(name: String, example: String): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(example.toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    return this
  }

  /**
   * Attribute that can be any number
   * @param name attribute name
   */
  fun numberType(name: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name!!, rootPath), RandomIntGenerator(0, Int.MAX_VALUE))
    return numberType(name, 100)
  }

  /**
   * Attributes that can be any number
   * @param names attribute names
   */
  fun numberType(vararg names: String): PactDslJsonBody {
    for (name in names) {
      numberType(name)
    }
    return this
  }

  /**
   * Attribute that can be any number
   * @param name attribute name
   * @param number example number to use for generated bodies
   */
  fun numberType(name: String, number: Number): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Decimal(number.toString().toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER))
    return this
  }

  /**
   * Attribute that must be an integer
   * @param name attribute name
   */
  fun integerType(name: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name!!, rootPath), RandomIntGenerator(0, Int.MAX_VALUE))
    return integerType(name, 100)
  }

  /**
   * Attributes that must be an integer
   * @param names attribute names
   */
  fun integerType(vararg names: String): PactDslJsonBody {
    for (name in names) {
      integerType(name)
    }
    return this
  }

  /**
   * Attribute that must be an integer
   * @param name attribute name
   * @param number example integer value to use for generated bodies
   */
  fun integerType(name: String, number: Long): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Integer(number.toString().toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    return this
  }

  /**
   * Attribute that must be an integer
   * @param name attribute name
   * @param number example integer value to use for generated bodies
   */
  fun integerType(name: String, number: Int): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Integer(number.toString().toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    return this
  }

  /**
   * Attribute that must be a decimal value
   * @param name attribute name
   */
  fun decimalType(name: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), RandomDecimalGenerator(10))
    return decimalType(name, 100.0)
  }

  /**
   * Attributes that must be a decimal values
   * @param names attribute names
   */
  fun decimalType(vararg names: String): PactDslJsonBody {
    for (name in names) {
      decimalType(name)
    }
    return this
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  fun decimalType(name: String, number: BigDecimal): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Decimal(number.toString().toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    return this
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  fun decimalType(name: String, number: Double): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Decimal(number.toString().toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    return this
  }

  /**
   * Attributes that must be a boolean
   * @param names attribute names
   */
  fun booleanType(vararg names: String): PactDslJsonBody {
    for (name in names) {
      booleanType(name)
    }
    return this
  }

  /**
   * Attribute that must be a boolean
   * @param name attribute name
   * @param example example boolean to use for generated bodies
   */
  @JvmOverloads
  fun booleanType(name: String, example: Boolean = true): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, if (example) JsonValue.True else JsonValue.False)
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    return this
  }

  /**
   * Attribute that must match the regular expression
   * @param name attribute name
   * @param regex regular expression
   * @param value example value to use for generated bodies
   */
  fun stringMatcher(name: String, regex: String, value: String): PactDslJsonBody {
    if (!value.matches(Regex(regex))) {
      throw InvalidMatcherException("Example \"$value\" does not match regular expression \"$regex\"")
    }
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(value.toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), regexp(regex))
    return this
  }

  /**
   * Attribute that must match the regular expression
   * @param name attribute name
   * @param regex regular expression
   */
  fun stringMatcher(name: String, regex: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), RegexGenerator(regex))
    stringMatcher(name, regex, Generex(regex).random())
    return this
  }

  /**
   * Attribute named 'timestamp' that must be an ISO formatted timestamp
   */
  @Deprecated("Use datetime instead")
  fun timestamp(): PactDslJsonBody {
    return timestamp("timestamp")
  }

  /**
   * Attribute that must be an ISO formatted timestamp
   * @param name
   */
  @Deprecated("Use datetime instead")
  fun timestamp(name: String): PactDslJsonBody {
    datetime(name)
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format timestamp format
   */
  @Deprecated("use datetime instead")
  fun timestamp(name: String, format: String): PactDslJsonBody {
    datetime(name, format)
    return this
  }

  /**
   * Attribute that must match the given timestamp format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   */
  @Deprecated("use datetime instead")
  fun timestamp(name: String, format: String, example: Date): PactDslJsonBody {
    return datetime(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given timestamp format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  @Deprecated("use datetime instead")
  fun timestamp(name: String, format: String, example: Date, timeZone: TimeZone): PactDslJsonBody {
    datetime(name, format, example, timeZone)
    return this
  }

  /**
   * Attribute that must match the given timestamp format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   */
  @Deprecated("use datetime instead")
  fun timestamp(name: String, format: String, example: Instant): PactDslJsonBody {
    return datetime(name, format, example, TimeZone.getDefault())
  }

  /**
   * Attribute that must match the given timestamp format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  @Deprecated("use datetime instead")
  fun timestamp(name: String, format: String, example: Instant, timeZone: TimeZone): PactDslJsonBody {
    datetime(name, format, example, timeZone)
    return this
  }

  /**
   * Attribute that must be an ISO formatted datetime
   * @param name
   */
  fun datetime(name: String): PactDslJsonBody {
    val pattern = DateFormatUtils.ISO_DATETIME_FORMAT.pattern
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), DateTimeGenerator(pattern, null))
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(
      DateFormatUtils.ISO_DATETIME_FORMAT.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(pattern))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   */
  fun datetime(name: String, format: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), DateTimeGenerator(format, null))
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(formatter.format(Date(DATE_2000).toInstant()).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  @JvmOverloads
  fun datetime(
    name: String,
    format: String,
    example: Date,
    timeZone: TimeZone = TimeZone.getDefault()
  ): PactDslJsonBody {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId())
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(formatter.format(example.toInstant()).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format))
    return this
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  @JvmOverloads
  fun datetime(
    name: String,
    format: String,
    example: Instant,
    timeZone: TimeZone = TimeZone.getDefault()
  ): PactDslJsonBody {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId())
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(formatter.format(example).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format))
    return this
  }

  /**
   * Attribute that must be formatted as an ISO date
   * @param name attribute name
   */
  @JvmOverloads
  fun date(name: String = "date"): PactDslJsonBody {
    val pattern = DateFormatUtils.ISO_DATE_FORMAT.pattern
    generators.addGenerator(Category.BODY, matcherKey(name!!, rootPath), DateGenerator(pattern, null))
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(
      DateFormatUtils.ISO_DATE_FORMAT.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchDate(pattern))
    return this
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   */
  fun date(name: String, format: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), DateGenerator(format, null))
    val instance = FastDateFormat.getInstance(format)
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchDate(format))
    return this
  }

  /**
   * Attribute that must match the provided date format
   * @param name attribute date
   * @param format date format to match
   * @param example example date to use for generated values
   * @param timeZone time zone used for formatting of example date
   */
  @JvmOverloads
  fun date(name: String, format: String, example: Date, timeZone: TimeZone = TimeZone.getDefault()): PactDslJsonBody {
    val instance = FastDateFormat.getInstance(format, timeZone)
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(instance.format(example).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchDate(format))
    return this
  }
  /**
   * Attribute that must be an ISO formatted time
   * @param name attribute name
   */
  /**
   * Attribute named 'time' that must be an ISO formatted time
   */
  @JvmOverloads
  fun time(name: String = "time"): PactDslJsonBody {
    val pattern = DateFormatUtils.ISO_TIME_FORMAT.pattern
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), TimeGenerator(pattern, null))
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(
      DateFormatUtils.ISO_TIME_FORMAT.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTime(pattern))
    return this
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   */
  fun time(name: String, format: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), TimeGenerator(format, null))
    val instance = FastDateFormat.getInstance(format)
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTime(format))
    return this
  }

  /**
   * Attribute that must match the given time format
   * @param name attribute name
   * @param format time format to match
   * @param example example time to use for generated bodies
   * @param timeZone time zone used for formatting of example time
   */
  @JvmOverloads
  fun time(name: String, format: String, example: Date, timeZone: TimeZone = TimeZone.getDefault()): PactDslJsonBody {
    val instance = FastDateFormat.getInstance(format, timeZone)
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(instance.format(example).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTime(format))
    return this
  }

  /**
   * Attribute that must be an IP4 address
   * @param name attribute name
   */
  fun ipAddress(name: String): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue("127.0.0.1".toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), regexp("(\\d{1,3}\\.)+\\d{1,3}"))
    return this
  }

  /**
   * Attribute that is a JSON object
   * @param name field name
   */
  override fun `object`(name: String): PactDslJsonBody {
    return PactDslJsonBody(matcherKey(name, rootPath) + ".", "", this)
  }

  override fun `object`(): PactDslJsonBody {
    throw UnsupportedOperationException("use the object(String name) form")
  }

  /**
   * Attribute that is a JSON object defined from a DSL part
   * @param name field name
   * @param value DSL Part to set the value as
   */
  fun `object`(name: String, value: DslPart): PactDslJsonBody {
    val base = matcherKey(name, rootPath)
    if (value is PactDslJsonBody) {
      val obj = PactDslJsonBody(base, "", this, value)
      putObjectPrivate(obj)
    } else if (value is PactDslJsonArray) {
      val obj = PactDslJsonArray(base, "", this, (value as PactDslJsonArray?)!!)
      putArrayPrivate(obj)
    }
    return this
  }

  /**
   * Closes the current JSON object
   */
  override fun closeObject(): DslPart? {
    if (parent != null) {
      parent.putObjectPrivate(this)
    } else {
      matchers.applyMatcherRootPrefix("$")
      generators.applyRootPrefix("$")
    }
    closed = true
    return parent
  }

  override fun close(): DslPart? {
    var parentToReturn: DslPart? = this
    if (!closed) {
      var parent = closeObject()
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

  /**
   * Attribute that is an array
   * @param name field name
   */
  override fun array(name: String): PactDslJsonArray {
    return PactDslJsonArray(matcherKey(name, rootPath), name, this)
  }

  override fun array(): PactDslJsonArray {
    throw UnsupportedOperationException("use the array(String name) form")
  }

  override fun unorderedArray(name: String): PactDslJsonArray {
    matchers.addRule(matcherKey(name, rootPath), EqualsIgnoreOrderMatcher)
    return this.array(name)
  }

  override fun unorderedArray(): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedArray(String name) form")
  }

  override fun unorderedMinArray(name: String, size: Int): PactDslJsonArray {
    matchers.addRule(matcherKey(name, rootPath), MinEqualsIgnoreOrderMatcher(size))
    return this.array(name)
  }

  override fun unorderedMinArray(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedMinArray(String name, int size) form")
  }

  override fun unorderedMaxArray(name: String, size: Int): PactDslJsonArray {
    matchers.addRule(matcherKey(name, rootPath), MaxEqualsIgnoreOrderMatcher(size))
    return this.array(name)
  }

  override fun unorderedMaxArray(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedMaxArray(String name, int size) form")
  }

  override fun unorderedMinMaxArray(name: String, minSize: Int, maxSize: Int): PactDslJsonArray {
    require(minSize <= maxSize) {
      String.format("The minimum size of %d is greater than the maximum of %d",
        minSize, maxSize)
    }
    matchers.addRule(matcherKey(name, rootPath), MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize))
    return this.array(name)
  }

  override fun unorderedMinMaxArray(minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the unorderedMinMaxArray(String name, int minSize, int maxSize) form")
  }

  /**
   * Closes the current array
   */
  override fun closeArray(): DslPart? {
    return if (parent is PactDslJsonArray) {
      closeObject()
      parent.closeArray()
    } else {
      throw UnsupportedOperationException("can't call closeArray on an Object")
    }
  }

  /**
   * Attribute that is an array where each item must match the following example
   * @param name field name
   */
  override fun eachLike(name: String): PactDslJsonBody {
    return eachLike(name, 1)
  }

  override fun eachLike(name: String, obj: DslPart): PactDslJsonBody {
    val base = matcherKey(name, rootPath)
    matchers.addRule(base, TypeMatcher)
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray() as PactDslJsonBody
  }

  override fun eachLike(): PactDslJsonBody {
    throw UnsupportedOperationException("use the eachLike(String name) form")
  }

  override fun eachLike(obj: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachLike(String name, DslPart object) form")
  }

  /**
   * Attribute that is an array where each item must match the following example
   * @param name field name
   * @param numberExamples number of examples to generate
   */
  override fun eachLike(name: String, numberExamples: Int): PactDslJsonBody {
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", ".", parent)
  }

  override fun eachLike(numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the eachLike(String name, int numberExamples) form")
  }

  /**
   * Attribute that is an array of values that are not objects where each item must match the following example
   * @param name field name
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  @JvmOverloads
  fun eachLike(name: String, value: PactDslJsonRootValue, numberExamples: Int = 1): PactDslJsonBody {
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    parent.putObjectPrivate(value)
    return parent.closeArray() as PactDslJsonBody
  }

  /**
   * Attribute that is an array with a minimum size where each item must match the following example
   * @param name field name
   * @param size minimum size of the array
   */
  override fun minArrayLike(name: String, size: Int): PactDslJsonBody {
    return minArrayLike(name, size, size)
  }

  override fun minArrayLike(size: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the minArrayLike(String name, Integer size) form")
  }

  override fun minArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody {
    val base = matcherKey(name, rootPath)
    matchers.addRule(base, matchMin(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray() as PactDslJsonBody
  }

  override fun minArrayLike(size: Int, obj: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException("use the minArrayLike(String name, Integer size, DslPart object) form")
  }

  /**
   * Attribute that is an array with a minimum size where each item must match the following example
   * @param name field name
   * @param size minimum size of the array
   * @param numberExamples number of examples to generate
   */
  override fun minArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody {
    require(numberExamples >= size) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size)
    }
    matchers.addRule(matcherKey(name, rootPath), matchMin(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", "", parent)
  }

  override fun minArrayLike(size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the minArrayLike(String name, Integer size, int numberExamples) form")
  }

  /**
   * Attribute that is an array of values with a minimum size that are not objects where each item must match
   * the following example
   * @param name field name
   * @param size minimum size of the array
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  @JvmOverloads
  fun minArrayLike(name: String, size: Int, value: PactDslJsonRootValue, numberExamples: Int = 2): PactDslJsonBody {
    require(numberExamples >= size) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size)
    }
    matchers.addRule(matcherKey(name, rootPath), matchMin(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    parent.putObjectPrivate(value)
    return parent.closeArray() as PactDslJsonBody
  }

  /**
   * Attribute that is an array with a maximum size where each item must match the following example
   * @param name field name
   * @param size maximum size of the array
   */
  override fun maxArrayLike(name: String, size: Int): PactDslJsonBody {
    return maxArrayLike(name, size, 1)
  }

  override fun maxArrayLike(size: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the maxArrayLike(String name, Integer size) form")
  }

  override fun maxArrayLike(name: String, size: Int, obj: DslPart): PactDslJsonBody {
    val base = matcherKey(name, rootPath)
    matchers.addRule(base, matchMax(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray() as PactDslJsonBody
  }

  override fun maxArrayLike(size: Int, obj: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException("use the maxArrayLike(String name, Integer size, DslPart object) form")
  }

  /**
   * Attribute that is an array with a maximum size where each item must match the following example
   * @param name field name
   * @param size maximum size of the array
   * @param numberExamples number of examples to generate
   */
  override fun maxArrayLike(name: String, size: Int, numberExamples: Int): PactDslJsonBody {
    require(numberExamples <= size) {
      String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size)
    }
    matchers.addRule(matcherKey(name, rootPath), matchMax(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", "", parent)
  }

  override fun maxArrayLike(size: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the maxArrayLike(String name, Integer size, int numberExamples) form")
  }

  /**
   * Attribute that is an array of values with a maximum size that are not objects where each item must match the
   * following example
   * @param name field name
   * @param size maximum size of the array
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  @JvmOverloads
  fun maxArrayLike(name: String, size: Int, value: PactDslJsonRootValue, numberExamples: Int = 1): PactDslJsonBody {
    require(numberExamples <= size) {
      String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size)
    }
    matchers.addRule(matcherKey(name, rootPath), matchMax(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    parent.putObjectPrivate(value)
    return parent.closeArray() as PactDslJsonBody
  }

  /**
   * Attribute that must be a numeric identifier
   * @param name attribute name
   */
  /**
   * Attribute named 'id' that must be a numeric identifier
   */
  @JvmOverloads
  fun id(name: String = "id"): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), RandomIntGenerator(0, Int.MAX_VALUE))
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Integer("1234567890".toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    return this
  }

  /**
   * Attribute that must be a numeric identifier
   * @param name attribute name
   * @param id example id to use for generated bodies
   */
  fun id(name: String, id: Long): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.Integer(id.toString().toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    return this
  }

  /**
   * Attribute that must be encoded as a hexadecimal value
   * @param name attribute name
   */
  fun hexValue(name: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name!!, rootPath), RandomHexadecimalGenerator(10))
    return hexValue(name, "1234a")
  }

  /**
   * Attribute that must be encoded as a hexadecimal value
   * @param name attribute name
   * @param hexValue example value to use for generated bodies
   */
  fun hexValue(name: String, hexValue: String): PactDslJsonBody {
    if (!hexValue.matches(HEXADECIMAL)) {
      throw InvalidMatcherException("Example \"$hexValue\" is not a hexadecimal value")
    }
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(hexValue.toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), regexp("[0-9a-fA-F]+"))
    return this
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   */
  fun uuid(name: String): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name!!, rootPath), UuidGenerator)
    return uuid(name, "e2490de5-5bd3-43d5-b7c4-526e33f71304")
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   * @param uuid example UUID to use for generated bodies
   */
  fun uuid(name: String, uuid: UUID): PactDslJsonBody {
    return uuid(name, uuid.toString())
  }

  /**
   * Attribute that must be encoded as an UUID
   * @param name attribute name
   * @param uuid example UUID to use for generated bodies
   */
  fun uuid(name: String, uuid: String): PactDslJsonBody {
    if (!uuid.matches(UUID_REGEX)) {
      throw InvalidMatcherException("Example \"$uuid\" is not an UUID")
    }
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(uuid.toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), regexp(UUID_REGEX.pattern))
    return this
  }

  /**
   * Sets the field to a null value
   * @param fieldName field name
   */
  fun nullValue(fieldName: String): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(fieldName, JsonValue.Null)
    return this
  }

  override fun eachArrayLike(name: String): PactDslJsonArray {
    return eachArrayLike(name, 1)
  }

  override fun eachArrayLike(): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayLike(String name) form")
  }

  override fun eachArrayLike(name: String, numberExamples: Int): PactDslJsonArray {
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    val parent = PactDslJsonArray(matcherKey(name, rootPath), name, this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }

  override fun eachArrayLike(numberExamples: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayLike(String name, int numberExamples) form")
  }

  override fun eachArrayWithMaxLike(name: String, size: Int): PactDslJsonArray {
    return eachArrayWithMaxLike(name, 1, size)
  }

  override fun eachArrayWithMaxLike(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMaxLike(String name, Integer size) form")
  }

  override fun eachArrayWithMaxLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray {
    require(numberExamples <= size) {
      String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size)
    }
    matchers.addRule(matcherKey(name, rootPath), matchMax(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), name, this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }

  override fun eachArrayWithMaxLike(numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(
      "use the eachArrayWithMaxLike(String name, int numberExamples, Integer size) form")
  }

  override fun eachArrayWithMinLike(name: String, size: Int): PactDslJsonArray {
    return eachArrayWithMinLike(name, size, size)
  }

  override fun eachArrayWithMinLike(size: Int): PactDslJsonArray {
    throw UnsupportedOperationException("use the eachArrayWithMinLike(String name, Integer size) form")
  }

  override fun eachArrayWithMinLike(name: String, numberExamples: Int, size: Int): PactDslJsonArray {
    require(numberExamples >= size) {
      String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size)
    }
    matchers.addRule(matcherKey(name, rootPath), matchMin(size))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), name, this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }

  override fun eachArrayWithMinLike(numberExamples: Int, size: Int): PactDslJsonArray {
    throw UnsupportedOperationException(
      "use the eachArrayWithMinLike(String name, int numberExamples, Integer size) form")
  }

  /**
   * Accepts any key, and each key is mapped to a list of items that must match the following object definition
   * @param exampleKey Example key to use for generating bodies
   */
  fun eachKeyMappedToAnArrayLike(exampleKey: String): PactDslJsonBody {
    matchers.addRule(
      if (rootPath.endsWith(".")) rootPath.substring(0, rootPath.length - 1) else rootPath, ValuesMatcher
    )
    val parent = PactDslJsonArray("$rootPath*", exampleKey, this, true)
    return PactDslJsonBody(".", "", parent)
  }

  /**
   * Accepts any key, and each key is mapped to a map that must match the following object definition
   * @param exampleKey Example key to use for generating bodies
   */
  fun eachKeyLike(exampleKey: String): PactDslJsonBody {
    matchers.addRule(
      if (rootPath.endsWith(".")) rootPath.substring(0, rootPath.length - 1) else rootPath, ValuesMatcher
    )
    return PactDslJsonBody("$rootPath*.", exampleKey, this)
  }

  /**
   * Accepts any key, and each key is mapped to a map that must match the provided object definition
   * @param exampleKey Example key to use for generating bodies
   * @param value Value to use for matching and generated bodies
   */
  fun eachKeyLike(exampleKey: String, value: PactDslJsonRootValue): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(exampleKey, value.body)
    matchers.addRule(
      if (rootPath.endsWith(".")) rootPath.substring(0, rootPath.length - 1) else rootPath, ValuesMatcher
    )
    for (matcherName in value.matchers.matchingRules.keys) {
      matchers.addRules("$rootPath*$matcherName", value.matchers.matchingRules[matcherName]!!.rules)
    }
    return this
  }

  /**
   * Attribute that must include the provided string value
   * @param name attribute name
   * @param value Value that must be included
   */
  fun includesStr(name: String, value: String): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(value.toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), includesMatcher(value))
    return this
  }

  /**
   * Attribute that must be equal to the provided value.
   * @param name attribute name
   * @param value Value that will be used for comparisons
   */
  fun equalTo(name: String, value: Any?): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, toJson(value))
    matchers.addRule(matcherKey(name, rootPath), EqualsMatcher)
    return this
  }

  /**
   * Combine all the matchers using AND
   * @param name Attribute name
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  fun and(name: String, value: Any?, vararg rules: MatchingRule): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, toJson(value))
    matchers.setRules(matcherKey(name, rootPath), MatchingRuleGroup(mutableListOf(*rules), RuleLogic.AND))
    return this
  }

  /**
   * Combine all the matchers using OR
   * @param name Attribute name
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  fun or(name: String, value: Any?, vararg rules: MatchingRule): PactDslJsonBody {
    val body = body as JsonValue.Object
    body.add(name, toJson(value))
    matchers.setRules(matcherKey(name, rootPath), MatchingRuleGroup(mutableListOf(*rules), RuleLogic.OR))
    return this
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param name Attribute name
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  override fun matchUrl(name: String, basePath: String?, vararg pathFragments: Any): PactDslJsonBody {
    val urlMatcher = UrlMatcherSupport(basePath, listOf(*pathFragments))
    val exampleValue = urlMatcher.getExampleValue()
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(exampleValue.toCharArray()))
    val regexExpression = urlMatcher.getRegexExpression()
    matchers.addRule(matcherKey(name, rootPath), regexp(regexExpression))
    if (StringUtils.isEmpty(basePath)) {
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath),
        MockServerURLGenerator(exampleValue, regexExpression))
    }
    return this
  }

  override fun matchUrl(basePath: String?, vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException(
      "URL matcher without an attribute name is not supported for objects. " +
        "Use matchUrl(String name, String basePath, Object... pathFragments)")
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. Base path from the mock server
   * will be used.
   * @param name Attribute name
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  override fun matchUrl2(name: String, vararg pathFragments: Any): PactDslJsonBody {
    return matchUrl(name, null, *pathFragments)
  }

  override fun matchUrl2(vararg pathFragments: Any): DslPart {
    throw UnsupportedOperationException(
      "URL matcher without an attribute name is not supported for objects. " +
        "Use matchUrl2(Object... pathFragments)")
  }

  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int): PactDslJsonBody {
    return minMaxArrayLike(name, minSize, maxSize, minSize)
  }

  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonBody {
    validateMinAndMaxAndExamples(minSize, maxSize, minSize)
    val base = matcherKey(name, rootPath)
    matchers.addRule(base, matchMinMax(minSize, maxSize))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    if (obj is PactDslJsonBody) {
      parent.putObjectPrivate(obj)
    } else if (obj is PactDslJsonArray) {
      parent.putArrayPrivate(obj)
    }
    return parent.closeArray() as PactDslJsonBody
  }

  override fun minMaxArrayLike(minSize: Int, maxSize: Int): PactDslJsonBody {
    throw UnsupportedOperationException("use the minMaxArrayLike(String name, Integer minSize, Integer maxSize) form")
  }

  override fun minMaxArrayLike(minSize: Int, maxSize: Int, obj: DslPart): PactDslJsonArray {
    throw UnsupportedOperationException(
      "use the minMaxArrayLike(String name, Integer minSize, Integer maxSize, DslPart object) form")
  }

  override fun minMaxArrayLike(name: String, minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples)
    matchers.addRule(matcherKey(name, rootPath), matchMinMax(minSize, maxSize))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonBody(".", "", parent)
  }

  private fun validateMinAndMaxAndExamples(minSize: Int, maxSize: Int, numberExamples: Int) {
    require(minSize <= maxSize) {
      String.format("The minimum size %d is more than the maximum size of %d",
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
  }

  override fun minMaxArrayLike(minSize: Int, maxSize: Int, numberExamples: Int): PactDslJsonBody {
    throw UnsupportedOperationException(
      "use the minMaxArrayLike(String name, Integer minSize, Integer maxSize, int numberExamples) form")
  }

  override fun eachArrayWithMinMaxLike(name: String, minSize: Int, maxSize: Int): PactDslJsonArray {
    return eachArrayWithMinMaxLike(name, minSize, minSize, maxSize)
  }

  override fun eachArrayWithMinMaxLike(minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException(
      "use the eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize) form")
  }

  override fun eachArrayWithMinMaxLike(
    name: String,
    numberExamples: Int,
    minSize: Int,
    maxSize: Int
  ): PactDslJsonArray {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples)
    matchers.addRule(matcherKey(name, rootPath), matchMinMax(minSize, maxSize))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), name, this, true)
    parent.numberExamples = numberExamples
    return PactDslJsonArray("", "", parent)
  }

  override fun eachArrayWithMinMaxLike(numberExamples: Int, minSize: Int, maxSize: Int): PactDslJsonArray {
    throw UnsupportedOperationException(
      "use the eachArrayWithMinMaxLike(String name, int numberExamples, Integer minSize, Integer maxSize) form")
  }

  /**
   * Attribute that is an array of values with a minimum and maximum size that are not objects where each item must
   * match the following example
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  fun minMaxArrayLike(
    name: String,
    minSize: Int,
    maxSize: Int,
    value: PactDslJsonRootValue,
    numberExamples: Int
  ): PactDslJsonBody {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples)
    matchers.addRule(matcherKey(name, rootPath), matchMinMax(minSize, maxSize))
    val parent = PactDslJsonArray(matcherKey(name, rootPath), "", this, true)
    parent.numberExamples = numberExamples
    parent.putObjectPrivate(value)
    return parent.closeArray() as PactDslJsonBody
  }

  /**
   * Adds an attribute that will have it's value injected from the provider state
   * @param name Attribute name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  fun valueFromProviderState(name: String, expression: String, example: Any?): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath),
      ProviderStateGenerator(expression, from(example)))
    val body = body as JsonValue.Object
    body.add(name, toJson(example))
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher)
    return this
  }

  /**
   * Adds a date attribute with the value generated by the date expression
   * @param name Attribute name
   * @param expression Date expression to use to generate the values
   * @param format Date format to use
   */
  @JvmOverloads
  fun dateExpression(
    name: String,
    expression: String,
    format: String = DateFormatUtils.ISO_DATE_FORMAT.pattern
  ): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), DateGenerator(format, expression))
    val instance = FastDateFormat.getInstance(format)
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchDate(format))
    return this
  }

  /**
   * Adds a time attribute with the value generated by the time expression
   * @param name Attribute name
   * @param expression Time expression to use to generate the values
   * @param format Time format to use
   */
  @JvmOverloads
  fun timeExpression(
    name: String,
    expression: String,
    format: String = DateFormatUtils.ISO_TIME_NO_T_FORMAT.pattern
  ): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), TimeGenerator(format, expression))
    val instance = FastDateFormat.getInstance(format)
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTime(format))
    return this
  }

  /**
   * Adds a datetime attribute with the value generated by the expression
   * @param name Attribute name
   * @param expression Datetime expression to use to generate the values
   * @param format Datetime format to use
   */
  @JvmOverloads
  fun datetimeExpression(
    name: String,
    expression: String,
    format: String = DateFormatUtils.ISO_DATETIME_FORMAT.pattern
  ): PactDslJsonBody {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), DateTimeGenerator(format, expression))
    val instance = FastDateFormat.getInstance(format)
    val body = body as JsonValue.Object
    body.add(name, JsonValue.StringValue(instance.format(Date(DATE_2000)).toCharArray()))
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format))
    return this
  }

  override fun arrayContaining(name: String): DslPart {
    return PactDslJsonArrayContaining(rootPath, name, this)
  }
}
