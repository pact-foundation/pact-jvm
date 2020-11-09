package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.support.json.JsonValue
import difflib.ChangeDelta

private const val NEW_LINE = '\n'

fun generateDiff(expectedBodyString: String, actualBodyString: String): List<String> {
  val expectedLines = expectedBodyString.split(NEW_LINE)
  val actualLines = actualBodyString.split(NEW_LINE)
  val patch = difflib.DiffUtils.diff(expectedLines, actualLines)

  val diff = mutableListOf<String>()
  var line = 0
  patch.deltas.forEach { delta ->
    when (delta) {
      is ChangeDelta<*> -> {
        if (delta.original.position >= 1 && (diff.isEmpty() || expectedLines[delta.original.position - 1] != diff.last())) {
          diff.addAll(expectedLines.slice(line until delta.original.position))
        }

        delta.original.lines.forEach {
          diff.add("-$it")
        }
        delta.revised.lines.forEach {
          diff.add("+$it")
        }

        line = delta.original.position + delta.original.lines.size
      }
    }
  }
  if (line < expectedLines.size) {
    diff.addAll(expectedLines.listIterator(line).asSequence())
  }
  return diff
}

fun generateJsonDiff(expected: JsonValue, actual: JsonValue): String {
  val actualJson = actual.prettyPrint()
  val expectedJson = expected.prettyPrint()
  return generateDiff(expectedJson, actualJson).joinToString(separator = NEW_LINE.toString())
}
