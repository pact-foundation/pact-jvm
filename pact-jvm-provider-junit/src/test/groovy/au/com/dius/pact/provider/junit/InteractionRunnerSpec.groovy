package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.TestClass
import spock.lang.Specification

class InteractionRunnerSpec extends Specification {

  @SuppressWarnings('PublicInstanceField')
  class InteractionRunnerTestClass {
    @TestTarget
    public final Target target = new HttpTarget(8332)
  }

  def 'do not publish verification results if any interactions have been filtered'() {
    given:
    def interaction1 = new RequestResponseInteraction(description: 'Interaction 1')
    def interaction2 = new RequestResponseInteraction(description: 'Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def clazz = new TestClass(InteractionRunnerTestClass)
    def filteredPact = new FilteredPact(pact, { it.description == 'Interaction 1' })
    def runner = Spy(InteractionRunner, constructorArgs: [clazz, filteredPact, UnknownPactSource.INSTANCE])

    when:
    runner.run([:] as RunNotifier)

    then:
    0 * runner.reportVerificationResults(false)
  }

}
