package au.com.dius.pact.provider.junitsupport.loader;

public class NoPactsFoundException extends RuntimeException {
  public NoPactsFoundException() {
  }

  public NoPactsFoundException(String message) {
    super(message);
  }

  public NoPactsFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NoPactsFoundException(Throwable cause) {
    super(cause);
  }
}
