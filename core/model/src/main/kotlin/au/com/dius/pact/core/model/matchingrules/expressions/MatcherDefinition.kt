package au.com.dius.pact.core.model.matchingrules.expressions

import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EachKeyMatcher
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NotEmptyMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.SemverMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Either
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.parsers.StringLexer

class MatcherDefinitionLexer(expression: String): StringLexer(expression) {
  fun matchDecimal() = matchRegex(DECIMAL_LITERAL).isNotEmpty()

  fun matchInteger() = matchRegex(INTEGER_LITERAL).isNotEmpty()

  fun matchWholeNumber() = matchRegex(NUMBER_LITERAL).isNotEmpty()

  fun matchBoolean() = matchRegex(BOOLEAN_LITERAL).isNotEmpty()

  fun highlightPosition(): String {
    return if (index > 0) "^".padStart(index + 1)
    else "^"
  }

  companion object {
    val INTEGER_LITERAL = Regex("^-?\\d+")
    val NUMBER_LITERAL = Regex("^\\d+")
    val DECIMAL_LITERAL = Regex("^-?\\d+\\.\\d+")
    val BOOLEAN_LITERAL = Regex("^(true|false)")
  }
}

data class MatchingRuleResult(
  val value: String?,
  val type: ValueType,
  val rule: MatchingRule?,
  val generator: Generator? = null,
  val reference: MatchingReference? = null
)

@Suppress("MaxLineLength")
class MatcherDefinitionParser(private val lexer: MatcherDefinitionLexer) {
  /**
   * Parse a matcher expression into a MatchingRuleDefinition containing the example value, matching rules and any generator.
   * The following are examples of matching rule definitions:
   * * `matching(type,'Name')` - type matcher
   * * `matching(number,100)` - number matcher
   * * `matching(datetime, 'yyyy-MM-dd','2000-01-01')` - datetime matcher with format string
   **/
  //  matchingDefinition returns [ MatchingRuleDefinition value ] :
  //      matchingDefinitionExp { $value = $matchingDefinitionExp.value; } ( COMMA e=matchingDefinitionExp {  if ($value != null) { $value = $value.merge($e.value); } } )* EOF
  //      ;
  @Suppress("ReturnCount")
  fun matchingDefinition(): Result<MatchingRuleDefinition?, String> {
    val definition = when (val result = matchingDefinitionExp()) {
      is Result.Ok -> {
        var definitions = result.value
        lexer.skipWhitespace()
        if (lexer.peekNextChar() == ',') {
          while (lexer.peekNextChar() == ',') {
            lexer.advance()
            lexer.skipWhitespace()
            when (val additionalResult = matchingDefinitionExp()) {
              is Result.Ok -> {
                definitions = definitions.merge(additionalResult.value)
                lexer.skipWhitespace()
              }
              is Result.Err -> return additionalResult
            }
          }
          definitions
        } else {
          definitions
        }
      }
      is Result.Err -> return result
    }

    return if (lexer.empty) {
      Result.Ok(definition)
    } else {
      Result.Err(parseError("Error parsing expression: Unexpected characters at ${lexer.index}"))
    }
  }

  fun parseError(message: String): String {
    return message +
      "\n        ${lexer.buffer}" +
      "\n        ${lexer.highlightPosition()}"
  }

  //  matchingDefinitionExp returns [ MatchingRuleDefinition value ] :
  //      (
  //        'matching' LEFT_BRACKET matchingRule RIGHT_BRACKET
  //        | 'notEmpty' LEFT_BRACKET primitiveValue RIGHT_BRACKET
  //        | 'eachKey' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET
  //        | 'eachValue' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET
  //        | 'atLeast' LEFT_BRACKET DIGIT+ RIGHT_BRACKET
  //        | 'atMost' LEFT_BRACKET DIGIT+ RIGHT_BRACKET
  //      )
  //      ;
  @Suppress("ReturnCount", "LongMethod")
  fun matchingDefinitionExp(): Result<MatchingRuleDefinition, String> {
    return when {
      lexer.matchString("matching") -> {
        if (matchChar('(')) {
          when (val matchingRuleResult = matchingRule()) {
            is Result.Ok -> {
              if (matchChar(')')) {
                if (matchingRuleResult.value.reference != null) {
                  Result.Ok(
                    MatchingRuleDefinition(
                      matchingRuleResult.value.value, matchingRuleResult.value.reference!!,
                      matchingRuleResult.value.generator
                    )
                  )
                } else {
                  Result.Ok(
                    MatchingRuleDefinition(
                      matchingRuleResult.value.value, matchingRuleResult.value.rule,
                      matchingRuleResult.value.generator
                    )
                  )
                }
              } else {
                Result.Err(parseError("Was expecting a ')' at index ${lexer.index}"))
              }
            }
            is Result.Err -> return matchingRuleResult
          }
        } else {
          Result.Err(parseError("Was expecting a '(' at index ${lexer.index}"))
        }
      }
      lexer.matchString("notEmpty") -> {
        if (matchChar('(')) {
          when (val primitiveValueResult = primitiveValue()) {
            is Result.Ok -> {
              if (matchChar(')')) {
                Result.Ok(MatchingRuleDefinition(primitiveValueResult.value.first, NotEmptyMatcher, null)
                  .withType(primitiveValueResult.value.second))
              } else {
                Result.Err(parseError("Was expecting a ')' at index ${lexer.index}"))
              }
            }
            is Result.Err -> return primitiveValueResult
          }
        } else {
          Result.Err(parseError("Was expecting a '(' at index ${lexer.index}"))
        }
      }
      lexer.matchString("eachKey") -> {
        if (matchChar('(')) {
          when (val definitionResult = matchingDefinitionExp()) {
            is Result.Ok -> {
              if (matchChar(')')) {
                Result.Ok(MatchingRuleDefinition(null, EachKeyMatcher(definitionResult.value), null))
              } else {
                Result.Err(parseError("Was expecting a ')' at index ${lexer.index}"))
              }
            }
            is Result.Err -> return definitionResult
          }
        } else {
          Result.Err(parseError("Was expecting a '(' at index ${lexer.index}"))
        }
      }
      lexer.matchString("eachValue") -> {
        if (matchChar('(')) {
          when (val definitionResult = matchingDefinitionExp()) {
            is Result.Ok -> {
              if (matchChar(')')) {
                Result.Ok(MatchingRuleDefinition(null, ValueType.Unknown,
                  listOf(Either.A(EachValueMatcher(definitionResult.value))), null))
              } else {
                Result.Err(parseError("Was expecting a ')' at index ${lexer.index}"))
              }
            }
            is Result.Err -> return definitionResult
          }
        } else {
          Result.Err(parseError("Was expecting a '(' at index ${lexer.index}"))
        }
      }
      lexer.matchString("atLeast") -> {
        if (matchChar('(')) {
          when (val lengthResult = unsignedNumber()) {
            is Result.Ok -> {
              if (matchChar(')')) {
                Result.Ok(MatchingRuleDefinition("", MinTypeMatcher(lengthResult.value), null))
              } else {
                Result.Err(parseError("Was expecting a ')' at index ${lexer.index}"))
              }
            }
            is Result.Err -> return lengthResult
          }
        } else {
          Result.Err(parseError("Was expecting a '(' at index ${lexer.index}"))
        }
      }
      lexer.matchString("atMost") -> {
        if (matchChar('(')) {
          when (val lengthResult = unsignedNumber()) {
            is Result.Ok -> {
              if (matchChar(')')) {
                Result.Ok(MatchingRuleDefinition("", MaxTypeMatcher(lengthResult.value), null))
              } else {
                Result.Err(parseError("Was expecting a ')' at index ${lexer.index}"))
              }
            }
            is Result.Err -> return lengthResult
          }
        } else {
          Result.Err(parseError("Was expecting a '(' at index ${lexer.index}"))
        }
      }
      else -> Result.Err(parseError("Was expecting a matching rule definition type at index ${lexer.index}"))
    }
  }

  private fun matchChar(c: Char): Boolean {
    lexer.skipWhitespace()
    return lexer.matchChar(c)
  }

  //  matchingRule returns [ String value, ValueType type, MatchingRule rule, Generator generator, MatchingReference reference ] :
  //    (
  //      ( 'equalTo' { $rule = EqualsMatcher.INSTANCE; }
  //      | 'type'  { $rule = TypeMatcher.INSTANCE; } )
  //      COMMA v=primitiveValue { $value = $v.value; $type = $v.type; } )
  //    | 'number' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER); } COMMA val=( DECIMAL_LITERAL | INTEGER_LITERAL ) { $value = $val.getText(); $type = ValueType.Number; }
  //    | 'integer' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER); } COMMA val=INTEGER_LITERAL { $value = $val.getText(); $type = ValueType.Integer; }
  //    | 'decimal' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL); } COMMA val=DECIMAL_LITERAL { $value = $val.getText(); $type = ValueType.Decimal; }
  //    | matcherType=( 'datetime' | 'date' | 'time' ) COMMA format=string {
  //      if ($matcherType.getText().equals("datetime")) { $rule = new TimestampMatcher($format.contents); }
  //      if ($matcherType.getText().equals("date")) { $rule = new DateMatcher($format.contents); }
  //      if ($matcherType.getText().equals("time")) { $rule = new TimeMatcher($format.contents); }
  //      } COMMA s=string { $value = $s.contents; $type = ValueType.String; }
  //    | 'regex' COMMA r=string COMMA s=string { $rule = new RegexMatcher($r.contents); $value = $s.contents; $type = ValueType.String; }
  //    | 'include' COMMA s=string { $rule = new IncludeMatcher($s.contents); $value = $s.contents; $type = ValueType.String; }
  //    | 'boolean' COMMA BOOLEAN_LITERAL { $rule = BooleanMatcher.INSTANCE; $value = $BOOLEAN_LITERAL.getText(); $type = ValueType.Boolean; }
  //    | 'semver' COMMA s=string { $rule = SemverMatcher.INSTANCE; $value = $s.contents; $type = ValueType.String; }
  //    | 'contentType' COMMA ct=string COMMA s=string { $rule = new ContentTypeMatcher($ct.contents); $value = $s.contents; $type = ValueType.Unknown; }
  //    | DOLLAR ref=string { $reference = new MatchingReference($ref.contents); $type = ValueType.Unknown; }
  //    ;
  fun matchingRule(): Result<MatchingRuleResult, String> {
    lexer.skipWhitespace()
    val equalTo = lexer.matchString("equalTo")
    return when {
      equalTo || lexer.matchString("type") -> matchEqualOrType(equalTo)
      lexer.matchString("number") -> matchNumber()
      lexer.matchString("integer") -> matchInteger()
      lexer.matchString("decimal") -> matchDecimal()
      lexer.matchString("datetime") || lexer.matchString("date") || lexer.matchString("time") ->
        matchDateTime()
      lexer.matchString("regex") -> matchRegex()
      lexer.matchString("include") -> matchInclude()
      lexer.matchString("boolean") -> matchBoolean()
      lexer.matchString("semver") -> matchSemver()
      lexer.matchString("contentType") -> matchContentType()
      lexer.peekNextChar() == '$' -> matchReference()
      else -> Result.Err("Was expecting a matching rule definition at index ${lexer.index}")
    }
  }

  private fun matchRegex() = if (matchChar(',')) {
    when (val regexResult = string()) {
      is Result.Ok -> {
        if (regexResult.value != null) {
          if (matchChar(',')) {
            when (val stringResult = string()) {
              is Result.Ok -> Result.Ok(
                MatchingRuleResult(stringResult.value, ValueType.String, RegexMatcher(regexResult.value!!))
              )

              is Result.Err -> stringResult
            }
          } else {
            Result.Err("Was expecting a ',' at index ${lexer.index}")
          }
        } else {
          Result.Err("Regex can not be null (at index ${lexer.index})")
        }
      }

      is Result.Err -> regexResult
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun matchDateTime(): Result<MatchingRuleResult, String> {
    val type = lexer.lastMatch
    return if (matchChar(',')) {
      when (val formatResult = string()) {
        is Result.Ok -> {
          val matcher = when (type) {
            "date" -> if (formatResult.value != null) DateMatcher(formatResult.value!!) else DateMatcher()
            "time" -> if (formatResult.value != null) TimeMatcher(formatResult.value!!) else TimeMatcher()
            else -> if (formatResult.value != null) TimestampMatcher(formatResult.value!!) else TimestampMatcher()
          }
          if (matchChar(',')) {
            when (val stringResult = string()) {
              is Result.Ok -> Result.Ok(MatchingRuleResult(stringResult.value, ValueType.String, matcher))
              is Result.Err -> stringResult
            }
          } else {
            Result.Err("Was expecting a ',' at index ${lexer.index}")
          }
        }

        is Result.Err -> formatResult
      }
    } else {
      Result.Err("Was expecting a ',' at index ${lexer.index}")
    }
  }

  private fun matchDecimal() = if (matchChar(',')) {
    lexer.skipWhitespace()
    when {
      lexer.matchDecimal() -> Result.Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Decimal,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
        )
      )

      else -> Result.Err("Was expecting a decimal number at index ${lexer.index}")
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun matchInteger() = if (matchChar(',')) {
    lexer.skipWhitespace()
    when {
      lexer.matchInteger() -> Result.Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Integer,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)
        )
      )

      else -> Result.Err("Was expecting an integer at index ${lexer.index}")
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun matchNumber() = if (matchChar(',')) {
    lexer.skipWhitespace()
    when {
      lexer.matchDecimal() -> Result.Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Number,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
        )
      )

      lexer.matchInteger() -> Result.Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Number,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
        )
      )

      else -> Result.Err("Was expecting a number at index ${lexer.index}")
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun unsignedNumber(): Result<Int, String> {
    lexer.skipWhitespace()
    return if (lexer.matchWholeNumber()) {
      Result.Ok(lexer.lastMatch!!.toInt())
    } else {
      Result.Err("Was expecting an unsigned number at index ${lexer.index}")
    }
  }

  private fun matchEqualOrType(equalTo: Boolean) = if (matchChar(',')) {
    when (val primitiveValueResult = primitiveValue()) {
      is Result.Ok -> {
        Result.Ok(
          MatchingRuleResult(
            primitiveValueResult.value.first, primitiveValueResult.value.second,
            if (equalTo) EqualsMatcher else TypeMatcher
          )
        )
      }

      is Result.Err -> primitiveValueResult
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'include' COMMA s=string { $rule = new IncludeMatcher($s.contents); $value = $s.contents; $type = ValueType.String; }
  private fun matchInclude() = if (matchChar(',')) {
    when (val stringResult = string()) {
      is Result.Ok -> Result.Ok(MatchingRuleResult(stringResult.value, ValueType.String, IncludeMatcher(stringResult.value.toString())))
      is Result.Err -> stringResult
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'boolean' COMMA BOOLEAN_LITERAL { $rule = BooleanMatcher.INSTANCE; $value = $BOOLEAN_LITERAL.getText(); $type = ValueType.Boolean; }
  private fun matchBoolean() = if (matchChar(',')) {
    lexer.skipWhitespace()
    if (lexer.matchBoolean()) {
      Result.Ok(MatchingRuleResult(lexer.lastMatch, ValueType.Boolean, BooleanMatcher))
    } else {
      Result.Err("Was expecting a boolean value at index ${lexer.index}")
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'semver' COMMA s=string { $rule = SemverMatcher.INSTANCE; $value = $s.contents; $type = ValueType.String; }
  private fun matchSemver() = if (matchChar(',')) {
    when (val stringResult = string()) {
      is Result.Ok -> Result.Ok(MatchingRuleResult(stringResult.value, ValueType.String, SemverMatcher))
      is Result.Err -> stringResult
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'contentType' COMMA ct=string COMMA s=string { $rule = new ContentTypeMatcher($ct.contents); $value = $s.contents; $type = ValueType.Unknown; }
  private fun matchContentType() = if (matchChar(',')) {
    when (val ctResult = string()) {
      is Result.Ok -> {
        if (ctResult.value != null) {
          if (matchChar(',')) {
            when (val stringResult = string()) {
              is Result.Ok -> Result.Ok(
                MatchingRuleResult(stringResult.value, ValueType.Unknown, ContentTypeMatcher(ctResult.value!!))
              )
              is Result.Err -> stringResult
            }
          } else {
            Result.Err("Was expecting a ',' at index ${lexer.index}")
          }
        } else {
          Result.Err("Content type can not be null (at index ${lexer.index})")
        }
      }
      is Result.Err -> ctResult
    }
  } else {
    Result.Err("Was expecting a ',' at index ${lexer.index}")
  }

  // DOLLAR ref=string { $reference = new MatchingReference($ref.contents); $type = ValueType.Unknown; }
  private fun matchReference() = if (matchChar('$')) {
    when (val stringResult = string()) {
      is Result.Ok -> if (stringResult.value != null) {
        Result.Ok(MatchingRuleResult(null, ValueType.Unknown, null, null, MatchingReference(stringResult.value!!)))
      } else {
        Result.Err("Matching reference value must not be null (at index ${lexer.index})")
      }
      is Result.Err -> stringResult
    }
  } else {
    Result.Err("Was expecting a '$' at index ${lexer.index}")
  }

  //  primitiveValue returns [ String value, ValueType type ] :
  //    string
  //    | v=DECIMAL_LITERAL
  //    | v=INTEGER_LITERAL
  //    | v=BOOLEAN_LITERAL
  //    ;
  fun primitiveValue(): Result<Pair<String?, ValueType>, String> {
    lexer.skipWhitespace()
    return when {
      lexer.peekNextChar() == '\'' -> {
        when (val stringResult = string()) {
          is Result.Ok -> Result.Ok(stringResult.value to ValueType.String)
          is Result.Err -> stringResult
        }
      }
      lexer.matchString("null") -> Result.Ok(null to ValueType.String)
      lexer.matchDecimal() -> Result.Ok(lexer.lastMatch to ValueType.Decimal)
      lexer.matchInteger() -> Result.Ok(lexer.lastMatch to ValueType.Decimal)
      lexer.matchBoolean() -> Result.Ok(lexer.lastMatch to ValueType.Boolean)
      else -> Result.Err("Was expecting a primitive value at index ${lexer.index}")
    }
  }

  //  string returns [ String contents ] :
  //    STRING_LITERAL {
  //      String contents = $STRING_LITERAL.getText();
  //      $contents = contents.substring(1, contents.length() - 1);
  //    }
  //    | 'null'
  //    ;
  fun string(): Result<String?, String> {
    lexer.skipWhitespace()
    return if (lexer.matchChar('\'')) {
      var ch = lexer.nextChar()
      var ch2 = lexer.peekNextChar()
      var stringResult = ""
      while (ch != null && ((ch == '\\' && ch2 == '\'') || (ch != '\''))) {
        stringResult += ch
        if (ch == '\\' && ch2 == '\'') {
          stringResult += ch2
          lexer.advance()
        }
        ch = lexer.nextChar()
        ch2 = lexer.peekNextChar()
      }

      if (ch == '\'') {
        processRawString(stringResult)
      } else {
        Result.Err("Unterminated string found at index ${lexer.index}")
      }
    } else {
      Result.Err("Was expecting a string at index ${lexer.index}")
    }
  }

  @Suppress("ComplexMethod", "LongMethod")
  fun processRawString(rawString: String): Result<String, String> {
    val buffer = StringBuilder(rawString.length)
    val chars = rawString.chars().iterator()
    while (chars.hasNext()) {
      val ch = chars.nextInt().toChar()
      if (ch == '\\') {
        if (chars.hasNext()) {
          when (val ch2 = chars.nextInt().toChar()) {
            '\\' -> buffer.append(ch)
            'b' -> buffer.append('\u0008')
            'f' -> buffer.append('\u000C')
            'n' -> buffer.append('\n')
            'r' -> buffer.append('\r')
            't' -> buffer.append('\t')
            'u' -> {
              if (!chars.hasNext()) {
                return Result.Err("Invalid unicode escape found at index ${lexer.index}")
              }
              val code1 = chars.nextInt().toChar()
              val b = StringBuilder(4)
              if (code1 == '{') {
                var c: Char? = null
                while (chars.hasNext()) {
                  c = chars.nextInt().toChar()
                  if (c == '}') {
                    break
                  }
                  b.append(c)
                }
                if (c != '}') {
                  return Result.Err("Invalid unicode escape found at index ${lexer.index}")
                }
              } else {
                b.append(code1)
                if (!chars.hasNext()) {
                  return Result.Err("Invalid unicode escape found at index ${lexer.index}")
                }
                val code2 = chars.nextInt().toChar()
                b.append(code2)
                if (!chars.hasNext()) {
                  return Result.Err("Invalid unicode escape found at index ${lexer.index}")
                }
                val code3 = chars.nextInt().toChar()
                b.append(code3)
                if (!chars.hasNext()) {
                  return Result.Err("Invalid unicode escape found at index ${lexer.index}")
                }
                val code4 = chars.nextInt().toChar()
                b.append(code4)
              }
              val code = try {
                b.toString().toInt(16)
              } catch (e: NumberFormatException) {
                return Result.Err("Invalid unicode escape found at index ${lexer.index}")
              }
              buffer.append(Character.toString(code))
            }
            else -> {
              buffer.append(ch)
              buffer.append(ch2)
            }
          }
        } else {
          buffer.append(ch)
        }
      } else {
        buffer.append(ch)
      }
    }
    return Result.Ok(buffer.toString())
  }
}
