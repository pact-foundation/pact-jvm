package au.com.dius.pact.core.matchers.util

import spock.lang.Specification

class LargestKeyValueSpec extends Specification {
  def 'gets the largest value'() {
    given:
    def largest = new LargestKeyValue<Integer, String>()

    when:
    largest.useIfLarger(1, 'a')
    largest.useIfLarger(3, 'b')
    largest.useIfLarger(2, 'c')

    then:
    largest.key == 3
    largest.value == 'b'
  }
}
