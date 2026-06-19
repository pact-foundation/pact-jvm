package au.com.dius.pact.core.support.json;

import au.com.dius.pact.core.support.Result;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * Base Lexer for tokenising a JSON document
 */
public class BaseJsonLexer {
  protected JsonSource json;

  private static final class ScannedString {
    private final char[] buffer;
    private final int length;

    private ScannedString(char[] buffer, int length) {
      this.buffer = buffer;
      this.length = length;
    }

    private char[] toCharArray() {
      return length == buffer.length ? buffer : Arrays.copyOf(buffer, length);
    }

    private String toStringValue() {
      return new String(buffer, 0, length);
    }
  }

  public BaseJsonLexer(JsonSource json) {
    this.json = json;
  }

  protected void skipWhitespace() {
    int next = json.peekNextChar();
    while (next != JsonSource.EOF && Character.isWhitespace(next)) {
      json.advance();
      next = json.peekNextChar();
    }
  }

  protected Result<JsonToken.StringValue, JsonException> scanString() {
    try {
      return new Result.Ok(new JsonToken.StringValue(scanStringValue().toCharArray()));
    } catch (JsonException e) {
      return new Result.Err(e);
    }
  }

  protected Result<String, JsonException> scanKeyString() {
    try {
      return new Result.Ok(scanStringValue().toStringValue());
    } catch (JsonException e) {
      return new Result.Err(e);
    }
  }

  private ScannedString scanStringValue() {
    char[] buffer = new char[128];
    int index = 0;
    int next;
    do {
      next = json.nextChar();
      if (next == '\\') {
        int escapeCode = json.nextChar();
        if (escapeCode == JsonSource.EOF) {
          throw new JsonException(String.format(
            "Invalid JSON (%s), End of document scanning for string terminator", json.documentPointer()));
        }
        switch (escapeCode) {
          case '"': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '"'; break;
          case '\\': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '\\'; break;
          case '/': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '/'; break;
          case 'b': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '\b'; break;
          case 'f': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '\u000c'; break;
          case 'n': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '\n'; break;
          case 'r': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '\r'; break;
          case 't': if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = '\t'; break;
          case 'u': {
            int hex = 0;
            for (int i = 0; i < 4; i++) {
              int codePoint = json.nextChar();
              if (codePoint == JsonSource.EOF) {
                throw new JsonException(String.format(
                  "Invalid JSON (%s), Unicode characters require 4 hex digits", json.documentPointer()));
              }

              int digit = hexValue(codePoint);
              if (digit == -1) {
                throw new JsonException(String.format(
                  "Invalid JSON (%s), '%c' is not a valid hex code character", json.documentPointer(), (char) codePoint));
              }

              hex = (hex << 4) | digit;
            }
            if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = (char) hex;
            break;
          }
          default: throw new JsonException(String.format(
            "Invalid JSON (%s), '%c' is not a valid escape code", json.documentPointer(), (char) escapeCode));
        }
      } else if (next == JsonSource.EOF) {
        throw new JsonException(String.format("Invalid JSON (%s), End of document scanning for string terminator",
          json.documentPointer()));
      } else if (next != '"') {
        if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = (char) next;
      }
    } while (next != '"');
    return new ScannedString(buffer, index);
  }

  private char[] allocate(char[] buffer) {
    return allocate(buffer, 1);
  }

  private char[] allocate(char[] buffer, int size) {
    char[] newBuffer = new char[buffer.length + Math.max(buffer.length, size)];
    System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
    return newBuffer;
  }

  private int hexValue(int ch) {
    if (ch >= '0' && ch <= '9') {
      return ch - '0';
    }
    if (ch >= 'a' && ch <= 'f') {
      return ch - 'a' + 10;
    }
    if (ch >= 'A' && ch <= 'F') {
      return ch - 'A' + 10;
    }
    return -1;
  }

  protected char[] consumeChars(int first, IntPredicate predicate) {
    char[] buffer = new char[16];
    buffer[0] = (char) first;
    int index = 1;
    int next = json.peekNextChar();
    while (next != JsonSource.EOF && predicate.test(next)) {
      if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = (char) next;
      json.advance();
      next = json.peekNextChar();
    }
    return Arrays.copyOf(buffer, index);
  }

  protected Result<JsonToken, JsonException> scanNumber(int next) {
    char[] buffer = consumeChars(next, Character::isDigit);
    if (next == '-' && buffer.length == 1) {
      return new Result.Err(new JsonException(String.format(
        "Invalid JSON (%s), found a '%c' that was not followed by any digits", json.documentPointer(), (char) next)));
    }
    int ch = json.peekNextChar();
    if (ch != JsonSource.EOF && (ch == '.' || ch == 'e' || ch == 'E')) {
      return scanDecimalNumber(buffer);
    } else {
      return new Result.Ok(new JsonToken.Integer(buffer));
    }
  }

  protected Result<JsonToken, JsonException> scanDecimalNumber(char[] buffer) {
    int index = buffer.length;
    int next = json.peekNextChar();
    if (next != JsonSource.EOF && next == '.') {
      char[] digits = consumeChars(json.nextChar(), Character::isDigit);
      buffer = allocate(buffer, digits.length);
      System.arraycopy(digits, 0, buffer, index, digits.length);
      index += digits.length;
      if (!Character.isDigit(buffer[index - 1])) {
        return new Result.Err(new JsonException(String.format("Invalid JSON (%s), '%s' is not a valid number",
          json.documentPointer(), new String(Arrays.copyOf(buffer, index)))));
      }
      next = json.peekNextChar();
    }
    if (next != JsonSource.EOF && (next == 'e' || next == 'E')) {
      if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = (char) json.nextChar();
      next = json.peekNextChar();
      if (next != JsonSource.EOF && (next == '+' || next == '-')) {
        if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = (char) json.nextChar();
      }
      char[] digits = consumeChars(json.nextChar(), Character::isDigit);
      buffer = allocate(buffer, digits.length);
      System.arraycopy(digits, 0, buffer, index, digits.length);
      index += digits.length;
      if (!Character.isDigit(buffer[index - 1])) {
        return new Result.Err(new JsonException(String.format("Invalid JSON (%s), '%s' is not a valid number",
          json.documentPointer(), new String(Arrays.copyOf(buffer, index)))));
      }
    }
    return new Result.Ok(new JsonToken.Decimal(Arrays.copyOf(buffer, index)));
  }
}
