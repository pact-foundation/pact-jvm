package au.com.dius.pact.core.support.json

import com.github.michaelbull.result.Err
import spock.lang.Specification
import spock.lang.Unroll

class JsonLexerSpec extends Specification {

  @Unroll
  def 'next token - #description'() {
    given:
    def lexer = new JsonLexer(new StringSource(json.chars))

    when:
    def token = lexer.nextToken()

    then:
    token.value == tokenValue

    where:

    description                     | json               | tokenValue
    'empty document'                | ''                 | null
    'whitespace'                    | '  \t\r\n'         | JsonToken.Whitespace.INSTANCE
    'digit'                         | '6'                | new JsonToken.Integer('6'.chars)
    'integer'                       | '1234'             | new JsonToken.Integer('1234'.chars)
    'negative integer'              | '-666'             | new JsonToken.Integer('-666'.chars)
    'null value'                    | 'null'             | JsonToken.Null.INSTANCE
    'true value'                    | 'true'             | JsonToken.True.INSTANCE
    'false value'                   | 'false'            | JsonToken.False.INSTANCE
    'decimal'                       | '1234.65'          | new JsonToken.Decimal('1234.65'.chars)
    'decimal with exponent'         | '1234.65E-4'       | new JsonToken.Decimal('1234.65E-4'.chars)
    'decimal with exponent 2'       | '12345E4'          | new JsonToken.Decimal('12345E4'.chars)
    'string'                        | '"12345E4"'        | new JsonToken.StringValue('12345E4'.chars)
    'string with escaped chars'     | '"123\\"45E4"'     | new JsonToken.StringValue('123"45E4'.chars)
    'string with escaped hex chars' | '"123\\uaBcD45E4"' | new JsonToken.StringValue('123\uaBcD45E4'.chars)
    'array start'                   | '['                | JsonToken.ArrayStart.INSTANCE
    'array end'                     | ']'                | JsonToken.ArrayEnd.INSTANCE
    'object start'                  | '{'                | JsonToken.ObjectStart.INSTANCE
    'object end'                    | '}'                | JsonToken.ObjectEnd.INSTANCE
    'comma'                         | ','                | JsonToken.Comma.INSTANCE
    'colon'                         | ':'                | JsonToken.Colon.INSTANCE
  }

  @Unroll
  def 'invalid next token - #description'() {
    given:
    def lexer = new JsonLexer(new StringSource(json.chars))

    when:
    def token = lexer.nextToken()

    then:
    token instanceof Err

    where:

    description                             | json
    'invalid characters similar to null'    | 'nue'
    'invalid characters similar to true'    | 'trnull'
    'invalid characters similar to false'   | 'fals'
    'invalid number'                        | '123.'
    'invalid number 2'                      | '123.e2'
    'invalid exponent'                      | '123ex'
    'unterminated string'                   | '"123ex'
    'string with invalid escape'            | '"12\\3ex"'
    'string with invalid escaped hex chars' | '"12\\uabxex"'
  }

}
