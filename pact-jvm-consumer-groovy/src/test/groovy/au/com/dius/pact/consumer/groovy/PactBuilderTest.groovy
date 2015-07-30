package au.com.dius.pact.consumer.groovy

import org.junit.Before
import org.junit.Test
import scala.None$

class PactBuilderTest {

  def alice_service = new PactBuilder()

  @Before
  void setup() {
    alice_service {
      serviceConsumer "Consumer"
      hasPactWith "Alice Service"
      port 1234
    }
  }

  @Test
  void 'should not define providerState when no given()'() {
    alice_service {
      uponReceiving('a retrieve Mallory request')
      withAttributes(method: 'get', path: '/mallory')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    alice_service.buildInteractions()
    assert alice_service.interactions.size() == 1
    assert alice_service.interactions[0].providerState == None$.empty()
  }

  @Test
  void 'allows matching on paths'() {
    alice_service {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: ~'/mallory/[0-9]+')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    alice_service.buildInteractions()
    assert alice_service.interactions.size() == 1
    assert alice_service.interactions[0].request.path =~ '/mallory/[0-9]+'
    assert alice_service.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  @Test
  void 'allows using the defined matcher on paths'() {
    alice_service {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: regexp(~'/mallory/[0-9]+', '/mallory/1234567890'))
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    alice_service.buildInteractions()
    assert alice_service.interactions.size() == 1
    assert alice_service.interactions[0].request.path == '/mallory/1234567890'
    assert alice_service.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  @Test
  void 'allows matching on headers'() {
    alice_service {
      uponReceiving('a request to match a header')
      withAttributes(method: 'get', path: '/headers', headers: [MALLORY: ~'mallory:[0-9]+'])
      willRespondWith(
        status: 200,
        headers: ['Content-Type': regexp('text/.*', 'text/html')],
        body: '"That is some good Mallory."'
      )
    }
    alice_service.buildInteractions()
    assert alice_service.interactions.size() == 1

    def firstInteraction = alice_service.interactions[0]
    assert firstInteraction.request.headers.get().apply('MALLORY') =~ 'mallory:[0-9]+'
    assert firstInteraction.request.matchingRules.get().apply('$.headers.MALLORY').apply('regex') == 'mallory:[0-9]+'
    assert firstInteraction.response.headers.get().apply('Content-Type') == 'text/html'
    assert firstInteraction.response.matchingRules.get().apply('$.headers.Content-Type').apply('regex') == 'text/.*'
  }
}
