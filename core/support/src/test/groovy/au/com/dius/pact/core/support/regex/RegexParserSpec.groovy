package au.com.dius.pact.core.support.regex

import spock.lang.Specification
import spock.lang.Unroll

class RegexParserSpec extends Specification {

  // ── Literals ──────────────────────────────────────────────────────────────

  def 'parses a single literal character'() {
    when:
    def node = new RegexParser('a').parse()

    then:
    node instanceof RegexNode.Literal
    node.char == ('a' as char)
  }

  def 'parses a sequence of literals'() {
    when:
    def node = new RegexParser('abc').parse()

    then:
    node instanceof RegexNode.Sequence
    node.nodes.size() == 3
    node.nodes[0] instanceof RegexNode.Literal
    (node.nodes[0] as RegexNode.Literal).char == ('a' as char)
    node.nodes[1] instanceof RegexNode.Literal
    (node.nodes[1] as RegexNode.Literal).char == ('b' as char)
    node.nodes[2] instanceof RegexNode.Literal
    (node.nodes[2] as RegexNode.Literal).char == ('c' as char)
  }

  // ── Escape sequences ──────────────────────────────────────────────────────

  @Unroll
  def 'parses escaped literal #desc as Literal(#expected)'() {
    when:
    def node = new RegexParser(pattern).parse()

    then:
    node instanceof RegexNode.Literal
    node.char == (expected as char)

    where:
    desc           | pattern | expected
    'newline'      | '\\n'   | '\n'
    'tab'          | '\\t'   | '\t'
    'carriage ret' | '\\r'   | '\r'
    'dot'          | '\\.'   | '.'
    'asterisk'     | '\\*'   | '*'
    'open paren'   | '\\('   | '('
    'caret'        | '\\^'   | '^'
    'dollar'       | '\\$'   | '$'
    'backslash'    | '\\\\'  | '\\'
  }

  def 'parses anchors as empty sequences'() {
    expect:
    new RegexParser('^').parse() instanceof RegexNode.Sequence
    new RegexParser('$').parse() instanceof RegexNode.Sequence
  }

  // ── Predefined character classes ─────────────────────────────────────────

  def 'parses \\w as a word-character class'() {
    when:
    def node = new RegexParser('\\w').parse()

    then:
    node instanceof RegexNode.CharClass
    !node.negated
    with(node.toCharSet()) {
      contains('a' as char)
      contains('z' as char)
      contains('A' as char)
      contains('Z' as char)
      contains('0' as char)
      contains('9' as char)
      contains('_' as char)
      !contains(' ' as char)
      !contains('@' as char)
    }
  }

  def 'parses \\d as a digit class'() {
    when:
    def node = new RegexParser('\\d').parse()

    then:
    node instanceof RegexNode.CharClass
    !node.negated
    def charset = node.toCharSet()
    ('0'..'9').every { charset.contains(it as char) }
    !charset.contains('a' as char)
  }

  def 'parses \\s as a whitespace class'() {
    when:
    def node = new RegexParser('\\s').parse()

    then:
    node instanceof RegexNode.CharClass
    !node.negated
    def charset = node.toCharSet()
    charset.contains(' ' as char)
    charset.contains('\t' as char)
  }

  def 'parses \\W as a non-word class'() {
    when:
    def node = new RegexParser('\\W').parse()

    then:
    node instanceof RegexNode.CharClass
    def charset = node.toCharSet()
    // must NOT include word chars
    !charset.contains('a' as char)
    !charset.contains('Z' as char)
    !charset.contains('5' as char)
    !charset.contains('_' as char)
    // must include common non-word chars
    charset.contains(' ' as char)
    charset.contains('@' as char)
  }

  def 'parses \\D as a non-digit class'() {
    when:
    def node = new RegexParser('\\D').parse()

    then:
    node instanceof RegexNode.CharClass
    def charset = node.toCharSet()
    !charset.contains('0' as char)
    !charset.contains('9' as char)
    charset.contains('a' as char)
  }

  def 'parses \\S as a non-whitespace class'() {
    when:
    def node = new RegexParser('\\S').parse()

    then:
    node instanceof RegexNode.CharClass
    def charset = node.toCharSet()
    !charset.contains(' ' as char)
    charset.contains('a' as char)
    charset.contains('!' as char)
  }

  // ── Custom character classes ──────────────────────────────────────────────

  def 'parses a simple character class'() {
    when:
    def node = new RegexParser('[abc]').parse()

    then:
    node instanceof RegexNode.CharClass
    !node.negated
    node.chars.containsAll(['a' as char, 'b' as char, 'c' as char])
  }

  def 'parses a character class with a range'() {
    when:
    def node = new RegexParser('[a-z]').parse()

    then:
    node instanceof RegexNode.CharClass
    !node.negated
    node.ranges.size() == 1
    node.ranges[0].first == ('a' as char)
    node.ranges[0].second == ('z' as char)
  }

  def 'parses a negated character class'() {
    when:
    def node = new RegexParser('[^abc]').parse()

    then:
    node instanceof RegexNode.CharClass
    node.negated
    node.chars.containsAll(['a' as char, 'b' as char, 'c' as char])
  }

  def 'parses a character class with multiple ranges and chars'() {
    when:
    def node = new RegexParser('[a-zA-Z0-9_]').parse()
    def charset = node.toCharSet()

    then:
    node instanceof RegexNode.CharClass
    !node.negated
    charset.contains('a' as char)
    charset.contains('Z' as char)
    charset.contains('5' as char)
    charset.contains('_' as char)
    !charset.contains(' ' as char)
    !charset.contains('@' as char)
  }

  def 'parses a character class containing \\w'() {
    when:
    def node = new RegexParser('[\\w-]').parse()
    def charset = node.toCharSet()

    then:
    node instanceof RegexNode.CharClass
    !node.negated
    charset.contains('a' as char)
    charset.contains('Z' as char)
    charset.contains('5' as char)
    charset.contains('_' as char)
    charset.contains('-' as char)
  }

  def 'treats ] as a literal when it is the first character in a class'() {
    when:
    def node = new RegexParser('[]]').parse()

    then:
    node instanceof RegexNode.CharClass
    node.chars.contains(']' as char)
  }

  // ── Any-char ──────────────────────────────────────────────────────────────

  def 'parses the dot as AnyChar'() {
    when:
    def node = new RegexParser('.').parse()

    then:
    node instanceof RegexNode.AnyChar
  }

  // ── Alternation ───────────────────────────────────────────────────────────

  def 'parses a two-way alternation'() {
    when:
    def node = new RegexParser('a|b').parse()

    then:
    node instanceof RegexNode.Alternation
    node.alternatives.size() == 2
    node.alternatives[0] instanceof RegexNode.Literal
    (node.alternatives[0] as RegexNode.Literal).char == ('a' as char)
    node.alternatives[1] instanceof RegexNode.Literal
    (node.alternatives[1] as RegexNode.Literal).char == ('b' as char)
  }

  def 'parses a three-way alternation'() {
    when:
    def node = new RegexParser('foo|bar|baz').parse()

    then:
    node instanceof RegexNode.Alternation
    node.alternatives.size() == 3
  }

  // ── Groups ────────────────────────────────────────────────────────────────

  def 'parses a capturing group'() {
    when:
    def node = new RegexParser('(abc)').parse()

    then:
    node instanceof RegexNode.Group
  }

  def 'parses a non-capturing group'() {
    when:
    def node = new RegexParser('(?:abc)').parse()

    then:
    node instanceof RegexNode.Group
  }

  def 'parses a named group'() {
    when:
    def node = new RegexParser('(?<name>abc)').parse()

    then:
    node instanceof RegexNode.Group
  }

  def 'parses lookahead as empty sequence'() {
    when:
    def node = new RegexParser('(?=abc)').parse()

    then:
    node instanceof RegexNode.Sequence
    (node as RegexNode.Sequence).nodes.empty
  }

  def 'parses negative lookahead as empty sequence'() {
    when:
    def node = new RegexParser('(?!abc)').parse()

    then:
    node instanceof RegexNode.Sequence
    (node as RegexNode.Sequence).nodes.empty
  }

  def 'parses lookbehind as empty sequence'() {
    when:
    def node = new RegexParser('(?<=abc)').parse()

    then:
    node instanceof RegexNode.Sequence
    (node as RegexNode.Sequence).nodes.empty
  }

  // ── Quantifiers ───────────────────────────────────────────────────────────

  def 'parses * as Repeat(0, MAX_VALUE)'() {
    when:
    def node = new RegexParser('a*').parse()

    then:
    node instanceof RegexNode.Repeat
    node.min == 0
    node.max == Integer.MAX_VALUE
  }

  def 'parses + as Repeat(1, MAX_VALUE)'() {
    when:
    def node = new RegexParser('a+').parse()

    then:
    node instanceof RegexNode.Repeat
    node.min == 1
    node.max == Integer.MAX_VALUE
  }

  def 'parses ? as Repeat(0, 1)'() {
    when:
    def node = new RegexParser('a?').parse()

    then:
    node instanceof RegexNode.Repeat
    node.min == 0
    node.max == 1
  }

  def 'parses {n} as exact repetition'() {
    when:
    def node = new RegexParser('a{3}').parse()

    then:
    node instanceof RegexNode.Repeat
    node.min == 3
    node.max == 3
  }

  def 'parses {n,} as Repeat(n, MAX_VALUE)'() {
    when:
    def node = new RegexParser('a{2,}').parse()

    then:
    node instanceof RegexNode.Repeat
    node.min == 2
    node.max == Integer.MAX_VALUE
  }

  def 'parses {n,m} as bounded repetition'() {
    when:
    def node = new RegexParser('a{2,5}').parse()

    then:
    node instanceof RegexNode.Repeat
    node.min == 2
    node.max == 5
  }

  def 'treats lazy modifier ? after quantifier as decorative'() {
    when:
    def node = new RegexParser('a+?').parse()

    then:
    node instanceof RegexNode.Repeat
    node.min == 1
    node.max == Integer.MAX_VALUE
  }

  def 'treats { with no following digit as a literal'() {
    when:
    def node = new RegexParser('a{b}').parse()

    then:
    // 'a' is a Literal, '{' is treated as a literal because no digit follows
    node instanceof RegexNode.Sequence
    node.nodes[0] instanceof RegexNode.Literal
    node.nodes[1] instanceof RegexNode.Literal
    (node.nodes[1] as RegexNode.Literal).char == ('{' as char)
  }

  // ── Word boundaries ───────────────────────────────────────────────────────

  def 'parses word boundary \\b as empty sequence'() {
    when:
    def node = new RegexParser('\\b').parse()

    then:
    node instanceof RegexNode.Sequence
    (node as RegexNode.Sequence).nodes.empty
  }

  // ── Unicode properties ────────────────────────────────────────────────────

  @Unroll
  def 'parses \\p{#name} as a non-empty CharClass'() {
    when:
    def node = new RegexParser("\\p{$name}").parse()

    then:
    node instanceof RegexNode.CharClass
    !node.toCharSet().empty

    where:
    name << ['Alpha', 'Digit', 'Upper', 'Lower', 'Alnum', 'XDigit', 'ASCII']
  }

  def 'parses negated \\P{Alpha} as negated CharClass'() {
    when:
    def node = new RegexParser('\\P{Alpha}').parse()

    then:
    node instanceof RegexNode.CharClass
    node.negated
    def chars = node.toCharSet()
    !chars.contains('a' as char)
    chars.contains('1' as char)
  }
}
