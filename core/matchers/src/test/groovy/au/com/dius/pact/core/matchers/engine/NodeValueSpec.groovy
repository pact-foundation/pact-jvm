package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class NodeValueSpec extends Specification {
  def escapeString() {
    expect:
    NodeValue.escape(str) == result

    where:

    str                     | result
    ''                      | "''"
    'nospaces'              | 'nospaces'
    'some spaces'           | "'some spaces'"
    'he said "some spaces"' | "'he said \\\"some spaces\\\"'"
    "he said 'some spaces'" | "'he said \\'some spaces\\''"
  }

  def strForm() {
    expect:
    value.strForm() == result

    where:

    value                                                                     | result
    NodeValue.NULL.INSTANCE                                                   | 'NULL'
    new NodeValue.STRING('string')                                            | 'string'
    new NodeValue.STRING('a string')                                          | "'a string'"
    new NodeValue.BOOL(true)                                                  | 'BOOL(true)'
    new NodeValue.MMAP([:])                                                   | '{}'
    new NodeValue.MMAP([a: ['A']])                                            | '{a: A}'
    new NodeValue.MMAP([a: ['']])                                             | "{a: ''}"
    new NodeValue.MMAP([a: ['A'], b: ['B 1', 'B2']])                          | "{a: A, b: ['B 1', B2]}"
    new NodeValue.SLIST(['A', 'B 1', 'B2'])                                   | "[A, 'B 1', B2]"
    new NodeValue.SLIST([])                                                   | '[]'
    new NodeValue.LIST([new NodeValue.STRING('A'), new NodeValue.BOOL(true)]) | '[A, BOOL(true)]'
    new NodeValue.LIST([])                                                    | '[]'
    new NodeValue.BARRAY([1, 2, 3, 65] as byte[])                             | 'BYTES(4, AQIDQQ==)'
    new NodeValue.NAMESPACED('stuff', '*&^%$ %^&*&^')                         | 'stuff:*&^%$ %^&*&^'
    new NodeValue.UINT(1234)                                                  | 'UINT(1234)'
    new NodeValue.JSON(new JsonValue.StringValue('this is a string'))         | 'json:"this is a string"'
    new NodeValue.ENTRY('key', new NodeValue.STRING('A'))                     | 'key -> A'
    new NodeValue.ENTRY('a key', new NodeValue.BOOL(false))                   | "'a key' -> BOOL(false)"
  }
}
