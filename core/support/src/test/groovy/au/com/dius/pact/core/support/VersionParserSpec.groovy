package au.com.dius.pact.core.support

import spock.lang.Specification
import org.antlr.v4.runtime.InputMismatchException
import spock.lang.Unroll

class VersionParserSpec extends Specification {

  def 'parse full version'() {
    expect:
    Version.parse('1.2.3').component1() == new Version(1, 2, 3)
  }

  def 'parse major.minor version'() {
    expect:
    Version.parse('1.2').component1() == new Version(1, 2, null)
  }

  def 'parse invalid version'() {
    expect:
    Version.parse('lkzasdjskjdf').component2() == 'Was expecting an integer at index 0'
  }

  @Unroll
  def 'parse errors'() {
    expect:
    Version.parse(version).component2() == error

    where:

    version      | error
    ''           | 'Was expecting an integer at index 0'
    'sdsd'       | 'Was expecting an integer at index 0'
    '0'          | "Was expecting a '.' at index 1 but got the end of the input"
    '0sass'      | "Was expecting a '.' at index 1 but got 's'"
    '100'        | "Was expecting a '.' at index 3 but got the end of the input"
    '100.'       | 'Was expecting an integer at index 4'
    '100.10.'    | 'Was expecting an integer at index 7'
    '100.10x'    | "Unexpected characters 'x' at index 6"
    '100.10.sss' | 'Was expecting an integer at index 7'
    '100.10.1ss' | "Unexpected characters 'ss' at index 8"
  }
}
