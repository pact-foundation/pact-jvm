package au.com.dius.pact.core.support.json

import spock.lang.Issue
import spock.lang.Specification

class JsonValueSpec extends Specification {

  @SuppressWarnings('JUnitPublicField')
  public static final String JSON_POINTER_EXAMPLE = '''{
      "foo": ["bar", "baz"],
      "": 0,
      "a/b": 1,
      "c%d": 2,
      "e^f": 3,
      "g|h": 4,
      "i\\\\j": 5,
      "k\\"l": 6,
      " ": 7,
      "m~n": 8
    }'''

  @Issue('#1416')
  def 'serialise with special chars in keys'() {
    expect:
    JsonParser.parseString('{"ä": "abc"}').serialise() == '{"ä":"abc"}'
  }

  def 'json pointer'() {
    given:
    def json = JsonParser.parseString(JSON_POINTER_EXAMPLE)

    expect:
    json.pointer(pointer).serialise() == result

    where:

    pointer  | result
    ''       | JsonParser.parseString(JSON_POINTER_EXAMPLE).serialise()
    '/foo'   | '["bar","baz"]'
    '/x'     | 'null'
    '/foo/0' | '"bar"'
    '/foo/2' | 'null'
    '/'      | '0'
    '/a~1b'  | '1'
    '/c%d'   | '2'
    '/e^f'   | '3'
    '/g|h'   | '4'
    '/i\\j'  | '5'
    '/k"l'   | '6'
    '/ '     | '7'
    '/m~0n'  | '8'
  }
}
