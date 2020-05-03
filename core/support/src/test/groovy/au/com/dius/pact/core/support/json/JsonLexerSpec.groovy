package au.com.dius.pact.core.support.json

import arrow.core.Either
import spock.lang.Specification
import spock.lang.Unroll

class JsonLexerSpec extends Specification {

  @Unroll
  def 'next token - #description'() {
    given:
    def lexer = new JsonLexer(new JsonSource.StringSource(json, 0))

    when:
    def token = lexer.nextToken()

    then:
    token.b == tokenValue

    where:

    description                     | json               | tokenValue
    'empty document'                | ''                 | null
    'whitespace'                    | '  \t\r\n'         | new JsonToken.Whitespace('  \t\r\n')
    'digit'                         | '6'                | new JsonToken.Integer('6')
    'integer'                       | '1234'             | new JsonToken.Integer('1234')
    'negative integer'              | '-666'             | new JsonToken.Integer('-666')
    'null value'                    | 'null'             | JsonToken.Null.INSTANCE
    'true value'                    | 'true'             | JsonToken.True.INSTANCE
    'false value'                   | 'false'            | JsonToken.False.INSTANCE
    'decimal'                       | '1234.65'          | new JsonToken.Decimal('1234.65')
    'decimal with exponent'         | '1234.65E-4'       | new JsonToken.Decimal('1234.65E-4')
    'decimal with exponent 2'       | '12345E4'          | new JsonToken.Decimal('12345E4')
    'string'                        | '"12345E4"'        | new JsonToken.StringValue('12345E4')
    'string with escaped chars'     | '"123\\"45E4"'     | new JsonToken.StringValue('123"45E4')
    'string with escaped hex chars' | '"123\\uaBcD45E4"' | new JsonToken.StringValue('123\uaBcD45E4')
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
    def lexer = new JsonLexer(new JsonSource.StringSource(json, 0))

    when:
    def token = lexer.nextToken()

    then:
    token instanceof Either.Left

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
