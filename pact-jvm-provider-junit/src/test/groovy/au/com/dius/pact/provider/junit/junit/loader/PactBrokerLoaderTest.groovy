package au.com.dius.pact.provider.junit.junit.loader

import au.com.dius.pact.provider.junit.loader.PactBroker
import au.com.dius.pact.provider.junit.loader.PactBrokerLoader
import com.github.rholder.retry.Attempt
import com.github.rholder.retry.RetryException
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.junit.Before
import org.junit.Test

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.is

@PactBroker(host = 'pactbroker.host', port = '1000')
class PactBrokerLoaderTest {

  private Closure<PactBrokerLoader> pactBrokerLoader
  private String host
  private String port
  private String protocol
  private Closure<HttpResponse> closure

  @Before
  void setup() {
    host = 'pactbroker'
    port = '1234'
    protocol = 'http'
    pactBrokerLoader = {
      PactBrokerLoader pactBrokerLoader = new PactBrokerLoader(host, port, protocol)
      pactBrokerLoader.setHttpResponseCallable(closure as Callable<HttpResponse>)
      pactBrokerLoader
    }
  }

  @Test
  void 'Returns Empty List On 404'() {
    closure = { mockResponse(404) }
    assertThat(pactBrokerLoader.call().load('test'), is(empty()))
  }

  @Test(expected = IOException)
  void 'Throws Io Exception On Execution Exception'() {
    closure = { throw new ExecutionException('message', null) }
    pactBrokerLoader.call().load('test')
  }

  @Test(expected = IOException)
  void 'Throws Io Exception On Retry Exception'() {
    closure = { throw new RetryException(1, [:] as Attempt) }
    pactBrokerLoader.call().load('test')
  }

  @Test(expected = RuntimeException)
  void 'Throws Runtime Exception On Any Non 200 status'() {
    closure = { mockResponse(401) }
    pactBrokerLoader.call().load('test')
  }

  @Test
  void 'Loads Pacts Configured From A Pact Broker Annotation'() {
    closure = { mockResponse(404) }
    pactBrokerLoader = {
      PactBrokerLoader pactBrokerLoader = new PactBrokerLoader(this.class.getAnnotation(PactBroker))
      pactBrokerLoader.setHttpResponseCallable(closure)
      pactBrokerLoader
    }
    assertThat(pactBrokerLoader.call().load('test'), is(empty()))
  }

  private static HttpResponse mockResponse(Integer status) {
    [
      getStatusLine: {
        [
          getStatusCode: { status }
        ] as StatusLine
      }
    ] as HttpResponse
  }

}
