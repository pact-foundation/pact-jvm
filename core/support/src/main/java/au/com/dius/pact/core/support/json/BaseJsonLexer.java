package au.com.dius.pact.core.support.json;

import com.github.michaelbull.result.Err;
import com.github.michaelbull.result.Ok;
import com.github.michaelbull.result.Result;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Predicate;

public class BaseJsonLexer {
  protected JsonSource json;

  public BaseJsonLexer(JsonSource json) {
    this.json = json;
  }

  protected void skipWhitespace() {
    Character next = json.peekNextChar();
    while (next != null && Character.isWhitespace(next)) {
      json.advance();
      next = json.peekNextChar();
    }
  }

  protected Result<JsonToken.StringValue, JsonException> scanString() {
    char[] buffer = new char[128];
    int index = 0;
    Character next;
    do {
      next = json.nextChar();
      if (next != null && next == '\\') {
        Character escapeCode = json.nextChar();
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
            Character u1 = json.nextChar();
            if (u1 == null) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), Unicode characters require 4 hex digits", json.documentPointer())));
            } else if (invalidHex(u1)) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), '%c' is not a valid hex code character", json.documentPointer(), u1)));
            }
            Character u2 = json.nextChar();
            if (u2 == null) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), Unicode characters require 4 hex digits", json.documentPointer())));
            } else if (invalidHex(u2)) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), '%c' is not a valid hex code character", json.documentPointer(), u2)));
            }
            Character u3 = json.nextChar();
            if (u3 == null) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), Unicode characters require 4 hex digits", json.documentPointer())));
            } else if (invalidHex(u3)) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), '%c' is not a valid hex code character", json.documentPointer(), u3)));
            }
            Character u4 = json.nextChar();
            if (u4 == null) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), Unicode characters require 4 hex digits", json.documentPointer())));
            } else if (invalidHex(u4)) {
              return new Err(new JsonException(String.format(
                "Invalid JSON (%s), '%c' is not a valid hex code character", json.documentPointer(), u4)));
            }
            int hex = Integer.parseInt(new String(new char[]{u1, u2, u3, u4}), 16);
            if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = (char) hex;
            break;
          }
          default: return new Err(new JsonException(String.format(
            "Invalid JSON (%s), '%c' is not a valid escape code", json.documentPointer(), escapeCode)));
        }
      } else if (next == null) {
        return new Err(new JsonException(String.format("Invalid JSON (%s), End of document scanning for string terminator",
          json.documentPointer())));
      } else if (next != '"') {
        if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = next;
      }
    } while (next != '"');
    return new Ok(new JsonToken.StringValue(ArrayUtils.subarray(buffer, 0, index)));
  }

  private char[] allocate(char[] buffer) {
    return allocate(buffer, 1);
  }

  private char[] allocate(char[] buffer, int size) {
    char[] newBuffer = new char[buffer.length + Math.max(buffer.length, size)];
    System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
    return newBuffer;
  }

  private boolean invalidHex(Character ch) {
    if (Character.isDigit(ch)) {
      return false;
    } else {
      switch (ch) {
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
          return false;
        default:
          return true;
      }
    }
  }

  protected char[] consumeChars(Character first, Predicate<Character> predicate) {
    char[] buffer = new char[16];
    buffer[0] = first;
    int index = 1;
    Character next = json.peekNextChar();
    while (next != null && predicate.test(next)) {
      if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = next;
      json.advance();
      next = json.peekNextChar();
    }
    return ArrayUtils.subarray(buffer, 0, index);
  }

  protected Result<JsonToken, JsonException> scanNumber(Character next) {
    char[] buffer = consumeChars(next, Character::isDigit);
    if (next == '-' && buffer.length == 1) {
      return new Err(new JsonException(String.format(
        "Invalid JSON (%s), found a '%c' that was not followed by any digits", json.documentPointer(), next)));
    }
    Character ch = json.peekNextChar();
    if (ch != null && (ch == '.' || ch == 'e' || ch == 'E')) {
      return scanDecimalNumber(buffer);
    } else {
      return new Ok(new JsonToken.Integer(buffer));
    }
  }

  protected Result<JsonToken, JsonException> scanDecimalNumber(char[] buffer) {
    int index = buffer.length;
    Character next = json.peekNextChar();
    if (next != null && next == '.') {
      char[] digits = consumeChars(json.nextChar(), Character::isDigit);
      buffer = allocate(buffer, digits.length);
      System.arraycopy(digits, 0, buffer, index, digits.length);
      index += digits.length;
      if (!Character.isDigit(buffer[index - 1])) {
        return new Err(new JsonException(String.format("Invalid JSON (%s), '%s' is not a valid number",
          json.documentPointer(), new String(ArrayUtils.subarray(buffer, 0, index)))));
      }
      next = json.peekNextChar();
    }
    if (next != null && (next == 'e' || next == 'E')) {
      if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = json.nextChar();
      next = json.peekNextChar();
      if (next != null && (next == '+' || next == '-')) {
        if (index >= buffer.length) { buffer = allocate(buffer); }; buffer[index++] = json.nextChar();
      }
      char[] digits = consumeChars(json.nextChar(), Character::isDigit);
      buffer = allocate(buffer, digits.length);
      System.arraycopy(digits, 0, buffer, index, digits.length);
      index += digits.length;
      if (!Character.isDigit(buffer[index - 1])) {
        return new Err(new JsonException(String.format("Invalid JSON (%s), '%s' is not a valid number",
          json.documentPointer(), new String(ArrayUtils.subarray(buffer, 0, index)))));
      }
    }
    return new Ok(new JsonToken.Decimal(ArrayUtils.subarray(buffer, 0, index)));
  }
}
