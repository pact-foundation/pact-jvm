package au.com.dius.pact.consumer.dsl

import spock.lang.Specification
import spock.lang.Unroll

class DslSpec extends Specification {

  @Unroll
  def 'correctly generates a key for an attribute name'() {
    expect:
    Dsl.matcherKey(name, 'a.b.c.') == result

    where:

    name         | result
    'a'          | 'a.b.c.a'
    'a1'         | 'a.b.c.a1'
    '_a'         | 'a.b.c._a'
    '@a'         | 'a.b.c.@a'
    '#a'         | 'a.b.c.#a'
    'b-a'        | 'a.b.c.b-a'
    'b:a'        | 'a.b.c.b:a'
    '01/01/2001' | "a.b.c['01/01/2001']"
    'a['         | "a.b.c['a[']"
  }
}
