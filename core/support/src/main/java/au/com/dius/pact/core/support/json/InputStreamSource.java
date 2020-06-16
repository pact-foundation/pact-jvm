package au.com.dius.pact.core.support.json;

import java.io.InputStream;
import java.io.InputStreamReader;

public class InputStreamSource extends ReaderSource {
  public InputStreamSource(InputStream source) {
    super(new InputStreamReader(source));
  }
}
