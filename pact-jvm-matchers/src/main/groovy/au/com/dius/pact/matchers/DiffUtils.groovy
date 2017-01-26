package au.com.dius.pact.matchers

import difflib.Delta
import difflib.Patch
import groovy.json.JsonOutput

/**
 * Utility methods for generating diffs
 */
class DiffUtils {

  private static final String NL = '\n'

  static generateDiff(String expectedBodyString, String actualBodyString) {
    def expectedLines = expectedBodyString.split(NL) as List
    def actualLines = actualBodyString.split(NL) as List
    Patch<String> patch = difflib.DiffUtils.diff(expectedLines, actualLines)

    def diff = []

    patch.deltas.each { Delta<String> delta ->
      if (diff.empty || delta.original.position > 1 && expectedLines[delta.original.position - 1] != diff.last()) {
        diff << expectedLines[delta.original.position - 1]
      }

      delta.original.lines.each {
        diff << "-$it"
      }
      delta.revised.lines.each {
        diff << "+$it"
      }

      int pos = delta.original.position + delta.original.lines.size()
      if (pos < expectedLines.size()) {
        diff << expectedLines[pos]
      }
    }
    diff
  }

  static String generateObjectDiff(expected, actual) {
    def actualJson = ''
    if (actual) {
      actualJson = JsonOutput.prettyPrint(JsonOutput.toJson(actual))
    }

    def expectedJson = ''
    if (expected) {
      expectedJson = JsonOutput.prettyPrint(JsonOutput.toJson(expected))
    }

    generateDiff(expectedJson, actualJson).join(NL)
 }
}
