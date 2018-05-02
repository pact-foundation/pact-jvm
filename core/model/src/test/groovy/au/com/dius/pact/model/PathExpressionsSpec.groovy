package au.com.dius.pact.model

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class PathExpressionsSpec extends Specification {

  def 'Parse Path Exp Handles Empty String'() {
    expect:
    PathExpressionsKt.parsePath('') == []
  }

  def 'Parse Path Exp Handles Root'() {
    expect:
    PathExpressionsKt.parsePath('$') == [PathToken.Root.INSTANCE]
  }

  def 'Parse Path Exp Handles Missing Root'() {
    when:
    PathExpressionsKt.parsePath('adsjhaskjdh')

    then:
    def ex = thrown(InvalidPathExpression)
    ex.message == 'Path expression "adsjhaskjdh" does not start with a root marker "$"'
  }

  def 'Parse Path Exp Handles Missing Path'() {
    when:
    PathExpressionsKt.parsePath('$adsjhaskjdh')

    then:
    def ex = thrown(InvalidPathExpression)
    ex.message == 'Expected a "." or "[" instead of "a" in path expression "$adsjhaskjdh" at index 1'
  }

  @Unroll
  def 'Parse Path Exp Handles Missing Path Name in "#expression"'() {
    when:
    PathExpressionsKt.parsePath(expression)

    then:
    def ex = thrown(InvalidPathExpression)
    ex.message == message

    where:

    expression | message
    '$.'       | 'Expected a path after "." in path expression "$." at index 1'
    '$.a.b.c.' | 'Expected a path after "." in path expression "$.a.b.c." at index 7'
  }

  @Unroll
  def 'Parse Path Exp Handles Invalid Identifiers in "#expression"'() {
    when:
    PathExpressionsKt.parsePath(expression)

    then:
    def ex = thrown(InvalidPathExpression)
    ex.message == message

    where:

    expression  | message
    '$.abc!'    | '"!" is not allowed in an identifier in path expression "$.abc!" at index 5'
    '$.a.b.c.}' | 'Expected either a "*" or path identifier in path expression "$.a.b.c.}" at index 8'
  }

  @Unroll
  def 'Parse Path Exp With Simple Identifiers - #expression'() {
    expect:
    PathExpressionsKt.parsePath(expression) == result

    where:

    expression | result
    '$.a'      | [PathToken.Root.INSTANCE, new PathToken.Field('a')]
    '$.a-b'    | [PathToken.Root.INSTANCE, new PathToken.Field('a-b')]
    '$.a_b'    | [PathToken.Root.INSTANCE, new PathToken.Field('a_b')]
    '$._b'     | [PathToken.Root.INSTANCE, new PathToken.Field('_b')]
    '$.a.b.c'  | [PathToken.Root.INSTANCE, new PathToken.Field('a'), new PathToken.Field('b'),
                  new PathToken.Field('c')]
  }

  @Unroll
  def 'Parse Path Exp With Star Instead Of Identifiers - #expression'() {
    expect:
    PathExpressionsKt.parsePath(expression) == result

    where:

    expression | result
    '$.*'      | [PathToken.Root.INSTANCE, PathToken.Star.INSTANCE]
    '$.a.*.c'  | [PathToken.Root.INSTANCE, new PathToken.Field('a'), PathToken.Star.INSTANCE,
                  new PathToken.Field('c')]
  }

  @Unroll
  def 'Parse Path Exp With Bracket Notation - #expression'() {
    expect:
    PathExpressionsKt.parsePath(expression) == result

    where:

    expression         | result
    "\$['val1']"       | [PathToken.Root.INSTANCE, new PathToken.Field('val1')]
    "\$.a['val@1.'].c" | [PathToken.Root.INSTANCE, new PathToken.Field('a'), new PathToken.Field('val@1.'),
                          new PathToken.Field('c')]
    "\$.a[1].c"        | [PathToken.Root.INSTANCE, new PathToken.Field('a'), new PathToken.Index(1),
                          new PathToken.Field('c')]
    "\$.a[*].c"        | [PathToken.Root.INSTANCE, new PathToken.Field('a'), PathToken.StarIndex.INSTANCE,
                          new PathToken.Field('c')]
  }

  @Unroll
  def 'Parse Path Exp With Invalid Bracket Notation - #expression'() {
    when:
    PathExpressionsKt.parsePath(expression)

    then:
    def ex = thrown(InvalidPathExpression)
    ex.message == message

    where:

    expression                | message
    '$['                      | 'Expected a "\'" (single quote) or a digit in path expression "$[" after index 1'
    '$[\''                    | 'Unterminated string in path expression "$[\'" at index 2'
    '$[\'Unterminated string' | 'Unterminated string in path expression "$[\'Unterminated string" at index 21'
    '$[\'\']'                 | 'Empty strings are not allowed in path expression "$[\'\']" at index 3'
    '$[\'test\'.b.c'          | 'Unterminated brackets, found "." instead of "]" in path expression "$[\'test\'.b.c" at index 8'
    '$[\'test\''              | 'Unterminated brackets in path expression "$[\'test\'" at index 2'
    '$[\'test\']b.c'          | 'Expected a "." or "[" instead of "b" in path expression "$[\'test\']b.c" at index 9'
  }

  @Unroll
  def 'Parse Path Exp With Invalid Bracket Index Notation - #expression'() {
    when:
    PathExpressionsKt.parsePath(expression)

    then:
    def ex = thrown(InvalidPathExpression)
    ex.message == message

    where:

    expression | message
    '$[dhghh]' | 'Indexes can only consist of numbers or a "*", found "d" instead in path expression "$[dhghh]" at index 2'
    '$[12abc]' | 'Indexes can only consist of numbers or a "*", found "a" instead in path expression "$[12abc]" at index 4'
    '$[]'      | 'Empty bracket expressions are not allowed in path expression "$[]" at index 2'
    '$[-1]'    | 'Indexes can only consist of numbers or a "*", found "-" instead in path expression "$[-1]" at index 2'
  }

}
