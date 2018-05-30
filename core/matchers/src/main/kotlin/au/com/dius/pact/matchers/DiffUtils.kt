package au.com.dius.pact.matchers

import groovy.json.JsonOutput

private const val NEW_LINE = '\n'

fun generateDiff(expectedBodyString: String, actualBodyString: String): List<String> {
  val expectedLines = expectedBodyString.split(NEW_LINE)
  val actualLines = actualBodyString.split(NEW_LINE)
  val patch = difflib.DiffUtils.diff(expectedLines, actualLines)

  val diff = mutableListOf<String>()

  patch.deltas.forEach { delta ->
    if (delta.original.position >= 1 && (diff.isEmpty() || expectedLines[delta.original.position - 1] != diff.last())) {
      diff.add(expectedLines[delta.original.position - 1])
    }

    delta.original.lines.forEach {
      diff.add("-$it")
    }
    delta.revised.lines.forEach {
      diff.add("+$it")
    }

    val pos = delta.original.position + delta.original.lines.size
    if (pos < expectedLines.size) {
      diff.add(expectedLines[pos])
    }
  }
  return diff
}

fun generateObjectDiff(expected: Any?, actual: Any?): String {
    var actualJson = ""
    if (actual != null) {
      actualJson = JsonOutput.prettyPrint(JsonOutput.toJson(actual))
    }

    var expectedJson = ""
    if (expected != null) {
      expectedJson = JsonOutput.prettyPrint(JsonOutput.toJson(expected))
    }

    return generateDiff(expectedJson, actualJson).joinToString(separator = NEW_LINE.toString())
}
