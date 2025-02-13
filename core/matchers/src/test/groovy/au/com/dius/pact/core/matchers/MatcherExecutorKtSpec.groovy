package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.support.json.JsonValue
import groovy.transform.TupleConstructor
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.UserDataHandler
import spock.lang.Specification

import static au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher.NumberType.NUMBER

@SuppressWarnings(['LineLength', 'SpaceAroundOperator', 'GetterMethodCouldBeProperty'])
class MatcherExecutorKtSpec extends Specification {

  def 'match regex'() {
    expect:
    MatcherExecutorKt.matchRegex(regex, [], '', actual, { a, b, c, d -> new HeaderMismatch('test', '', actual, c) } as MismatchFactory) == result

    where:

    regex                           | actual          | result
    'look|look_bordered|slider_cta' | 'look_bordered' | []
  }

  def 'match semver'() {
    expect:
    MatcherExecutorKt.matchSemver(['$'], '1.2.3', actual, { a, b, c, d -> new HeaderMismatch('test', '', actual.toString(), c) } as MismatchFactory) == result

    where:

    actual                             | result
    '4.5.7'                            | []
    '4.5.7.8'                          | [new HeaderMismatch('test', '', '4.5.7.8', "'4.5.7.8' is not a valid semantic version")]
    '04.5.7'                           | [new HeaderMismatch('test', '', '04.5.7', "'04.5.7' is not a valid semantic version")]
    new JsonValue.StringValue('4.5.7') | []
  }

  def 'matching numbers'() {
    given:
    def mismatchFactory = { a, b, c, d -> new HeaderMismatch('test', '', actual.toString(), c) } as MismatchFactory

    expect:
    MatcherExecutorKt.matchNumber(NUMBER, ['$'], expected, actual, mismatchFactory, null)?[0]?.mismatch == result

    where:

    expected | actual                               | result
    null     | null                                 | 'Expected null (Null) to be a number'
    null     | '4.5'                                | "Expected '4.5' (String) to be a null value"
    100      | null                                 | 'Expected null (Null) to be a number'
    100      | '4.5'                                | "Expected '4.5' (String) to be a number"
    100      | 4.5                                  | null
    100      | 4                                    | null
    100      | new JsonValue.StringValue('4.5.7.8') | "Expected '4.5.7.8' (String) to be a number"
    100      | new JsonValue.Integer(200)           | null
    100      | new JsonValue.Decimal(200.10)        | null
    100      | false                                | 'Expected false (Boolean) to be a number'
    100      | [100]                                | 'Expected [100] (Array) to be a number'
    100      | [a: 200.3, b: 200, c: 300]           | 'Expected {a=200.3, b=200, c=300} (LinkedHashMap) to be a number'
    100      | 2.300g                               | null
    100      | 2.300d                               | null
    100      | new TestNode('not a number')         | 'Expected TestNode(not a number) (TestNode) to be a number'
    100      | new TestNode('22.33.44')             | 'Expected TestNode(22.33.44) (TestNode) to be a number'
    100      | new TestNode('22.33')                | null
  }

  def 'matching numbers with coercion enabled'() {
    given:
    def mismatchFactory = { a, b, c, d -> new HeaderMismatch('test', '', actual.toString(), c) } as MismatchFactory
    def context = new MatchingContext(new MatchingRuleCategory('test'), false, [:], true)

    expect:
    MatcherExecutorKt.matchNumber(NUMBER, ['$'], expected, actual, mismatchFactory, context)?[0]?.mismatch == result

    where:

    expected | actual                               | result
    100      | '4.5'                                | null
    100      | 4.5                                  | null
    100      | 4                                    | null
    100      | new JsonValue.StringValue('4.5.7.8') | "Expected '4.5.7.8' (String) to be a number"
    100      | new JsonValue.StringValue('4.5')     | null
    100      | new JsonValue.Integer(200)           | null
    100      | new JsonValue.Decimal(200.10)        | null
  }

  @SuppressWarnings('UnnecessaryCast')
  def 'matching integer values'() {
    expect:
    MatcherExecutorKt.matchInteger(value, null) == result

    where:

    value             | result
    '100'             | false
    '100x'            | false
    100               | true
    100.0             | false
    100i              | true
    100l              | true
    100 as BigInteger | true
    100g              | true
    BigInteger.ZERO   | true
    BigDecimal.ZERO   | true
  }

  @SuppressWarnings('UnnecessaryCast')
  def 'matching integer values with coercion enabled'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('test'), false, [:], true)

    expect:
    MatcherExecutorKt.matchInteger(value, context) == result

    where:

    value             | result
    '100'             | true
    '100x'            | false
    'x100'            | false
    100               | true
  }

  @SuppressWarnings('UnnecessaryCast')
  def 'matching decimal number values'() {
    expect:
    MatcherExecutorKt.matchDecimal(value, null) == result

    where:

    value                            | result
    new JsonValue.Decimal('0'.chars) | true
    '100'                            | false
    '100.0'                          | false
    100                              | false
    100.0                            | true
    100.0f                           | true
    100.0d                           | true
    100i                             | false
    100l                             | false
    100 as BigInteger                | false
    BigInteger.ZERO                  | false
    BigDecimal.ZERO                  | true
  }

  @SuppressWarnings('UnnecessaryCast')
  def 'matching decimal number values with coercion enabled'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('test'), false, [:], true)

    expect:
    MatcherExecutorKt.matchDecimal(value, context) == result

    where:

    value                            | result
    new JsonValue.Decimal('0'.chars) | true
    '100'                            | false
    '100.0'                          | true
    '100.0x'                         | false
    'x100.0'                         | false
  }

  @TupleConstructor
  @SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
  static class TestNode implements Node {
    String value

    String toString() { "TestNode($value)" }

    String getNodeName() { 'TestNode' }
    String getNodeValue() { value }
    void setNodeValue(String nodeValue) { }
    short getNodeType() { 0 }
    Node getParentNode() { null }
    NodeList getChildNodes() { null }
    Node getFirstChild() { null }
    Node getLastChild() { null }
    Node getPreviousSibling() { null }
    Node getNextSibling() { null }
    NamedNodeMap getAttributes() { null }
    Document getOwnerDocument() { null }
    Node insertBefore(Node newChild, Node refChild) { null }
    Node replaceChild(Node newChild, Node oldChild) { null }
    Node removeChild(Node oldChild) { null }
    Node appendChild(Node newChild) { null }
    boolean hasChildNodes() { false }
    Node cloneNode(boolean deep) { null }
    void normalize() { }
    boolean isSupported(String feature, String version) { false }
    String getNamespaceURI() { null }
    String getPrefix() { null }
    void setPrefix(String prefix) { }
    String getLocalName() { null }
    boolean hasAttributes() { false }
    String getBaseURI() { null }
    short compareDocumentPosition(Node other) { 0 }
    String getTextContent() { null }
    void setTextContent(String textContent) { }
    boolean isSameNode(Node other) { false }
    String lookupPrefix(String namespaceURI) { null }
    boolean isDefaultNamespace(String namespaceURI) { false }
    String lookupNamespaceURI(String prefix) { null }
    boolean isEqualNode(Node arg) { false }
    Object getFeature(String feature, String version) { null }
    Object setUserData(String key, Object data, UserDataHandler handler) { null }
    Object getUserData(String key) { null }
  }
}
