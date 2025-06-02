package au.com.dius.pact.core.matchers.engine

import spock.lang.Specification

class ExecutionPlanNodeSpec extends Specification {
  def escapeString() {
    expect:
    ExecutionPlanNode.escape(str) == result

    where:

    str                     | result
    ''                      | ''
    'nospaces'              | 'nospaces'
    'some spaces'           | "'some spaces'"
    'he said "some spaces"' | "'he said \"some spaces\"'"
    "he said 'some spaces'" | "\"he said 'some spaces'\""
  }
}
