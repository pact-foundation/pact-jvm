package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.HttpStatus
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NotEmptyMatcher
import au.com.dius.pact.core.model.matchingrules.NullMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.matchingrules.SemverMatcher
import au.com.dius.pact.core.model.matchingrules.StatusCodeMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.json.JsonValue
import com.github.zafarkhaja.semver.UnexpectedCharacterException
import com.github.zafarkhaja.semver.Version
import io.pact.plugins.jvm.core.CatalogueEntry
import io.pact.plugins.jvm.core.CatalogueEntryProviderType
import io.pact.plugins.jvm.core.CatalogueEntryType
import mu.KotlinLogging
import org.apache.commons.lang3.time.DateUtils
import org.apache.tika.config.TikaConfig
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.math.BigDecimal
import java.math.BigInteger
import java.text.ParseException
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}
private val integerRegex = Regex("^-?\\d+$")
private val decimalRegex = Regex("^0|-?\\d+\\.\\d*$")
private val booleanRegex = Regex("^true|false$")

fun valueOf(value: Any?): String {
  return when (value) {
    null -> "null"
    is String -> "'$value'"
    is Element -> "<${QualifiedName(value)}>"
    is Text -> "'${value.wholeText}'"
    is JsonValue -> value.serialise()
    else -> value.toString()
  }
}

fun typeOf(value: Any?): String {
  return when (value) {
    null -> "Null"
    is JsonValue -> value.type()
    is Attr -> "XmlAttr"
    else -> value.javaClass.simpleName
  }
}

fun safeToString(value: Any?): String {
  return when (value) {
    null -> ""
    is Text -> value.wholeText
    is Element -> value.textContent
    is Attr -> value.nodeValue
    is JsonValue -> value.toString()
    else -> value.toString()
  }
}

fun <M : Mismatch> matchInclude(
  includedValue: String,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  val matches = safeToString(actual).contains(includedValue)
  logger.debug { "comparing if ${valueOf(actual)} includes '$includedValue' at $path -> $matches" }
  return if (matches) {
    listOf()
  } else {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} to include ${valueOf(includedValue)}", path))
  }
}

/**
 * Executor for matchers
 */
fun <M : Mismatch> domatch(
  matchers: MatchingRuleGroup,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFn: MismatchFactory<M>
): List<M> {
  val result = matchers.rules.map { matchingRule ->
    domatch(matchingRule, path, expected, actual, mismatchFn, matchers.cascaded)
  }

  return if (matchers.ruleLogic == RuleLogic.AND) {
    result.flatten()
  } else {
    if (result.any { it.isEmpty() }) {
      emptyList()
    } else {
      result.flatten()
    }
  }
}

fun <M : Mismatch> domatch(
  matcher: MatchingRule,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFn: MismatchFactory<M>,
  cascaded: Boolean
): List<M> {
  return when (matcher) {
    is RegexMatcher -> matchRegex(matcher.regex, path, expected, actual, mismatchFn)
    is TypeMatcher -> matchType(path, expected, actual, mismatchFn, true)
    is NumberTypeMatcher -> matchNumber(matcher.numberType, path, expected, actual, mismatchFn)
    is DateMatcher -> matchDate(matcher.format, path, expected, actual, mismatchFn)
    is TimeMatcher -> matchTime(matcher.format, path, expected, actual, mismatchFn)
    is TimestampMatcher -> matchDateTime(matcher.format, path, expected, actual, mismatchFn)
    is MinTypeMatcher -> matchMinType(matcher.min, path, expected, actual, mismatchFn, cascaded)
    is MaxTypeMatcher -> matchMaxType(matcher.max, path, expected, actual, mismatchFn, cascaded)
    is MinMaxTypeMatcher -> matchMinType(matcher.min, path, expected, actual, mismatchFn, cascaded) +
            matchMaxType(matcher.max, path, expected, actual, mismatchFn, cascaded)
    is IncludeMatcher -> matchInclude(matcher.value, path, expected, actual, mismatchFn)
    is NullMatcher -> matchNull(path, actual, mismatchFn)
    is NotEmptyMatcher -> matchType(path, expected, actual, mismatchFn, false)
    is SemverMatcher -> matchSemver(path, expected, actual, mismatchFn)
    is EqualsIgnoreOrderMatcher -> matchEqualsIgnoreOrder(path, expected, actual, mismatchFn)
    is MinEqualsIgnoreOrderMatcher -> matchMinEqualsIgnoreOrder(matcher.min, path, expected, actual, mismatchFn)
    is MaxEqualsIgnoreOrderMatcher -> matchMaxEqualsIgnoreOrder(matcher.max, path, expected, actual, mismatchFn)
    is MinMaxEqualsIgnoreOrderMatcher -> matchMinEqualsIgnoreOrder(matcher.min, path, expected, actual, mismatchFn) +
            matchMaxEqualsIgnoreOrder(matcher.max, path, expected, actual, mismatchFn)
    is ContentTypeMatcher ->
      matchHeaderWithParameters(path, ContentType.fromString(matcher.contentType), actual, mismatchFn)
    is ArrayContainsMatcher -> listOf()
    is BooleanMatcher -> matchBoolean(path, expected, actual, mismatchFn)
    is StatusCodeMatcher ->
      matchStatusCode(matcher.statusType, matcher.values, expected as Int, actual as Int) as List<M>
    else -> matchEquality(path, expected, actual, mismatchFn)
  }
}

fun <M : Mismatch> matchEquality(
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  val matches = when {
    (actual == null || actual is JsonValue.Null) && (expected == null || expected is JsonValue.Null) -> true
    actual is Element && expected is Element -> QualifiedName(actual) == QualifiedName(expected)
    actual is Attr && expected is Attr -> QualifiedName(actual) == QualifiedName(expected) &&
      actual.nodeValue == expected.nodeValue
    actual is BigDecimal && expected is BigDecimal -> actual.compareTo(expected) == 0
    else -> actual != null && actual == expected
  }
  logger.debug {
    "comparing ${valueOf(actual)} (${typeOf(actual)}) to " +
      "${valueOf(expected)} (${typeOf(expected)}) at $path -> $matches"
  }
  return if (matches) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} (${typeOf(actual)}) to equal ${valueOf(expected)} (${typeOf(expected)})", path))
  }
}

fun <M : Mismatch> matchRegex(
  regex: String,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  val matches = if (actual == null || actual is JsonValue.Null) false else safeToString(actual).matches(Regex(regex))
  logger.debug { "comparing ${valueOf(actual)} with regexp $regex at $path -> $matches" }
  return if (matches ||
    expected is List<*> && actual is List<*> ||
    expected is JsonValue.Array && actual is JsonValue.Array ||
    expected is Map<*, *> && actual is Map<*, *> ||
    expected is JsonValue.Object && actual is JsonValue.Object) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to match '$regex'", path))
  }
}

fun <M : Mismatch> matchType(
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>,
  allowEmpty: Boolean
): List<M> {
  logger.debug {
    "comparing type of ${valueOf(actual)} (${typeOf(actual)}) to ${valueOf(expected)} (${typeOf(expected)}) at $path"
  }
  return if (expected is Number && actual is Number ||
    expected is Boolean && actual is Boolean ||
    expected is Element && actual is Element && QualifiedName(actual) == QualifiedName(expected) ||
    expected is Attr && actual is Attr && QualifiedName(actual) == QualifiedName(expected)
  ) {
    emptyList()
  } else if (expected is String && actual is String ||
    expected is List<*> && actual is List<*> ||
    expected is JsonValue.Array && actual is JsonValue.Array ||
    expected is Map<*, *> && actual is Map<*, *> ||
    expected is JsonValue.Object && actual is JsonValue.Object) {
    if (allowEmpty) {
      emptyList()
    } else {
      val empty = when (actual) {
        is String -> actual.isEmpty()
        is List<*> -> actual.isEmpty()
        is Map<*, *> -> actual.isEmpty()
        is JsonValue.Array -> actual.size == 0
        is JsonValue.Object -> actual.size == 0
        else -> false
      }
      if (empty) {
        listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to not be empty", path))
      } else {
        emptyList()
      }
    }
  } else if (expected is JsonValue && actual is JsonValue &&
    ((expected.isBoolean && actual.isBoolean) ||
      (expected.isNumber && actual.isNumber) ||
      (expected.isString && actual.isString))) {
      emptyList()
  } else if (expected == null || expected is JsonValue.Null) {
    if (actual == null || actual is JsonValue.Null) {
      emptyList()
    } else {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be null", path))
    }
  } else {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} (${typeOf(actual)}) to be the same type as " +
        "${valueOf(expected)} (${typeOf(expected)})", path))
  }
}

@Suppress("ReturnCount")
fun <M : Mismatch> matchNumber(
  numberType: NumberTypeMatcher.NumberType,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  if (expected == null && actual != null) {
    return listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be null", path))
  }
  when (numberType) {
    NumberTypeMatcher.NumberType.NUMBER -> {
      logger.debug { "comparing type of ${valueOf(actual)} (${typeOf(actual)}) to a number at $path" }
      if (actual is JsonValue && !actual.isNumber || actual is Attr && actual.nodeValue.matches(decimalRegex) ||
        actual !is JsonValue && actual !is Node && actual !is Number) {
        return listOf(mismatchFactory.create(expected, actual,
          "Expected ${valueOf(actual)} (${typeOf(actual)}) to be a number", path))
      }
    }
    NumberTypeMatcher.NumberType.INTEGER -> {
      logger.debug { "comparing type of ${valueOf(actual)} (${typeOf(actual)}) to an integer at $path" }
      if (!matchInteger(actual)) {
        return listOf(mismatchFactory.create(expected, actual,
          "Expected ${valueOf(actual)} (${typeOf(actual)}) to be an integer", path))
      }
    }
    NumberTypeMatcher.NumberType.DECIMAL -> {
      logger.debug { "comparing type of ${valueOf(actual)} (${typeOf(actual)}) to a decimal at $path" }
      if (!matchDecimal(actual)) {
        return listOf(mismatchFactory.create(expected, actual,
          "Expected ${valueOf(actual)} (${typeOf(actual)}) to be a decimal number",
          path))
      }
    }
  }
  return emptyList()
}

fun matchDecimal(actual: Any?): Boolean {
  val result = when {
    actual == 0 -> true
    actual is Float -> true
    actual is Double -> true
    actual is BigDecimal && (actual == BigDecimal.ZERO || actual.scale() > 0) -> true
    actual is JsonValue.Decimal -> {
      val bigDecimal = actual.toBigDecimal()
      bigDecimal == BigDecimal.ZERO || bigDecimal.scale() > 0
    }
    actual is JsonValue.Integer -> decimalRegex.matches(actual.toString())
    actual is Attr -> decimalRegex.matches(actual.nodeValue)
    else -> false
  }
  logger.debug { "${valueOf(actual)} (${typeOf(actual)}) matches decimal number -> $result" }
  return result
}

fun matchInteger(actual: Any?): Boolean {
  val result = when {
    actual is Int -> true
    actual is Long -> true
    actual is BigInteger -> true
    actual is JsonValue.Integer -> true
    actual is BigDecimal && actual.scale() == 0 -> true
    actual is JsonValue.Decimal -> integerRegex.matches(actual.toString())
    actual is Attr -> integerRegex.matches(actual.nodeValue)
    else -> false
  }
  logger.debug { "${valueOf(actual)} (${typeOf(actual)}) matches integer -> $result" }
  return result
}

fun <M : Mismatch> matchBoolean(
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  if (expected == null && actual != null) {
    return listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to be null", path))
  }
  logger.debug { "comparing type of ${valueOf(actual)} (${typeOf(actual)}) to match a boolean at $path" }
  return when {
    expected == null && actual == null -> emptyList()
    actual is Boolean -> emptyList()
    actual is JsonValue && actual.isBoolean -> emptyList()
    actual is Attr && actual.nodeValue.matches(booleanRegex) -> emptyList()
    actual is String && actual.matches(booleanRegex) -> emptyList()
    actual is List<*> -> emptyList()
    actual is Map<*, *> -> emptyList()
    else -> listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} (${typeOf(actual)}) to match a boolean", path))
  }
}

fun <M : Mismatch> matchDate(
  pattern: String,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} to date pattern $pattern at $path" }
  return if (isCollection(actual)) {
    emptyList()
  } else {
    try {
      DateUtils.parseDate(safeToString(actual), pattern)
      emptyList<M>()
    } catch (e: ParseException) {
      listOf(mismatchFactory.create(expected, actual,
        "Expected ${valueOf(actual)} to match a date of '$pattern': " +
          "${e.message}", path))
    }
  }
}

fun isCollection(value: Any?) = value is List<*> || value is Map<*, *>

fun <M : Mismatch> matchTime(
  pattern: String,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} to time pattern $pattern at $path" }
  return if (isCollection(actual)) {
    emptyList()
  } else {
    try {
      DateUtils.parseDate(safeToString(actual), pattern)
      emptyList<M>()
    } catch (e: ParseException) {
      listOf(mismatchFactory.create(expected, actual,
        "Expected ${valueOf(actual)} to match a time of '$pattern': " +
          "${e.message}", path))
    }
  }
}

fun <M : Mismatch> matchDateTime(
  pattern: String,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} to datetime pattern $pattern at $path" }
  return if (isCollection(actual)) {
    emptyList()
  } else {
    var newPattern = pattern
    try {
      if (pattern.endsWith('Z')) {
        newPattern = pattern.replace('Z', 'X')
        logger.warn {
          """Found unsupported UTC designator in pattern '$pattern'. Replacing non quote 'Z's with 'X's
          This is in order to offer backwards compatibility for consumers using the ISO 8601 UTC designator 'Z'
          Please update your patterns in your pact tests as this may not be supported in future versions."""
        }
      }
      DateTimeFormatter.ofPattern(newPattern).parse(safeToString(actual))
      emptyList<M>()
    } catch (e: DateTimeParseException) {
      try {
        logger.warn {
          """Failed to parse ${valueOf(actual)} with '$pattern' using java.time.format.DateTimeFormatter.
          Exception was: ${e.message}.
          Will attempt to parse using org.apache.commons.lang3.time.DateUtils to guarantee backwards 
          compatibility with versions < 4.1.1.
          Please update your patterns in your pact tests as this may not be supported in future versions."""
        }
        DateUtils.parseDate(safeToString(actual), pattern)
        emptyList<M>()
      } catch (e: ParseException) {
        listOf(mismatchFactory.create(expected, actual,
                "Expected ${valueOf(actual)} to match a datetime of '$pattern': " +
                        "${e.message}", path))
      }
    }
  }
}

fun <M : Mismatch> matchMinType(
  min: Int,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>,
  cascaded: Boolean
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} with minimum $min at $path" }
  return if (!cascaded) {
    when (actual) {
      is List<*> -> {
        if (actual.size < min) {
          listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
        } else {
          emptyList()
        }
      }
      is JsonValue.Array -> {
        if (actual.size < min) {
          listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
        } else {
          emptyList()
        }
      }
      is Element -> {
        if (actual.childNodes.length < min) {
          listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
        } else {
          emptyList()
        }
      }
      else -> matchType(path, expected, actual, mismatchFactory, true)
    }
  } else {
    matchType(path, expected, actual, mismatchFactory, true)
  }
}

fun <M : Mismatch> matchMaxType(
  max: Int,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>,
  cascaded: Boolean
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} with maximum $max at $path" }
  return if (!cascaded) {
    when (actual) {
      is List<*> -> {
        if (actual.size > max) {
          listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
        } else {
          emptyList()
        }
      }
      is JsonValue.Array -> {
        if (actual.size > max) {
          listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
        } else {
          emptyList()
        }
      }
      is Element -> {
        if (actual.childNodes.length > max) {
          listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
        } else {
          emptyList()
        }
      }
      else -> matchType(path, expected, actual, mismatchFactory, true)
    }
  } else {
    matchType(path, expected, actual, mismatchFactory, true)
  }
}

fun <M : Mismatch> matchEqualsIgnoreOrder(
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} to ${valueOf(expected)} with ignore-order at $path" }
  return if (actual is JsonValue.Array && expected is JsonValue.Array) {
    matchEqualsIgnoreOrder(path, expected, actual, expected.size(), actual.size(), mismatchFactory)
  } else if (actual is List<*> && expected is List<*>) {
    matchEqualsIgnoreOrder(path, expected, actual, expected.size, actual.size, mismatchFactory)
  } else if (actual is Element && expected is Element) {
    matchEqualsIgnoreOrder(path, expected, actual,
      expected.childNodes.length, actual.childNodes.length, mismatchFactory)
  } else {
    matchEquality(path, expected, actual, mismatchFactory)
  }
}

fun <M : Mismatch> matchEqualsIgnoreOrder(
  path: List<String>,
  expected: Any?,
  actual: Any?,
  expectedSize: Int,
  actualSize: Int,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  return if (expectedSize == actualSize) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} to have $expectedSize elements", path))
  }
}

fun <M : Mismatch> matchMinEqualsIgnoreOrder(
  min: Int,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} with minimum $min at $path" }
  return if (actual is List<*>) {
    if (actual.size < min) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
    } else {
      emptyList()
    }
  } else if (actual is JsonValue.Array) {
    if (actual.size() < min) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
    } else {
      emptyList()
    }
  } else if (actual is Element) {
    if (actual.childNodes.length < min) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have minimum $min", path))
    } else {
      emptyList()
    }
  } else {
    matchEquality(path, expected, actual, mismatchFactory)
  }
}

fun <M : Mismatch> matchMaxEqualsIgnoreOrder(
  max: Int,
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  logger.debug { "comparing ${valueOf(actual)} with maximum $max at $path" }
  return if (actual is List<*>) {
    if (actual.size > max) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
    } else {
      emptyList()
    }
  } else if (actual is JsonValue.Array) {
    if (actual.size() > max) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
    } else {
      emptyList()
    }
  } else if (actual is Element) {
    if (actual.childNodes.length > max) {
      listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to have maximum $max", path))
    } else {
      emptyList()
    }
  } else {
    matchEquality(path, expected, actual, mismatchFactory)
  }
}

fun <M : Mismatch> matchNull(path: List<String>, actual: Any?, mismatchFactory: MismatchFactory<M>): List<M> {
  val matches = actual == null || actual is JsonValue.Null
  logger.debug { "comparing ${valueOf(actual)} to null at $path -> $matches" }
  return if (matches) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(null, actual, "Expected ${valueOf(actual)} to be null", path))
  }
}

private val tika = TikaConfig()

fun <M : Mismatch> matchHeaderWithParameters(
  path: List<String>,
  contentType: ContentType,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  val binaryData = when (actual) {
    is ByteArray -> actual
    else -> actual.toString().toByteArray(contentType.asCharset())
  }
  val metadata = Metadata()
  val stream = TikaInputStream.get(binaryData)
  val detectedContentType = stream.use { stream ->
    tika.detector.detect(stream, metadata)
  }
  val matches = contentType.equals(detectedContentType)
  logger.debug { "Matching binary contents by content type: " +
    "expected '$contentType', detected '$detectedContentType' -> $matches" }
  return if (matches) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(contentType.toString(), actual,
      "Expected binary contents to have content type '$contentType' " +
        "but detected contents was '$detectedContentType'", path))
  }
}

fun matchStatusCode(
  statusType: HttpStatus,
  statusCodes: List<Int>,
  expected: Int,
  actual: Int
): List<StatusMismatch> {
  val matches = when (statusType) {
    HttpStatus.Information -> (100..199).contains(actual)
    HttpStatus.Success -> (200..299).contains(actual)
    HttpStatus.Redirect -> (300..399).contains(actual)
    HttpStatus.ClientError -> (400..499).contains(actual)
    HttpStatus.ServerError -> (500..599).contains(actual)
    HttpStatus.StatusCodes -> statusCodes.contains(actual)
    HttpStatus.NonError -> actual < 400
    HttpStatus.Error -> actual >= 400
  }
  logger.debug {
    "Matching status $actual with $statusType/$statusCodes -> $matches"
  }
  return if (matches) {
    emptyList()
  } else {
    listOf(StatusMismatch(expected, actual, statusType, statusCodes))
  }
}

fun <M : Mismatch> matchSemver(
  path: List<String>,
  expected: Any?,
  actual: Any?,
  mismatchFactory: MismatchFactory<M>
): List<M> {
  val asText = when (actual) {
    is Element -> actual.nodeName
    is Attr -> actual.name
    is JsonValue.StringValue -> actual.value.toString()
    else -> actual.toString()
  }
  val matches = try {
    Version.valueOf(asText)
    true
  } catch (ex: ParseException) {
    false
  } catch (ex: UnexpectedCharacterException) {
    false
  }
  logger.debug {
    "comparing ${valueOf(actual)} (${typeOf(actual)}) as a semantic version at $path -> $matches"
  }
  return if (matches) {
    emptyList()
  } else {
    listOf(mismatchFactory.create(expected, actual,
      "Expected ${valueOf(actual)} (${typeOf(actual)}) to be a semantic version", path))
  }
}

@Suppress("MaxLineLength")
fun matcherCatalogueEntries(): List<CatalogueEntry> {
  return listOf(
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v2-regex"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v2-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-number-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-integer-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-decimal-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-date"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-time"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-datetime"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v2-min-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v2-max-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v2-minmax-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-includes"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-null"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v4-equals-ignore-order"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v4-min-equals-ignore-order"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v4-max-equals-ignore-order"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v4-minmax-equals-ignore-order"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v3-content-type"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v4-array-contains"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v1-equality"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v4-notempty"),
    CatalogueEntry(CatalogueEntryType.MATCHER, CatalogueEntryProviderType.CORE, "core", "v4-semver")
  )
}
