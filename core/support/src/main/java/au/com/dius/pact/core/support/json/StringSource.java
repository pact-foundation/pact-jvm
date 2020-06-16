package au.com.dius.pact.core.support.json;

public class StringSource extends JsonSource {
  private char[] json;
  private int index = 0;

  public StringSource(char[] json) {
    this.json = json;
  }

  public Character nextChar() {
    Character c = peekNextChar();
    if (c != null) {
      if (c == '\n') {
        character = 0;
        line++;
      } else {
        character++;
      }
      index++;
    }
    return c;
  }

  public Character peekNextChar() {
    if (index >= json.length) {
      return null;
    } else {
      return json[index];
    }
  }

  public void advance(int count) {
    for (int i = 0; i < count; i++) {
      char next = json[index++];
      if (next == '\n') {
        character = 0;
        line++;
      } else {
        character++;
      }
    }
  }
}
