package au.com.dius.pact.core.support.json;

public abstract class JsonSource {
  public abstract Character nextChar();
  public abstract Character peekNextChar();
  public abstract void advance(int count);

  protected long line = 0;
  protected long character = 0;

  public void advance() {
    advance(1);
  }

  public String documentPointer() {
    return String.format("%d:%d", line + 1, character + 1);
  }
}
