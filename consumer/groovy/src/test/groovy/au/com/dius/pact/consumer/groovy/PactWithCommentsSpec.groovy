package au.com.dius.pact.consumer.groovy

import spock.lang.Specification

class PactWithCommentsSpec extends Specification {

  @SuppressWarnings('PrivateFieldCouldBeFinal')
  private PactBuilder providerService = new PactBuilder()

  void setup() {
    providerService {
      serviceConsumer 'ConsumerWithComments'
      hasPactWith 'Provider'
    }
  }

  @SuppressWarnings('LineLength')
  def 'allows comments'() {
    given:
    providerService {
      uponReceiving('a request')
      withAttributes(method: 'get', path: '/testPath')
      willRespondWith(status: 200)
      comment('This allows me to specify just a bit more information about the interaction')
      comment('It has no functional impact, but can be displayed in the broker HTML page, and potentially in the test output')
      comment('It could even contain the name of the running test on the consumer side to help marry the interactions back to the test case')
      testname('allows comments')
    }

    when:
    providerService.updateInteractions()

    then:
    providerService.interactions.size() == 1
    providerService.interactions[0].comments.testname == 'allows comments'
    providerService.interactions[0].comments.text.get(0) == 'This allows me to specify just a bit more information about the interaction'
    providerService.interactions[0].comments.text.get(1) == 'It has no functional impact, but can be displayed in the broker HTML page, and potentially in the test output'
    providerService.interactions[0].comments.text.get(2) == 'It could even contain the name of the running test on the consumer side to help marry the interactions back to the test case'
  }
}
