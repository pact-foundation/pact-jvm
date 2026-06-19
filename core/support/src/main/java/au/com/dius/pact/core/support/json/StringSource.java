package au.com.dius.pact.core.support.json;

/**
 * JSON source from a String
 */
public class StringSource extends JsonSource {
  private char[] json;
  private int index = 0;

  public StringSource(char[] json) {
    this.json = json;
  }

  public int nextChar() {
    int c = peekNextChar();
    if (c != EOF) {
      updatePosition(c);
      index++;
    }
    return c;
  }

  public int peekNextChar() {
    if (index >= json.length) {
      return EOF;
    } else {
      return json[index];
    }
  }

  public void advance(int count) {
    for (int i = 0; i < count; i++) {
      updatePosition(json[index++]);
    }
  }
}
