package au.com.dius.pact.provider.junit.loader;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@PactBroker(host = "pactbroker.host", port = "1000")
public class PactBrokerLoaderTest {

  private Supplier<PactBrokerLoader> pactBrokerLoader;
  private String host;
  private String port;
  private String protocol;
  private Callable<HttpResponse> callable;

  @Before
  public void setup() {
    host = "pactbroker";
    port = "1234";
    protocol = "http";
    pactBrokerLoader = () -> {
      PactBrokerLoader pactBrokerLoader = new PactBrokerLoader(host, port, protocol);
      pactBrokerLoader.setHttpResponseCallable(callable);
      return pactBrokerLoader;
    };
  }

  @Test
  public void returnsEmptyListOn404() throws IOException {
    callable = () -> mockResponse(404);
    assertThat(pactBrokerLoader.get().load("test"), is(empty()));
  }

  @Test(expected = IOException.class)
  public void throwsIoExceptionOnExecutionException() throws IOException {
    callable = () -> { throw new ExecutionException("message", null); };
    pactBrokerLoader.get().load("test");
  }

  @Test(expected = IOException.class)
  public void throwsIoExceptionOnRetryException() throws IOException {
    callable = () -> { throw new RetryException(1, mock(Attempt.class)); };
    pactBrokerLoader.get().load("test");
  }

  @Test(expected = RuntimeException.class)
  public void throwsRuntimeExceptionOnAnyNon200Status() throws IOException {
    callable = () -> mockResponse(401);
    pactBrokerLoader.get().load("test");
  }

  @Test
  public void loadsPactsConfiguredFromAPactBrokerAnnotation() throws IOException {
    callable = () -> mockResponse(404);
    pactBrokerLoader = () -> {
      PactBrokerLoader pactBrokerLoader = new PactBrokerLoader(getClass().getAnnotation(PactBroker.class));
      pactBrokerLoader.setHttpResponseCallable(callable);
      return pactBrokerLoader;
    };
    pactBrokerLoader.get().load("test");
  }

  private HttpResponse mockResponse(Integer status) {
    HttpResponse mock = mock(HttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    when(statusLine.getStatusCode()).thenReturn(status);
    when(mock.getStatusLine()).thenReturn(statusLine);
    return mock;
  }

}
