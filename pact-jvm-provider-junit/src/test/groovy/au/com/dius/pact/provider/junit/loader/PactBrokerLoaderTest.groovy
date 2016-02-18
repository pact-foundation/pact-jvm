package au.com.dius.pact.provider.junit.loader

import com.github.rholder.retry.Attempt
import com.github.rholder.retry.RetryException
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.junit.Before
import org.junit.Test

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

@PactBroker(host = 'pactbroker.host', port = '1000')
class PactBrokerLoaderTest {

  private static final String DEFAULT_BODY = '''
    {
    }
  '''
  private Closure<PactBrokerLoader> pactBrokerLoader
  private String host
  private String port
  private String protocol
  private Closure<HttpResponse> closure
  private List tags

  @Before
  void setup() {
    host = 'pactbroker'
    port = '1234'
    protocol = 'http'
    tags = ['latest']
    pactBrokerLoader = {
      PactBrokerLoader pactBrokerLoader = new PactBrokerLoader(host, port, protocol, tags)
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

  @Test
  void 'Loads pacts for each provided tag'() {
    def count = 0
    closure = { count++; mockResponse(200) }
    tags = ['latest', 'a', 'b', 'c']
    pactBrokerLoader.call().load('test')
    assertThat(count, is(equalTo(4)))
  }

  private static HttpResponse mockResponse(Integer status, String body = DEFAULT_BODY) {
    [
      getStatusLine: {
        [
          getStatusCode: { status }
        ] as StatusLine
      },
      getEntity: {
        [
          getContent: { new ByteArrayInputStream(body.bytes) }
        ] as HttpEntity
      }
    ] as HttpResponse
  }

}
