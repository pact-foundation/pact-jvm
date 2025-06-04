package au.com.dius.pact.core.matchers.engine

import spock.lang.Specification

@SuppressWarnings(['LineLength', 'AbcMetric', 'ExplicitCallToAndMethod'])
class NodeResultSpec extends Specification {
  def 'node result and'() {
    expect:
    a.and(b) == result

    where:
    a                                               | b                                               | result
    NodeResult.OK.INSTANCE                          | null                                            | NodeResult.OK.INSTANCE
    new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | null                                            | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)
    new NodeResult.ERROR('')                        | null                                            | new NodeResult.ERROR('')
    NodeResult.OK.INSTANCE                          | NodeResult.OK.INSTANCE                          | NodeResult.OK.INSTANCE
    NodeResult.OK.INSTANCE                          | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)
    NodeResult.OK.INSTANCE                          | new NodeResult.ERROR('error')                   | new NodeResult.ERROR('error')
    new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | NodeResult.OK.INSTANCE                          | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)
    new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)
    new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | new NodeResult.VALUE(new NodeValue.UINT(100))   | new NodeResult.VALUE(new NodeValue.UINT(100))
    new NodeResult.VALUE(new NodeValue.BOOL(false)) | new NodeResult.VALUE(new NodeValue.UINT(100))   | new NodeResult.VALUE(new NodeValue.BOOL(false))
    new NodeResult.VALUE(new NodeValue.BOOL(true))  | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | new NodeResult.VALUE(new NodeValue.BOOL(false))
    new NodeResult.VALUE(new NodeValue.BOOL(true))  | new NodeResult.VALUE(new NodeValue.BOOL(false)) | new NodeResult.VALUE(new NodeValue.BOOL(false))
    new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | new NodeResult.ERROR('error')                   | new NodeResult.ERROR('error')
    new NodeResult.ERROR('error')                   | NodeResult.OK.INSTANCE                          | new NodeResult.ERROR('error')
    new NodeResult.ERROR('error')                   | new NodeResult.VALUE(NodeValue.NULL.INSTANCE)   | new NodeResult.ERROR('error')
    new NodeResult.ERROR('error')                   | new NodeResult.ERROR('error2')                  | new NodeResult.ERROR('error')
  }
}
