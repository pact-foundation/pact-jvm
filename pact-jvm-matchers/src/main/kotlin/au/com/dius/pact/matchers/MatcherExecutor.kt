package au.com.dius.pact.matchers

import mu.KotlinLogging
import scala.collection.Seq
import scala.xml.Elem

private val logger = KotlinLogging.logger {}

fun valueOf(value: Any?): String {
  if (value == null) {
    return "null"
  } else if (value is String) {
    return "'$value'"
  } else {
    return value.toString()
  }
}

fun safeToString(value: Any?): String {
  if (value == null) {
    return ""
  } else if (value is Elem) {
    return value.text()
  } else {
    return value.toString()
  }
}

fun <Mismatch> matchInclude(includedValue: String, path: List<String>, expected: Any?, actual: Any?, mismatchFactory: MismatchFactory<Mismatch>): List<Mismatch> {
  val matches = safeToString(actual).contains(includedValue)
  logger.debug { "comparing if ${valueOf(actual)} includes '$includedValue' at $path -> $matches" }
  if (matches) {
    return listOf()
  } else {
    return listOf(mismatchFactory.create(expected, actual, "Expected ${valueOf(actual)} to include ${valueOf(includedValue)}", path))
  }
}
