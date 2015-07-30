package au.com.dius.pact.consumer.groovy

import org.junit.Before
import org.junit.Test
@SuppressWarnings('UnusedImport')
import scala.None$

class PactBuilderTest {

  private final aliceService = new PactBuilder()

  @Before
  void setup() {
    aliceService {
      serviceConsumer 'Consumer'
      hasPactWith 'Alice Service'
      port 1234
    }
  }

  @Test
  void 'should not define providerState when no given()'() {
    aliceService {
      uponReceiving('a retrieve Mallory request')
      withAttributes(method: 'get', path: '/mallory')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1
    assert aliceService.interactions[0].providerState == None$.empty()
  }

  @Test
  void 'allows matching on paths'() {
    aliceService {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: ~'/mallory/[0-9]+')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1
    assert aliceService.interactions[0].request.path =~ '/mallory/[0-9]+'
    assert aliceService.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  @Test
  void 'allows using the defined matcher on paths'() {
    aliceService {
      uponReceiving('a request to match by path')
      withAttributes(method: 'get', path: regexp(~'/mallory/[0-9]+', '/mallory/1234567890'))
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'text/html'],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1
    assert aliceService.interactions[0].request.path == '/mallory/1234567890'
    assert aliceService.interactions[0].request.matchingRules.get().apply('$.path').apply('regex') == '/mallory/[0-9]+'
  }

  @Test
  void 'allows matching on headers'() {
    aliceService {
      uponReceiving('a request to match a header')
      withAttributes(method: 'get', path: '/headers', headers: [MALLORY: ~'mallory:[0-9]+'])
      willRespondWith(
        status: 200,
        headers: ['Content-Type': regexp('text/.*', 'text/html')],
        body: '"That is some good Mallory."'
      )
    }
    aliceService.buildInteractions()
    assert aliceService.interactions.size() == 1

    def firstInteraction = aliceService.interactions[0]
    assert firstInteraction.request.headers.get().apply('MALLORY') =~ 'mallory:[0-9]+'
    assert firstInteraction.request.matchingRules.get().apply('$.headers.MALLORY').apply('regex') == 'mallory:[0-9]+'
    assert firstInteraction.response.headers.get().apply('Content-Type') == 'text/html'
    assert firstInteraction.response.matchingRules.get().apply('$.headers.Content-Type').apply('regex') == 'text/.*'
  }
}
