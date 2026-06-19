package au.com.dius.pact.core.support.json;

import java.io.IOException;
import java.io.Reader;

/**
 * JSON source from a Reader
 */
public class ReaderSource extends JsonSource {
  private static final int BUFFER_SIZE = 8192;

  private final Reader reader;
  private final char[] buffer = new char[BUFFER_SIZE];
  private int bufferIndex = 0;
  private int bufferLimit = 0;
  private boolean endOfInput = false;

  public ReaderSource(Reader reader) {
    this.reader = reader;
  }

  public int nextChar() {
    if (!ensureAvailable()) {
      return EOF;
    }

    int next = buffer[bufferIndex++];
    updatePosition(next);
    return next;
  }

  public int peekNextChar() {
    if (!ensureAvailable()) {
      return EOF;
    }
    return buffer[bufferIndex];
  }

  public void advance(int count) {
    for (int i = 0; i < count; i++) {
      if (!ensureAvailable()) {
        return;
      }
      updatePosition(buffer[bufferIndex++]);
    }
  }

  private boolean ensureAvailable() {
    if (bufferIndex < bufferLimit) {
      return true;
    }
    if (endOfInput) {
      return false;
    }

    try {
      bufferLimit = reader.read(buffer, 0, buffer.length);
      bufferIndex = 0;
      if (bufferLimit == -1) {
        endOfInput = true;
        bufferLimit = 0;
        return false;
      }
      return true;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
