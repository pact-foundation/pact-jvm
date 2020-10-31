package au.com.dius.pact.core.support

import spock.lang.Specification
import org.antlr.v4.runtime.InputMismatchException

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
    Version.parse('lkzasdjskjdf').component2() instanceof InputMismatchException
  }
}
