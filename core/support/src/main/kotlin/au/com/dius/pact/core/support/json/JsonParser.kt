package au.com.dius.pact.core.support.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.InputStream
import java.io.Reader
import java.util.ArrayDeque

class JsonException(message: String) : RuntimeException(message)

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

class JsonLexer(json: JsonSource) : BaseJsonLexer(json) {
  fun nextToken(): Result<JsonToken?, JsonException> {
    val next = json.nextChar()
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

  private fun unexpectedCharacter(next: Char?) = if (next == null)
    Err(JsonException("Invalid JSON (${documentPointer()}), unexpected end of the JSON document"))
  else
    Err(JsonException("Invalid JSON (${documentPointer()}), found unexpected character '$next'"))

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

  fun documentPointer() = json.documentPointer()
}

object JsonParser {

  @Throws(JsonException::class)
  fun parseString(json: String): JsonValue {
    if (json.isNotEmpty()) {
      return parse(StringSource(json.toCharArray()))
    } else {
      throw JsonException("Json document is empty")
    }
  }

  @Throws(JsonException::class)
  fun parseStream(json: InputStream): JsonValue {
    return parse(InputStreamSource(json))
  }

  @Throws(JsonException::class)
  fun parseReader(reader: Reader): JsonValue {
    return parse(ReaderSource(reader))
  }

  private fun parse(json: JsonSource): JsonValue {
    val lexer = JsonLexer(json)
    var token = nextTokenOrThrow(lexer)
    val jsonValue = when (token) {
      is JsonToken.Integer -> JsonValue.Integer(token)
      is JsonToken.Decimal -> JsonValue.Decimal(token)
      is JsonToken.StringValue -> JsonValue.StringValue(token)
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
          is JsonToken.Integer -> map[key] = JsonValue.Integer(token)
          is JsonToken.Decimal -> map[key] = JsonValue.Decimal(token)
          is JsonToken.StringValue -> map[key] = JsonValue.StringValue(token)
          is JsonToken.True -> map[key] = JsonValue.True
          is JsonToken.False -> map[key] = JsonValue.False
          is JsonToken.Null -> map[key] = JsonValue.Null
          is JsonToken.ArrayStart -> map[key] = parseArray(lexer)
          is JsonToken.ObjectStart -> map[key] = parseObject(lexer)
          else -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found unexpected characters '${String(token.chars)}'")
        }

        token = nextTokenOrThrow(lexer)
        if (token !is JsonToken.Comma && token != JsonToken.ObjectEnd && token != null) {
          throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - Expecting ',' or '}' while parsing object, found '${String(token.chars)}'")
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
    val array = ArrayDeque<JsonValue>()
    var token: JsonToken?

    do {
      token = nextTokenOrThrow(lexer)
      if (token !is JsonToken.ArrayEnd) {
        when (token) {
          null -> throw JsonException(
            "Invalid Json document (${lexer.documentPointer()}) - found end of document while parsing array")
          is JsonToken.Integer -> array.add(JsonValue.Integer(token))
          is JsonToken.Decimal -> array.add(JsonValue.Decimal(token))
          is JsonToken.StringValue -> array.add(JsonValue.StringValue(token))
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

    return JsonValue.Array(array.toMutableList())
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
