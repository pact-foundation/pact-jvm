package au.com.dius.pact.provider.reporters

import spock.lang.Specification

class SLF4JReporterSpec extends Specification {

  def 'can create an instance of a reporter'() {
    expect:
    ReporterManager.createReporter('slf4j', '/tmp/' as File) != null
  }

}
