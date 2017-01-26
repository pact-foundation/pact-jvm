package au.com.dius.pact.provider

import au.com.dius.pact.model.OptionalBody
import difflib.Delta
import difflib.Patch
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

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
      diff << "@${delta.original.position}"
      if (delta.original.position > 1) {
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

      diff << ''
    }
    diff
  }

  static generateMismatchDiff(String pathString, String actual, OptionalBody expected) {
    def path = pathString.split('\\.').drop(2) as List
    def actualBody = ''
    if (actual) {
      actualBody = walkGraph(new JsonSlurper().parseText(actual), path)
    }

    def expectedBody = ''
    if (expected?.present) {
      expectedBody = walkGraph(new JsonSlurper().parseText(expected.value), path)
    }

    generateDiff(expectedBody, actualBody)
 }

  private static String walkGraph(data, List<String> path) {
    def filteredData = walk(data, path)
    if (filteredData) {
      JsonOutput.prettyPrint(JsonOutput.toJson(filteredData))
    } else {
      ''
    }
  }

  private static walk(data, List<String> path) {
    if (path) {
      if (data instanceof Map) {
        walk(data[path.first()], path.drop(1))
      } else if (data instanceof List) {
        def p = path.first()
        if (p.matches('\\d+')) {
          walk(data[Integer.parseInt(p)], path.drop(1))
        } else {
          null
        }
      } else {
        null
      }
    } else {
      data
    }
  }
}
