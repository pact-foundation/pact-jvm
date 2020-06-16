package au.com.dius.pact.core.support.json;

import java.io.IOException;
import java.io.Reader;

public class ReaderSource extends JsonSource {
  private Reader reader;
  private Character buffer = null;

  public ReaderSource(Reader reader) {
    this.reader = reader;
  }

  public Character nextChar() {
    if (buffer != null) {
      Character c = buffer;
      buffer = null;
      return c;
    } else {
      int next = 0;
      try {
        next = reader.read();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (next == -1) {
        return null;
      } else {
        if (next == '\n') {
          character = 0;
          line++;
        } else {
          character++;
        }
        return (char) next;
      }
    }
  }

  public Character peekNextChar() {
    if (buffer == null) {
      int next = 0;
      try {
        next = reader.read();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (next == -1) {
        buffer = null;
      } else {
        buffer = (char) next;
      }
    }
    return buffer;
  }

  public void advance(int count) {
    int charsToSkip = count;
    if (buffer != null) {
      buffer = null;
      charsToSkip = count - 1;
    }
    try {
      for (int i = 0; i < charsToSkip; i++) {
        int next = reader.read();
        if (next == '\n') {
          character = 0;
          line++;
        } else {
          character++;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
