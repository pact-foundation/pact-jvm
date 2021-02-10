package au.com.dius.pact.provider.reporters

import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import spock.lang.Specification

class SLF4JReporterSpec extends Specification {

  def 'can create an instance of a reporter'() {
    expect:
    ReporterManager.createReporter('slf4j', '/tmp/' as File) != null
  }

  def 'can log the verification of a consumer from the PactBroker'() {
    given:
    def reporter = ReporterManager.createReporter('slf4j', '/tmp/' as File)
    def consumer = new ConsumerInfo(name: 'Pact between Foo Web Client (v1.0.2) and Activity Service')
    def provider = new ProviderInfo(name: 'Activity Service')

    ListAppender<ILoggingEvent> testAppender = setupTestLogAppender()

    when:
    reporter.reportVerificationForConsumer(consumer, provider, null)

    then:
    def loggedEvent = testAppender.list.get(0)
    loggedEvent.message.contains('Verifying a pact between Foo Web Client (v1.0.2) and Activity Service')
  }

  def 'can log the verification joining the consumer and provider names'() {
    given:
    def reporter = ReporterManager.createReporter('slf4j', '/tmp/' as File)
    def consumer = new ConsumerInfo(name: 'Foo Web Client')
    def provider = new ProviderInfo(name: 'Activity Service')

    ListAppender<ILoggingEvent> testAppender = setupTestLogAppender()

    when:
    reporter.reportVerificationForConsumer(consumer, provider, null)

    then:
    def loggedEvent = testAppender.list.get(0)
    loggedEvent.message.contains('Verifying a pact between Foo Web Client and Activity Service')
  }

  private static ListAppender<ILoggingEvent> setupTestLogAppender() {
    def testAppender = new ListAppender<ILoggingEvent>()
    testAppender.start()

    def logger = (Logger) LoggerFactory.getLogger(SLF4JReporter)
    logger.addAppender(testAppender)

    testAppender
  }

}
