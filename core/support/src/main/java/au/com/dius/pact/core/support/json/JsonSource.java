package au.com.dius.pact.core.support.json;

/**
 * Abstract class that represents the source of a JSON document
 */
public abstract class JsonSource {
  public static final int EOF = -1;

  public abstract int nextChar();
  public abstract int peekNextChar();
  public abstract void advance(int count);

  protected long line = 0;
  protected long character = 0;

  public void advance() {
    advance(1);
  }

  protected void updatePosition(int next) {
    if (next == '\n') {
      character = 0;
      line++;
    } else {
      character++;
    }
  }

  public String documentPointer() {
    return String.format("%d:%d", line + 1, character + 1);
  }
}
