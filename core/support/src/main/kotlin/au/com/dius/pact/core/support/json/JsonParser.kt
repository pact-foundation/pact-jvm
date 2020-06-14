package au.com.dius.pact.core.support.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

class JsonException(message: String) : RuntimeException(message)

sealed class JsonSource {
  abstract fun nextChar(): Char?
  abstract fun peekNextChar(): Char?
  abstract fun advance(count: Int = 1)

  open class StringSource(val json: CharArray, var index: Int = 0) : JsonSource() {
    override fun nextChar(): Char? {
      val c = peekNextChar()
      if (c != null) {
        index += 1
      }
      return c
    }

    override fun peekNextChar(): Char? {
      return if (index >= json.size) {
        null
      } else {
        json[index]
      }
    }

    override fun advance(count: Int) {
      index += count
    }
  }

  open class InputStreamSource(val json: InputStream) : ReaderSource(InputStreamReader(BufferedInputStream(json)))

  open class ReaderSource(private val reader: Reader) : JsonSource() {
    private var buffer: Char? = null

    override fun nextChar(): Char? {
      return if (buffer != null) {
        val c = buffer
        buffer = null
        c
      } else {
        val next = reader.read()
        if (next == -1) {
          null
        } else {
          next.toChar()
        }
      }
    }

    override fun peekNextChar(): Char? {
      if (buffer == null) {
        val next = reader.read()
        buffer = if (next == -1) {
          null
        } else {
          next.toChar()
        }
      }
      return buffer
    }

    override fun advance(count: Int) {
      val chars = if (buffer != null) {
        buffer = null
        count - 1
      } else {
        count
      }
      reader.skip(chars.toLong())
    }
  }
}

sealed class JsonToken(open val chars: CharArray) {
  object Whitespace : JsonToken("".toCharArray())
  class Integer(override val chars: CharArray) : JsonToken(chars)
  class Decimal(override val chars: CharArray) : JsonToken(chars)
  object True : JsonToken("true".toCharArray())
  object False : JsonToken("false".toCharArray())
  object Null : JsonToken("null".toCharArray())
  class StringValue(override val chars: CharArray) : JsonToken(chars)
  object ArrayStart : JsonToken("[".toCharArray())
  object ArrayEnd : JsonToken("]".toCharArray())
  object ObjectStart : JsonToken("{".toCharArray())
  object ObjectEnd : JsonToken("}".toCharArray())
  object Comma : JsonToken(",".toCharArray())
  object Colon : JsonToken(":".toCharArray())

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as JsonToken

    if (!chars.contentEquals(other.chars)) return false

    return true
  }

  override fun hashCode(): Int {
    return chars.contentHashCode()
  }

  override fun toString() = when (this) {
    is Whitespace -> "Whitespace"
    is Integer -> "Integer(${chars.contentToString()})"
    is Decimal -> "Decimal(${chars.contentToString()})"
    is True -> "True"
    is False -> "False"
    is Null -> "Null"
    is StringValue -> "String(${chars.contentToString()})"
    is ArrayStart -> "ArrayStart"
    is ArrayEnd -> "ArrayEnd"
    is ObjectStart -> "ObjectStart"
    is ObjectEnd -> "ObjectEnd"
    is Comma -> "Comma"
    is Colon -> "Colon"
  }
}

class JsonLexer(val json: JsonSource) {
  var line: Long = 0
  var character: Long = 0

  private val validNumberSuffixes = arrayOf('.', 'e', 'E')
  private val validExponent = arrayOf('e', 'E')
  private val validHexDigits = '0'..'9'
  private val validHexCharsLower = 'a'..'f'
  private val validHexCharsUpper = 'A'..'F'

  fun nextToken(): Result<JsonToken?, JsonException> {
    val next = nextChar()
    if (next != null) {
      return when {
        next.isWhitespace() -> {
          skipWhitespace()
          Ok(JsonToken.Whitespace)
        }
        next == '-' || next.isDigit() -> scanNumber(next)
        next == 't' -> scanTrue()
        next == 'f' -> scanFalse()
        next == 'n' -> scanNull()
        next == '"' -> scanString()
        next == '[' -> Ok(JsonToken.ArrayStart)
        next == ']' -> Ok(JsonToken.ArrayEnd)
        next == '{' -> Ok(JsonToken.ObjectStart)
        next == '}' -> Ok(JsonToken.ObjectEnd)
        next == ',' -> Ok(JsonToken.Comma)
        next == ':' -> Ok(JsonToken.Colon)
        else -> unexpectedCharacter(next)
      }
    }
    return Ok(null)
  }

  private fun skipWhitespace() {
    var next = json.peekNextChar()
    while (next != null && next.isWhitespace()) {
      json.advance()
      next = json.peekNextChar()
    }
  }

  private fun scanNumber(next: Char): Result<JsonToken, JsonException> {
    val chars = mutableListOf(next)
    chars.addAll(consumeChars(Char::isDigit).asIterable())
    if (next == '-' && chars.size == 1) {
      return Err(JsonException(
        "Invalid JSON (${documentPointer()}), found a '$next' that was not followed by any digits"))
    }
    return if (validNumberSuffixes.contains(json.peekNextChar())) {
      scanDecimalNumber(chars)
    } else {
      Ok(JsonToken.Integer(chars.toCharArray()))
    }
  }

  private fun scanDecimalNumber(chars: MutableList<Char>): Result<JsonToken.Decimal, JsonException> {
    var next = json.peekNextChar()
    if (next == '.') {
      chars.add(json.nextChar()!!)
      chars.addAll(consumeChars(Char::isDigit).asIterable())
      if (!chars.last().isDigit()) {
        return Err(JsonException("Invalid JSON (${documentPointer()}), '$chars' is not a valid number"))
      }
      next = json.peekNextChar()
    }
    if (validExponent.contains(next)) {
      chars.add(json.nextChar()!!)
      next = json.peekNextChar()
      if (next == '+' || next == '-') {
        chars.add(json.nextChar()!!)
      }
      chars.addAll(consumeChars(Char::isDigit).asIterable())
      if (!chars.last().isDigit()) {
        return Err(JsonException("Invalid JSON (${documentPointer()}), '$chars' is not a valid number"))
      }
    }
    return Ok(JsonToken.Decimal(chars.toCharArray()))
  }

  private fun scanString(): Result<JsonToken.StringValue, JsonException> {
    val chars = mutableListOf<Char>()
    do {
      val next = json.nextChar()
      if (next == '\\') {
        when (val escapeCode = json.nextChar()) {
          '"' -> chars.add('"')
          '\\' -> chars.add('\\')
          '/' -> chars.add('/')
          'b' -> chars.add('\b')
          'f' -> chars.add('\u000c')
          'n' -> chars.add('\n')
          'r' -> chars.add('\r')
          't' -> chars.add('\t')
          'u' -> {
            val u1 = json.nextChar()
            if (!validHex(u1)) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), '$u1' is not a valid hex code character"))
            } else if (u1 == null) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val u2 = json.nextChar()
            if (!validHex(u2)) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), '$u2' is not a valid hex code character"))
            } else if (u2 == null) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val u3 = json.nextChar()
            if (!validHex(u3)) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), '$u3' is not a valid hex code character"))
            } else if (u3 == null) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val u4 = json.nextChar()
            if (!validHex(u4)) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), '$u4' is not a valid hex code character"))
            } else if (u4 == null) {
              return Err(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val hex = String(charArrayOf(u1, u2, u3, u4)).toInt(radix = 16)
            chars.add(hex.toChar())
          }
          else -> return Err(
            JsonException("Invalid JSON (${documentPointer()}), '$escapeCode' is not a valid escape code"))
        }
      } else if (next == null) {
        return Err(
          JsonException("Invalid JSON (${documentPointer()}), End of document scanning for string terminator"))
      } else if (next != '"') {
        chars.add(next)
      }
    } while (next != '"')
    return Ok(JsonToken.StringValue(chars.toCharArray()))
  }

  private fun validHex(char: Char?) = validHexDigits.contains(char) || validHexCharsLower.contains(char) ||
    validHexCharsUpper.contains(char)

  private fun unexpectedCharacter(next: Char?) = if (next == null)
    Err(JsonException("Invalid JSON (${documentPointer()}), unexpected end of the JSON document"))
  else
    Err(JsonException("Invalid JSON (${documentPointer()}), found unexpected character '$next'"))

  fun documentPointer() = "${line + 1}:${character + 1}"

  private fun scanNull(): Result<JsonToken?, JsonException> {
    var next = json.nextChar()
    if (next == null || next != 'u') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'l') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'l') return unexpectedCharacter(next)
    return Ok(JsonToken.Null)
  }

  private fun scanFalse(): Result<JsonToken?, JsonException> {
    var next = json.nextChar()
    if (next == null || next != 'a') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'l') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 's') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'e') return unexpectedCharacter(next)
    return Ok(JsonToken.False)
  }

  private fun scanTrue(): Result<JsonToken?, JsonException> {
    var next = json.nextChar()
    if (next == null || next != 'r') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'u') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'e') return unexpectedCharacter(next)
    return Ok(JsonToken.True)
  }

  private fun consumeChars(predicate: (Char) -> Boolean): CharArray {
    val array = ArrayList<Char>(1000)
    var next = json.peekNextChar()
    while (next != null && predicate(next)) {
      array.add(next)
      json.advance()
      next = json.peekNextChar()
    }
    return array.toCharArray()
  }

  private fun nextChar(): Char? {
    val next = json.nextChar()
    if (next != null) {
      if (next == '\n') {
        character = 0
        line += 1
      } else {
        character += 1
      }
    }
    return next
  }
}

object JsonParser {

  @Throws(JsonException::class)
  fun parseString(json: String): JsonValue {
    if (json.isNotEmpty()) {
      return parse(JsonSource.StringSource(json.toCharArray()))
    } else {
      throw JsonException("Json document is empty")
    }
  }

  @Throws(JsonException::class)
  fun parseStream(json: InputStream): JsonValue {
    return parse(JsonSource.InputStreamSource(json))
  }

  @Throws(JsonException::class)
  fun parseReader(reader: Reader): JsonValue {
    return parse(JsonSource.ReaderSource(reader))
  }

  private fun parse(json: JsonSource): JsonValue {
    val lexer = JsonLexer(json)
    var token = nextTokenOrThrow(lexer)
    val jsonValue = when (token) {
      is JsonToken.Integer -> JsonValue.Integer(token.chars)
      is JsonToken.Decimal -> JsonValue.Decimal(token.chars)
      is JsonToken.StringValue -> JsonValue.StringValue(token.chars)
      is JsonToken.True -> JsonValue.True
      is JsonToken.False -> JsonValue.False
      is JsonToken.Null -> JsonValue.Null
      is JsonToken.ArrayStart -> parseArray(lexer)
      is JsonToken.ObjectStart -> parseObject(lexer)
      else -> if (token != null) {
        throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters '${String(token.chars)}'")
      } else {
        throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found only whitespace characters")
      }
    }

    token = nextTokenOrThrow(lexer)
    if (token != null) {
      throw JsonException(
        "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters '${String(token.chars)}'")
    }

    return jsonValue
  }

  private fun parseObject(lexer: JsonLexer): JsonValue.Object {
    val map = mutableMapOf<String, JsonValue>()
    var token: JsonToken?

    do {
      token = nextTokenOrThrow(lexer)
      if (token !is JsonToken.ObjectEnd) {
        val key = when (token) {
          null -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing object")
          is JsonToken.StringValue -> String(token.chars)
          else -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - expected a string but found unexpected characters " +
              "'${token.chars}'")
        }

        token = nextTokenOrThrow(lexer)
        if (token == null) {
          throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing object")
        } else if (token !is JsonToken.Colon) {
          throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - expected a colon but found unexpected characters " +
              "'${String(token.chars)}'")
        }

        token = nextTokenOrThrow(lexer)
        when (token) {
          null -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing object")
          is JsonToken.Integer -> map[key] = JsonValue.Integer(token.chars)
          is JsonToken.Decimal -> map[key] = JsonValue.Decimal(token.chars)
          is JsonToken.StringValue -> map[key] = JsonValue.StringValue(token.chars)
          is JsonToken.True -> map[key] = JsonValue.True
          is JsonToken.False -> map[key] = JsonValue.False
          is JsonToken.Null -> map[key] = JsonValue.Null
          is JsonToken.ArrayStart -> map[key] = parseArray(lexer)
          is JsonToken.ObjectStart -> map[key] = parseObject(lexer)
          else -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters '${token.chars}'")
        }

        token = nextTokenOrThrow(lexer)
        if (token !is JsonToken.Comma && token != JsonToken.ObjectEnd && token != null) {
          throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters ${token.chars}")
        }
      }
    } while (token != null && token != JsonToken.ObjectEnd)

    if (token == null) {
      throw JsonException(
        "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing object")
    }

    return JsonValue.Object(map)
  }

  private fun parseArray(lexer: JsonLexer): JsonValue.Array {
    val array = mutableListOf<JsonValue>()
    var token: JsonToken?

    do {
      token = nextTokenOrThrow(lexer)
      if (token !is JsonToken.ArrayEnd) {
        when (token) {
          null -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing array")
          is JsonToken.Integer -> array.add(JsonValue.Integer(token.chars))
          is JsonToken.Decimal -> array.add(JsonValue.Decimal(token.chars))
          is JsonToken.StringValue -> array.add(JsonValue.StringValue(token.chars))
          is JsonToken.True -> array.add(JsonValue.True)
          is JsonToken.False -> array.add(JsonValue.False)
          is JsonToken.Null -> array.add(JsonValue.Null)
          is JsonToken.ArrayStart -> array.add(parseArray(lexer))
          is JsonToken.ObjectStart -> array.add(parseObject(lexer))
          else -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters ${String(token.chars)}")
        }

        token = nextTokenOrThrow(lexer)
        if (token !is JsonToken.Comma && token != JsonToken.ArrayEnd && token != null) {
          throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters ${String(token.chars)}")
        }
      }
    } while (token != null && token != JsonToken.ArrayEnd)

    if (token == null) {
      throw JsonException(
        "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing array")
    }

    return JsonValue.Array(array)
  }

  private fun nextTokenOrThrow(lexer: JsonLexer): JsonToken? {
    var token: JsonToken?
    do {
      val next = lexer.nextToken()
      token = when (next) {
        is Err -> throw next.error
        is Ok -> next.value
      }
    } while (token is JsonToken.Whitespace)
    return token
  }
}
