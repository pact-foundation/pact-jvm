package au.com.dius.pact.core.matchers.engine.interpreter

import au.com.dius.pact.core.matchers.engine.ExecutionPlanNode
import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.NodeResult
import au.com.dius.pact.core.matchers.engine.NodeValue
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.matchers.engine.PlanNodeType
import au.com.dius.pact.core.matchers.engine.resolvers.ValueResolver
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.DocPath
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class ExecutionPlanInterpreterV2SyncSpec extends Specification {
  private static final byte[] FORM_BYTES = 'a=1&a=2&b=hello+world'.bytes
  private static final String FORM_STRING = 'a=1&a=2&b=hello+world'

  private PlanMatchingContext context
  private ExecutionPlanInterpreter interpreter
  private final ValueResolver resolver = Stub(ValueResolver)

  def setup() {
    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp('test interaction')
    context = new PlanMatchingContext(pact, interaction, new MatchingConfiguration(false, false, true, false))
    interpreter = new ExecutionPlanInterpreter(context)
  }

  def 'form parse converts bytes and strings into multi-value maps'() {
    given:
    def node = actionNode('form:parse')
    node.add(valueNode(value))

    when:
    def result = interpreter.walkTree(['$'], node, resolver)

    then:
    result.result == new NodeResult.VALUE(expected)

    where:
    value                             | expected
    new NodeValue.BARRAY(FORM_BYTES) | multiMap(a: ['1', '2'], b: ['hello world'])
    new NodeValue.STRING(FORM_STRING) | multiMap(a: ['1', '2'], b: ['hello world'])
    NodeValue.NULL.INSTANCE          | NodeValue.NULL.INSTANCE
  }

  def 'resolve stack value supports multi-value maps'() {
    given:
    interpreter.pushResult(new NodeResult.VALUE(new NodeValue.MMAP([a: ['1'], b: ['2', '3']])))

    expect:
    interpreter.resolveStackValue(path).unwrap() == expected

    where:
    path               | expected
    DocPath.root()     | new NodeValue.MMAP([a: ['1'], b: ['2', '3']])
    new DocPath('$.a') | new NodeValue.STRING('1')
    new DocPath('$.b') | new NodeValue.SLIST(['2', '3'])
    new DocPath('$.c') | NodeValue.NULL.INSTANCE
  }

  def 'match each key records child mismatches per failing key'() {
    given:
    def matcherParams = new JsonValue.Object([
      value: Json.toJson(''),
      rules: new JsonValue.Array([Json.toJson([match: 'regex', regex: '^[a-z]+$'])])
    ])
    def node = actionNode('match:each-key')
    node.add(valueNode(NodeValue.NULL.INSTANCE))
    node.add(valueNode(new NodeValue.JSON(new JsonValue.Object([
      good: new JsonValue.Integer(1),
      'bad-key': new JsonValue.Integer(2)
    ]))))
    node.add(jsonValueNode(matcherParams))

    when:
    def result = interpreter.walkTree(['$'], node, resolver)
    def childErrors = result.children.findAll {
      it.nodeType instanceof PlanNodeType.ACTION && it.nodeType.action.startsWith('each-key:')
    }

    then:
    result.result == new NodeResult.VALUE(new NodeValue.BOOL(false))
    childErrors*.nodeType*.action == ['each-key:bad-key']
    childErrors[0].result.message == "Expected 'bad-key' to match '^[a-z]+\$'"
  }

  def 'match each value supports json arrays'() {
    given:
    def matcherParams = new JsonValue.Object([
      value: Json.toJson('100'),
      rules: new JsonValue.Array([Json.toJson([match: 'number'])])
    ])
    def node = actionNode('match:each-value')
    node.add(jsonValueNode(new JsonValue.Array([new JsonValue.Integer(100)])))
    node.add(jsonValueNode(new JsonValue.Array([
      new JsonValue.Integer(1),
      new JsonValue.Integer(2)
    ])))
    node.add(jsonValueNode(matcherParams))

    when:
    def result = interpreter.walkTree(['$'], node, resolver)

    then:
    result.result == new NodeResult.VALUE(new NodeValue.BOOL(true))
    result.children.findAll {
      it.nodeType instanceof PlanNodeType.ACTION && it.nodeType.action.startsWith('each-value:')
    }*.nodeType*.action == []
  }

  def 'match each value supports json objects'() {
    given:
    def matcherParams = new JsonValue.Object([
      value: Json.toJson('100'),
      rules: new JsonValue.Array([Json.toJson([match: 'number'])])
    ])
    def node = actionNode('match:each-value')
    node.add(jsonValueNode(new JsonValue.Object([a: new JsonValue.Integer(100)])))
    node.add(jsonValueNode(new JsonValue.Object([
      a: new JsonValue.Integer(1),
      b: new JsonValue.Integer(2)
    ])))
    node.add(jsonValueNode(matcherParams))

    when:
    def result = interpreter.walkTree(['$'], node, resolver)

    then:
    result.result == new NodeResult.VALUE(new NodeValue.BOOL(true))
    result.children.findAll {
      it.nodeType instanceof PlanNodeType.ACTION && it.nodeType.action.startsWith('each-value:')
    }*.nodeType*.action == []
  }

  def 'match each value supports string lists'() {
    given:
    def matcherParams = new JsonValue.Object([
      value: Json.toJson('1'),
      rules: new JsonValue.Array([Json.toJson([match: 'regex', regex: '\\d+'])])
    ])
    def node = actionNode('match:each-value')
    node.add(valueNode(new NodeValue.SLIST(['1'])))
    node.add(valueNode(new NodeValue.SLIST(['1', 'oops'])))
    node.add(jsonValueNode(matcherParams))

    when:
    def result = interpreter.walkTree(['$'], node, resolver)

    then:
    result.result == new NodeResult.VALUE(new NodeValue.BOOL(false))
    result.children.findAll {
      it.nodeType instanceof PlanNodeType.ACTION && it.nodeType.action.startsWith('each-value:')
    }*.nodeType*.action == ['each-value:1']
  }

  private static ExecutionPlanNode actionNode(String action) {
    new ExecutionPlanNode(new PlanNodeType.ACTION(action), null, [])
  }

  private static ExecutionPlanNode valueNode(NodeValue value) {
    new ExecutionPlanNode(new PlanNodeType.VALUE(value), null, [])
  }

  private static ExecutionPlanNode jsonValueNode(JsonValue jsonValue) {
    valueNode(new NodeValue.JSON(jsonValue))
  }

  private static NodeValue.MMAP multiMap(Map<String, List<String>> values) {
    new NodeValue.MMAP(values)
  }
}
