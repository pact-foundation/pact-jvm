package au.com.dius.pact.core.support.json;

import java.io.IOException;
import java.io.Reader;

/**
 * JSON source from a Reader
 */
public class ReaderSource extends JsonSource {
  private Reader reader;
  private int buffer = EOF;
  private boolean hasBuffer = false;

  public ReaderSource(Reader reader) {
    this.reader = reader;
  }

  public int nextChar() {
    int next;
    if (hasBuffer) {
      next = buffer;
      hasBuffer = false;
      buffer = EOF;
    } else {
      next = readNextChar();
    }

    if (next != EOF) {
      updatePosition(next);
    }

    return next;
  }

  public int peekNextChar() {
    if (!hasBuffer) {
      buffer = readNextChar();
      hasBuffer = true;
    }
    return buffer;
  }

  public void advance(int count) {
    for (int i = 0; i < count; i++) {
      if (nextChar() == EOF) {
        return;
      }
    }
  }

  private int readNextChar() {
    try {
      return reader.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
