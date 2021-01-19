package au.com.dius.pact.core.model.generators

import spock.lang.Rollup
import spock.lang.Specification

class RandomDecimalGeneratorSpec extends Specification {

  @Rollup
  def 'generates a value with a decimal point and only a leading zero if the point is in the second position'() {
    given:
    def generator = new RandomDecimalGenerator(8)

    expect:
    with(generator.generate([:], null).toString()) {
      it.length() == 9
      it ==~ /^\d+\.\d+/
      it[0] != '0' || (it[0] == '0' && it[1] == '.')
    }

    where:
    _samples << (1..100).step(1)
  }

  def 'handle edge case when digits == 1'() {
    expect:
    new RandomDecimalGenerator(1).generate([:], null).toString() ==~ /^\d$/
  }

  def 'handle edge case when digits == 2'() {
    expect:
    new RandomDecimalGenerator(2).generate([:], null).toString() ==~ /^\d\.\d$/
  }
}
