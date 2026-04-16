package au.com.dius.pact.core.support.regex

import au.com.dius.pact.core.support.Random as PactRandom
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['LineLength', 'UnnecessaryGString'])
class RegexStringGeneratorSpec extends Specification {

  // Run each pattern multiple times to catch non-deterministic failures
  private static final int ITERATIONS = 20

  /**
   * Core property: every value generated from a pattern must fully match that pattern.
   */
  @Unroll
  def 'generated string matches pattern: #description'() {
    expect:
    ITERATIONS.times {
      assert RegexStringGenerator.generate(pattern) ==~ regex
    }

    where:
    description                | pattern                                  | regex
    'word chars'               | '\\w+'                                   | /\w+/
    'digits'                   | '\\d+'                                   | /\d+/
    'non-digits'               | '\\D+'                                   | /\D+/
    'whitespace'               | '\\s+'                                   | /\s+/
    'non-whitespace'           | '\\S+'                                   | /\S+/
    'lowercase range'          | '[a-z]+'                                 | /[a-z]+/
    'uppercase range'          | '[A-Z]+'                                 | /[A-Z]+/
    'digit range'              | '[0-9]+'                                 | /[0-9]+/
    'hex digits'               | '[0-9a-fA-F]+'                           | /[0-9a-fA-F]+/
    'literal string'           | 'hello'                                  | /hello/
    'literal with digits'      | 'v\\d+\\.\\d+'                           | /v\d+\.\d+/
    'optional char'            | 'colou?r'                                | /colou?r/
    'alternation'              | 'cat|dog'                                | /cat|dog/
    'three-way alternation'    | 'one|two|three'                          | /one|two|three/
    'capturing group'          | '(ab)+'                                  | /(ab)+/
    'non-capturing group'      | '(?:ab)+'                                | /(?:ab)+/
    'exact repetition'         | '\\d{4}'                                 | /\d{4}/
    'min repetition'           | '\\d{2,}'                                | /\d{2,}/
    'range repetition'         | '\\d{2,5}'                               | /\d{2,5}/
    'negated class'            | '[^0-9]+'                                | /[^0-9]+/
    'negated word class'       | '[^a-zA-Z]+'                             | /[^a-zA-Z]+/
    'any char'                 | '.+'                                     | /.+/
    'dot star'                 | '.*'                                     | /.*/
    'escaped dot'              | 'a\\.b'                                  | /a\.b/
    'UUID segment'             | '[0-9a-f]{8}-[0-9a-f]{4}'               | /[0-9a-f]{8}-[0-9a-f]{4}/
    'date-like'                | '\\d{4}-\\d{2}-\\d{2}'                   | /\d{4}-\d{2}-\d{2}/
    'mixed literal and class'  | 'foo[0-9]{3}bar'                        | /foo[0-9]{3}bar/
    'start with uppercase'     | '[A-Z][a-z]+'                           | /[A-Z][a-z]+/
    'alpha class via \\p'      | '\\p{Alpha}+'                            | /[a-zA-Z]+/
    'digit class via \\p'      | '\\p{Digit}+'                            | /[0-9]+/
    'uppercase via \\p'        | '\\p{Upper}+'                            | /[A-Z]+/
    'xdigit via \\p'           | '\\p{XDigit}+'                           | /[0-9a-fA-F]+/
  }

  // ── Anchor stripping (via Random.generateRandomString) ───────────────────

  @Issue('#1826')
  def 'generates a valid string from pattern with anchors via Random helper'() {
    expect:
    ITERATIONS.times {
      assert PactRandom.generateRandomString('^\\w+$') ==~ /\w+/
    }
  }

  def 'does not strip escaped anchor chars'() {
    expect:
    ITERATIONS.times {
      assert PactRandom.generateRandomString('\\^\\w+\\$') ==~ /\^\w+\$/
    }
  }

  def 'generates a valid string from unanchored pattern via Random helper'() {
    expect:
    ITERATIONS.times {
      assert PactRandom.generateRandomString('\\w+') ==~ /\w+/
    }
  }

  // ── Edge cases ────────────────────────────────────────────────────────────

  def 'empty pattern generates empty string'() {
    expect:
    RegexStringGenerator.generate('') == ''
  }

  def 'anchor-only pattern generates empty string'() {
    expect:
    RegexStringGenerator.generate('^') == ''
    RegexStringGenerator.generate('$') == ''
    RegexStringGenerator.generate('^$') == ''
  }

  def 'star quantifier on literal generates zero or more occurrences'() {
    when:
    def results = (1..100).collect { RegexStringGenerator.generate('a*') }

    then:
    results.every { it ==~ /a*/ }
    results.any { it == '' }           // zero repetitions must occur in 100 tries
    results.any { it.length() > 0 }   // non-zero repetitions must occur
  }

  def 'plus quantifier on literal generates one or more occurrences'() {
    when:
    def results = (1..50).collect { RegexStringGenerator.generate('a+') }

    then:
    results.every { it ==~ /a+/ }
    results.every { it.length() >= 1 }
  }

  def 'optional element is sometimes absent and sometimes present'() {
    when:
    def results = (1..100).collect { RegexStringGenerator.generate('ab?c') }

    then:
    results.every { it ==~ /ab?c/ }
    results.any { it == 'ac' }
    results.any { it == 'abc' }
  }

  def 'exact repetition produces exactly that many characters'() {
    when:
    def results = (1..20).collect { RegexStringGenerator.generate('\\d{4}') }

    then:
    results.every { it ==~ /\d{4}/ }
    results.every { it.length() == 4 }
  }

  def 'negated char class avoids excluded chars'() {
    when:
    def results = (1..30).collect { RegexStringGenerator.generate('[^abc]+') }

    then:
    results.every { it ==~ /[^abc]+/ }
    results.every { !it.contains('a') && !it.contains('b') && !it.contains('c') }
  }

  def 'alternation produces only valid alternatives'() {
    when:
    def results = (1..100).collect { RegexStringGenerator.generate('true|false') }

    then:
    results.every { it == 'true' || it == 'false' }
    results.any { it == 'true' }
    results.any { it == 'false' }
  }

  def 'word-boundary assertions produce no visible output'() {
    expect:
    ITERATIONS.times {
      assert RegexStringGenerator.generate('\\bfoo\\b') == 'foo'
    }
  }

  def 'generates strings from a complex email-like pattern'() {
    given:
    def emailPattern = '[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,4}'

    expect:
    ITERATIONS.times {
      assert RegexStringGenerator.generate(emailPattern) ==~ /[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,4}/
    }
  }

  def 'generates strings from a full UUID pattern'() {
    given:
    def uuidPattern = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

    expect:
    ITERATIONS.times {
      assert RegexStringGenerator.generate(uuidPattern) ==~
        /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/
    }
  }
}
