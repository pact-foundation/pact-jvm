package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.junit.VerificationReports
import au.com.dius.pact.core.support.expressions.ValueResolver
import org.junit.runners.model.TestClass
import spock.lang.Specification

@SuppressWarnings('CoupledTestCase')
class HttpTargetSpec extends Specification {

  private HttpTarget httpTarget
  private ProviderVerifier verifier
  private ValueResolver resolver

  @VerificationReports(['console', 'markdown'])
  class StubTest {

  }

  def setup() {
    httpTarget = new HttpTarget('localhost', 9000)
    verifier = Mock(ProviderVerifier)
    resolver = Mock(ValueResolver)
    httpTarget.setValueResolver(resolver)
  }

  def 'by default only enables the console reporter'() {
    given:
    httpTarget.setTestClass(new TestClass(HttpTargetSpec), this)

    when:
    httpTarget.setupReporters(verifier)

    then:
    1 * verifier.setReporters { r ->  r*.class*.simpleName == ['AnsiConsoleReporter'] }
  }

  def 'enables the verification reports if there is an annotation on the test class'() {
    given:
    httpTarget.setTestClass(new TestClass(StubTest), new StubTest())

    when:
    httpTarget.setupReporters(verifier)

    then:
    1 * verifier.setReporters { r ->  r*.class*.simpleName == ['AnsiConsoleReporter', 'MarkdownReporter'] }
  }

  @SuppressWarnings('ClosureStatementOnOpeningLineOfMultipleLineClosure')
  def 'enables the verification reports if there is java properties defined'() {
    given:
    httpTarget.setTestClass(new TestClass(HttpTargetSpec), this)
    resolver.propertyDefined('pact.verification.reports') >> true
    resolver.resolveValue('pact.verification.reports:') >> 'markdown,json'
    resolver.resolveValue(_) >> { args ->
      if (args[0].startsWith('pact.verification.reportDir')) {
        'target/reports/pact'
      } else {
        null
      }
    }

    when:
    httpTarget.setupReporters(verifier)

    then:
    1 * verifier.setReporters { r ->  r*.class*.simpleName == ['AnsiConsoleReporter', 'MarkdownReporter',
                                                               'JsonReporter'] }
  }

  @SuppressWarnings('ClosureStatementOnOpeningLineOfMultipleLineClosure')
  def 'handles white space in the report names'() {
    given:
    httpTarget.setTestClass(new TestClass(HttpTargetSpec), this)
    resolver.propertyDefined('pact.verification.reports') >> true
    resolver.resolveValue('pact.verification.reports:') >> 'markdown  ,\tjson '
    resolver.resolveValue(_) >> { args ->
      if (args[0].startsWith('pact.verification.reportDir')) {
        'target/reports/pact'
      } else {
        null
      }
    }

    when:
    httpTarget.setupReporters(verifier)

    then:
    1 * verifier.setReporters { r ->  r*.class*.simpleName == ['AnsiConsoleReporter', 'MarkdownReporter',
                                                               'JsonReporter'] }
  }

  def 'handles an empty pact.verification.reports'() {
    given:
    httpTarget.setTestClass(new TestClass(HttpTargetSpec), this)
    resolver.propertyDefined('pact.verification.reports') >> true
    resolver.resolveValue('pact.verification.reports:') >> ''
    resolver.resolveValue(_) >> { args ->
      if (args[0].startsWith('pact.verification.reportDir')) {
        'target/reports/pact'
      } else {
        null
      }
    }

    when:
    httpTarget.setupReporters(verifier)

    then:
    1 * verifier.setReporters { r ->  r*.class*.simpleName == ['AnsiConsoleReporter'] }
  }

}
