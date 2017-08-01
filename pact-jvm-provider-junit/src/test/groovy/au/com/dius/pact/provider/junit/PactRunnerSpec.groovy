package au.com.dius.pact.provider.junit

import au.com.dius.pact.provider.junit.loader.PactFolder
import au.com.dius.pact.provider.junit.loader.PactUrl
import org.junit.runners.model.InitializationError
import spock.lang.Specification

@SuppressWarnings('UnusedObject')
class PactRunnerSpec extends Specification {

  @Provider('Bob')
  class NoSourceTestClass {

  }

  @Provider('Bob')
  @PactUrl(urls = ["http://doesn't exist/I hope?"])
  class FailsTestClass {

  }

  @Provider('Bob')
  @PactFolder('pacts')
  class NoPactsTestClass {

  }

  @Provider('Bob')
  @PactFolder('pacts')
  @IgnoreNoPactsToVerify
  class NoPactsIgnoredTestClass {

  }

  def 'PactRunner throws an exception if there is no @Provider annotation on the test class'() {
    when:
    new PactRunner(PactRunnerSpec)

    then:
    InitializationError e = thrown()
    e.causes*.message ==
      ['Provider name should be specified by using au.com.dius.pact.provider.junit.Provider annotation']
  }

  def 'PactRunner throws an exception if there is no pact source'() {
    when:
    new PactRunner(NoSourceTestClass)

    then:
    InitializationError e = thrown()
    e.causes*.message == ['Exactly one pact source should be set']
  }

  def 'PactRunner throws an exception if the pact source throws an IO exception'() {
    when:
    new PactRunner(FailsTestClass)

    then:
    InitializationError e = thrown()
    e.causes*.message == ["Unable to process url: http://doesn't exist/I hope?"]
  }

  def 'PactRunner throws an exception if there are no pacts to verify'() {
    when:
    new PactRunner(NoPactsTestClass)

    then:
    InitializationError e = thrown()
    e.causes*.message == ['Did not find any pact files for provider Bob']
  }

  def 'PactRunner does not throw an exception if there are no pacts to verify and @IgnoreNoPactsToVerify'() {
    when:
    new PactRunner(NoPactsIgnoredTestClass)

    then:
    notThrown(InitializationError)
  }

}
