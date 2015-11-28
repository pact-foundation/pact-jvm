package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.PactVerification
import spock.lang.Specification

class GradleProviderInfoSpec extends Specification {

  def 'defaults the consumer verification type to what is set on the provider'() {
    given:
    def provider = new GradleProviderInfo('provider')
    provider.verificationType = PactVerification.ANNOTATED_METHOD

    when:
    provider.hasPactWith('boddy the consumer') {

    }

    then:
    provider.consumers.first().verificationType == PactVerification.ANNOTATED_METHOD
  }

}
