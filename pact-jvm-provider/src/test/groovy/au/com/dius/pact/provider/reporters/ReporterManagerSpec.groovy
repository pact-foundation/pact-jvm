package au.com.dius.pact.provider.reporters

import spock.lang.Specification

class ReporterManagerSpec extends Specification {

  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'returns boolean if a report is defined'() {
    expect:
    ReporterManager.reporterDefined(name) == isDefined

    where:
    name        || isDefined
    'console'   || true
    'Console'   || false
    'markdown'  || true
    'json'      || true
    'other'     || false
  }

  def 'returns true for all defined reports'() {
    expect:
    ReporterManager.reporterDefined(name)

    where:
    name << ReporterManager.availableReporters()
  }

  def 'can create an instance of a reporter'() {
    expect:
    ReporterManager.createReporter('console') != null
  }

  def 'when creating an instance of a reporter, sets the name if there is one to set'() {
    expect:
    ReporterManager.createReporter('json').name == 'json'
  }

  def 'can create an instance using fully qualified name'() {
    expect:
    ReporterManager.createReporter('au.com.dius.pact.provider.reporters.AnsiConsoleReporter') != null
  }

  def 'should throw an exception for none reporter classes'() {
    when:
      ReporterManager.createReporter('au.com.dius.pact.provider.ConsumerInfo')
    then:
      thrown(IllegalArgumentException)
  }

}
