package au.com.dius.pact.consumer;

public class MockServerException extends RuntimeException {
  public MockServerException() {
  }

  public MockServerException(String message) {
    super(message);
  }

  public MockServerException(String message, Throwable cause) {
    super(message, cause);
  }

  public MockServerException(Throwable cause) {
    super(cause);
  }

}
