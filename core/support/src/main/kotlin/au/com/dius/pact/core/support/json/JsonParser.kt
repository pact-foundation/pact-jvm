package au.com.dius.pact.core.support.json

import arrow.core.Either
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader

class JsonException(message: String) : RuntimeException(message)

sealed class JsonSource {
  abstract fun nextChar(): Char?
  abstract fun peekNextChar(): Char?
  abstract fun advance(count: Int = 1)

  data class StringSource(val json: String, var index: Int = 0) : JsonSource() {
    override fun nextChar(): Char? {
      val c = peekNextChar()
      if (c != null) {
        index += 1
      }
      return c
    }

    override fun peekNextChar(): Char? {
      return if (index >= json.length) {
        null
      } else {
        json[index]
      }
    }

    override fun advance(count: Int) {
      index += count
    }
  }

  data class InputStreamSource(val json: InputStream) : JsonSource() {
    private val reader = InputStreamReader(BufferedInputStream(json))
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

sealed class JsonToken(open val chars: String) {
  data class Whitespace(override val chars: String) : JsonToken(chars)
  data class Integer(override val chars: String) : JsonToken(chars) {
    fun toInteger() = chars.toBigInteger()
  }
  data class Decimal(override val chars: String) : JsonToken(chars) {
    fun toDecimal() = chars.toBigDecimal()
  }
  object True : JsonToken("true")
  object False : JsonToken("false")
  object Null : JsonToken("null")
  data class StringValue(override val chars: String) : JsonToken(chars)
  object ArrayStart : JsonToken("[")
  object ArrayEnd : JsonToken("]")
  object ObjectStart : JsonToken("{")
  object ObjectEnd : JsonToken("}")
  object Comma : JsonToken(",")
  object Colon : JsonToken(":")
}

class JsonLexer(val json: JsonSource) {
  var line: Long = 0
  var character: Long = 0

  private val validNumberSuffixes = arrayOf('.', 'e', 'E')
  private val validExponent = arrayOf('e', 'E')
  private val validHexDigits = '0'..'9'
  private val validHexCharsLower = 'a'..'f'
  private val validHexCharsUpper = 'A'..'F'

  fun nextToken(): Either<JsonException, JsonToken?> {
    val next = nextChar()
    if (next != null) {
      return when {
        next.isWhitespace() -> {
          val chars = mutableListOf(next)
          chars.addAll(consumeChars(Char::isWhitespace))
          Either.right(JsonToken.Whitespace(String(chars.toCharArray())))
        }
        next == '-' || next.isDigit() -> scanNumber(next)
        next == 't' -> scanTrue()
        next == 'f' -> scanFalse()
        next == 'n' -> scanNull()
        next == '"' -> scanString()
        next == '[' -> Either.right(JsonToken.ArrayStart)
        next == ']' -> Either.right(JsonToken.ArrayEnd)
        next == '{' -> Either.right(JsonToken.ObjectStart)
        next == '}' -> Either.right(JsonToken.ObjectEnd)
        next == ',' -> Either.right(JsonToken.Comma)
        next == ':' -> Either.right(JsonToken.Colon)
        else -> unexpectedCharacter(next)
      }
    }
    return Either.right(null)
  }

  private fun scanNumber(next: Char): Either<JsonException, JsonToken> {
    val chars = mutableListOf(next)
    chars.addAll(consumeChars(Char::isDigit))
    if (next == '-' && chars.size == 1) {
      return Either.left(JsonException(
        "Invalid JSON (${documentPointer()}), found a '$next' that was not followed by any digits"))
    }
    return if (validNumberSuffixes.contains(json.peekNextChar())) {
      scanDecimalNumber(chars)
    } else {
      Either.right(JsonToken.Integer(String(chars.toCharArray())))
    }
  }

  private fun scanDecimalNumber(chars: MutableList<Char>): Either<JsonException, JsonToken.Decimal> {
    var next = json.peekNextChar()
    if (next == '.') {
      chars.add(json.nextChar()!!)
      chars.addAll(consumeChars(Char::isDigit))
      if (!chars.last().isDigit()) {
        return Either.left(JsonException("Invalid JSON (${documentPointer()}), '$chars' is not a valid number"))
      }
      next = json.peekNextChar()
    }
    if (validExponent.contains(next)) {
      chars.add(json.nextChar()!!)
      next = json.peekNextChar()
      if (next == '+' || next == '-') {
        chars.add(json.nextChar()!!)
      }
      chars.addAll(consumeChars(Char::isDigit))
      if (!chars.last().isDigit()) {
        return Either.left(JsonException("Invalid JSON (${documentPointer()}), '$chars' is not a valid number"))
      }
    }
    return Either.right(JsonToken.Decimal(String(chars.toCharArray())))
  }

  private fun scanString(): Either<JsonException, JsonToken.StringValue> {
    val chars = mutableListOf<Char>()
    do {
      var next = json.nextChar()
      if (next == '\\') {
        val escapeCode = json.nextChar()
        when (escapeCode) {
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
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), '$u1' is not a valid hex code character"))
            } else if (u1 == null) {
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val u2 = json.nextChar()
            if (!validHex(u2)) {
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), '$u2' is not a valid hex code character"))
            } else if (u2 == null) {
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val u3 = json.nextChar()
            if (!validHex(u3)) {
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), '$u3' is not a valid hex code character"))
            } else if (u3 == null) {
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val u4 = json.nextChar()
            if (!validHex(u4)) {
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), '$u4' is not a valid hex code character"))
            } else if (u4 == null) {
              return Either.left(
                JsonException("Invalid JSON (${documentPointer()}), Unicode characters require 4 hex digits"))
            }
            val hex = String(charArrayOf(u1, u2, u3, u4)).toInt(radix = 16)
            chars.add(hex.toChar())
          }
          else -> return Either.left(
            JsonException("Invalid JSON (${documentPointer()}), '$escapeCode' is not a valid escape code"))
        }
      } else if (next == null) {
        return Either.left(
          JsonException("Invalid JSON (${documentPointer()}), End of document scanning for string terminator"))
      } else if (next != '"') {
        chars.add(next)
      }
    } while (next != '"')
    return Either.right(JsonToken.StringValue(String(chars.toCharArray())))
  }

  private fun validHex(char: Char?) = validHexDigits.contains(char) || validHexCharsLower.contains(char) ||
    validHexCharsUpper.contains(char)

  private fun unexpectedCharacter(next: Char?) = if (next == null)
    Either.left(JsonException("Invalid JSON (${documentPointer()}), unexpected end of the JSON document"))
  else
    Either.left(JsonException("Invalid JSON (${documentPointer()}), found unexpected character '$next'"))

  fun documentPointer() = "${line + 1}:${character + 1}"

  private fun scanNull(): Either<JsonException, JsonToken?> {
    var next = json.nextChar()
    if (next == null || next != 'u') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'l') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'l') return unexpectedCharacter(next)
    return Either.right(JsonToken.Null)
  }

  private fun scanFalse(): Either<JsonException, JsonToken?> {
    var next = json.nextChar()
    if (next == null || next != 'a') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'l') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 's') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'e') return unexpectedCharacter(next)
    return Either.right(JsonToken.False)
  }

  private fun scanTrue(): Either<JsonException, JsonToken?> {
    var next = json.nextChar()
    if (next == null || next != 'r') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'u') return unexpectedCharacter(next)
    next = json.nextChar()
    if (next == null || next != 'e') return unexpectedCharacter(next)
    return Either.right(JsonToken.True)
  }

  private fun consumeChars(predicate: (Char) -> Boolean): List<Char> {
    val list = mutableListOf<Char>()
    var next = json.peekNextChar()
    while (next != null && predicate(next)) {
      list.add(next)
      json.advance()
      next = json.peekNextChar()
    }
    return list
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

  fun parseString(json: String): JsonValue {
    if (json.isNotEmpty()) {
      return parse(JsonSource.StringSource(json))
    } else {
      throw JsonException("Json document is empty")
    }
  }

  fun parseStream(json: InputStream): JsonValue {
    return parse(JsonSource.InputStreamSource(json))
  }

  private fun parse(json: JsonSource): JsonValue {
    val lexer = JsonLexer(json)
    var token = nextTokenOrThrow(lexer)
    val jsonValue = when (token) {
      is JsonToken.Integer -> JsonValue.Integer(token.toInteger())
      is JsonToken.Decimal -> JsonValue.Decimal(token.toDecimal())
      is JsonToken.StringValue -> JsonValue.StringValue(token.chars)
      is JsonToken.True -> JsonValue.True
      is JsonToken.False -> JsonValue.False
      is JsonToken.Null -> JsonValue.Null
      is JsonToken.ArrayStart -> parseArray(lexer)
      is JsonToken.ObjectStart -> parseObject(lexer)
      else -> if (token != null) {
        throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters '${token.chars}'")
      } else {
        throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found only whitespace characters")
      }
    }

    token = nextTokenOrThrow(lexer)
    if (token != null) {
      throw JsonException(
        "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters '${token.chars}'")
    }

    return jsonValue
  }

  private fun parseObject(lexer: JsonLexer): JsonValue.Object {
    val map = mutableMapOf<String, JsonValue>()
    var token: JsonToken?

    do {
      token = nextTokenOrThrow(lexer)
      val key = when (token) {
        null -> throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing object")
        is JsonToken.StringValue -> token.chars
        else -> throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - expected a string but found unexpected characters '${token.chars}'")
      }

      token = nextTokenOrThrow(lexer)
      if (token == null) {
        throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing object")
      } else if (token !is JsonToken.Colon) {
        throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - expected a colon but found unexpected characters '${token.chars}'")
      }

      token = nextTokenOrThrow(lexer)
      when (token) {
        null -> throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing object")
        is JsonToken.Integer -> map[key] = JsonValue.Integer(token.toInteger())
        is JsonToken.Decimal -> map[key] = JsonValue.Decimal(token.toDecimal())
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
      when (token) {
        null -> throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing array")
        is JsonToken.Integer -> array.add(JsonValue.Integer(token.toInteger()))
        is JsonToken.Decimal -> array.add(JsonValue.Decimal(token.toDecimal()))
        is JsonToken.StringValue -> array.add(JsonValue.StringValue(token.chars))
        is JsonToken.True -> array.add(JsonValue.True)
        is JsonToken.False -> array.add(JsonValue.False)
        is JsonToken.Null -> array.add(JsonValue.Null)
        is JsonToken.ArrayStart -> array.add(parseArray(lexer))
        is JsonToken.ObjectStart -> array.add(parseObject(lexer))
        else -> throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters ${token.chars}")
      }

      token = nextTokenOrThrow(lexer)
      if (token !is JsonToken.Comma && token != JsonToken.ArrayEnd && token != null) {
        throw JsonException(
          "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters ${token.chars}")
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
        is Either.Left -> throw next.a
        is Either.Right -> next.b
      }
    } while (token is JsonToken.Whitespace)
    return token
  }
}
