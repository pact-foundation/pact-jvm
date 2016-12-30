package au.com.dius.pact.provider.junit;

public class MissingStateChangeMethod extends Exception {

  public MissingStateChangeMethod() {
  }

  public MissingStateChangeMethod(String message) {
    super(message);
  }

  public MissingStateChangeMethod(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingStateChangeMethod(Throwable cause) {
    super(cause);
  }

}
