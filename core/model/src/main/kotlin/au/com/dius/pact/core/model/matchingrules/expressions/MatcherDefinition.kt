package au.com.dius.pact.core.model.matchingrules.expressions;

import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EachKeyMatcher
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.NotEmptyMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.SemverMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Either
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.parsers.StringLexer
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class MatcherDefinitionLexer(expression: String): StringLexer(expression) {
  fun matchDecimal() = matchRegex(DECIMAL_LITERAL).isNotEmpty()

  fun matchInteger() = matchRegex(INTEGER_LITERAL).isNotEmpty()

  fun matchBoolean() = matchRegex(BOOLEAN_LITERAL).isNotEmpty()

  companion object {
    val INTEGER_LITERAL = Regex("^-?\\d+")
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
      is Ok -> {
        var definitions = result.value
        lexer.skipWhitespace()
        if (lexer.peekNextChar() == ',') {
          while (lexer.peekNextChar() == ',') {
            lexer.advance()
            lexer.skipWhitespace()
            when (val additionalResult = matchingDefinitionExp()) {
              is Ok -> {
                definitions = definitions.merge(additionalResult.value)
                lexer.skipWhitespace()
              }
              is Err -> return additionalResult
            }
          }
          definitions
        } else {
          definitions
        }
      }
      is Err -> return result
    }

    return if (lexer.empty) {
      Ok(definition)
    } else {
      Err("Error parsing expression: Unexpected characters '${lexer.remainder}' at ${lexer.index}")
    }
  }

  //  matchingDefinitionExp returns [ MatchingRuleDefinition value ] :
  //      (
  //        'matching' LEFT_BRACKET matchingRule RIGHT_BRACKET {
  //          if ($matchingRule.reference != null) {
  //            $value = new MatchingRuleDefinition($matchingRule.value, $matchingRule.reference, $matchingRule.generator);
  //          } else {
  //            $value = new MatchingRuleDefinition($matchingRule.value, $matchingRule.rule, $matchingRule.generator);
  //          }
  //        }
  //        | 'notEmpty' LEFT_BRACKET primitiveValue RIGHT_BRACKET { $value = new MatchingRuleDefinition($primitiveValue.value, NotEmptyMatcher.INSTANCE, null).withType($primitiveValue.type); }
  //        | 'eachKey' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET { if ($e.value != null) { $value = new MatchingRuleDefinition(null, new EachKeyMatcher($e.value), null); } }
  //        | 'eachValue' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET {
  //          if ($e.value != null) {
  //            $value = new MatchingRuleDefinition(null, ValueType.Unknown, List.of((Either<MatchingRule, MatchingReference>) new Either.A(new EachValueMatcher($e.value))), null);
  //          }
  //        }
  //      )
  //      ;
  @Suppress("ReturnCount")
  fun matchingDefinitionExp(): Result<MatchingRuleDefinition, String> {
    return when {
      lexer.matchString("matching") -> {
        if (matchChar('(')) {
          when (val matchingRuleResult = matchingRule()) {
            is Ok -> {
              if (matchChar(')')) {
                if (matchingRuleResult.value.reference != null) {
                  Ok(
                    MatchingRuleDefinition(
                      matchingRuleResult.value.value, matchingRuleResult.value.reference!!,
                      matchingRuleResult.value.generator
                    )
                  )
                } else {
                  Ok(
                    MatchingRuleDefinition(
                      matchingRuleResult.value.value, matchingRuleResult.value.rule,
                      matchingRuleResult.value.generator
                    )
                  )
                }
              } else {
                Err("Was expecting a ')' at index ${lexer.index}")
              }
            }
            is Err -> return matchingRuleResult
          }
        } else {
          Err("Was expecting a '(' at index ${lexer.index}")
        }
      }
      lexer.matchString("notEmpty") -> {
        if (matchChar('(')) {
          when (val primitiveValueResult = primitiveValue()) {
            is Ok -> {
              if (matchChar(')')) {
                Ok(MatchingRuleDefinition(primitiveValueResult.value.first, NotEmptyMatcher, null)
                  .withType(primitiveValueResult.value.second))
              } else {
                Err("Was expecting a ')' at index ${lexer.index}")
              }
            }
            is Err -> return primitiveValueResult
          }
        } else {
          Err("Was expecting a '(' at index ${lexer.index}")
        }
      }
      lexer.matchString("eachKey") -> {
        if (matchChar('(')) {
          when (val definitionResult = matchingDefinitionExp()) {
            is Ok -> {
              if (matchChar(')')) {
                Ok(MatchingRuleDefinition(null, EachKeyMatcher(definitionResult.value), null))
              } else {
                Err("Was expecting a ')' at index ${lexer.index}")
              }
            }
            is Err -> return definitionResult
          }
        } else {
          Err("Was expecting a '(' at index ${lexer.index}")
        }
      }
      lexer.matchString("eachValue") -> {
        if (matchChar('(')) {
          when (val definitionResult = matchingDefinitionExp()) {
            is Ok -> {
              if (matchChar(')')) {
                Ok(MatchingRuleDefinition(null, ValueType.Unknown,
                  listOf(Either.A(EachValueMatcher(definitionResult.value))), null))
              } else {
                Err("Was expecting a ')' at index ${lexer.index}")
              }
            }
            is Err -> return definitionResult
          }
        } else {
          Err("Was expecting a '(' at index ${lexer.index}")
        }
      }
      else -> Err("Was expecting a matching rule definition type at index ${lexer.index}")
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
      else -> Err("Was expecting a matching rule definition at index ${lexer.index}")
    }
  }

  private fun matchRegex() = if (matchChar(',')) {
    when (val regexResult = string()) {
      is Ok -> {
        if (regexResult.value != null) {
          if (matchChar(',')) {
            when (val stringResult = string()) {
              is Ok -> Ok(
                MatchingRuleResult(stringResult.value, ValueType.String, RegexMatcher(regexResult.value!!))
              )

              is Err -> stringResult
            }
          } else {
            Err("Was expecting a ',' at index ${lexer.index}")
          }
        } else {
          Err("Regex can not be null (at index ${lexer.index})")
        }
      }

      is Err -> regexResult
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun matchDateTime(): Result<MatchingRuleResult, String> {
    val type = lexer.lastMatch
    return if (matchChar(',')) {
      when (val formatResult = string()) {
        is Ok -> {
          val matcher = when (type) {
            "date" -> if (formatResult.value != null) DateMatcher(formatResult.value!!) else DateMatcher()
            "time" -> if (formatResult.value != null) TimeMatcher(formatResult.value!!) else TimeMatcher()
            else -> if (formatResult.value != null) TimestampMatcher(formatResult.value!!) else TimestampMatcher()
          }
          if (matchChar(',')) {
            when (val stringResult = string()) {
              is Ok -> Ok(MatchingRuleResult(stringResult.value, ValueType.String, matcher))
              is Err -> stringResult
            }
          } else {
            Err("Was expecting a ',' at index ${lexer.index}")
          }
        }

        is Err -> formatResult
      }
    } else {
      Err("Was expecting a ',' at index ${lexer.index}")
    }
  }

  private fun matchDecimal() = if (matchChar(',')) {
    lexer.skipWhitespace()
    when {
      lexer.matchDecimal() -> Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Decimal,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
        )
      )

      else -> Err("Was expecting a decimal number at index ${lexer.index}")
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun matchInteger() = if (matchChar(',')) {
    lexer.skipWhitespace()
    when {
      lexer.matchInteger() -> Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Integer,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)
        )
      )

      else -> Err("Was expecting an integer at index ${lexer.index}")
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun matchNumber() = if (matchChar(',')) {
    lexer.skipWhitespace()
    when {
      lexer.matchDecimal() -> Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Number,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
        )
      )

      lexer.matchInteger() -> Ok(
        MatchingRuleResult(
          lexer.lastMatch, ValueType.Number,
          NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
        )
      )

      else -> Err("Was expecting a number at index ${lexer.index}")
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  private fun matchEqualOrType(equalTo: Boolean) = if (matchChar(',')) {
    when (val primitiveValueResult = primitiveValue()) {
      is Ok -> {
        Ok(
          MatchingRuleResult(
            primitiveValueResult.value.first, primitiveValueResult.value.second,
            if (equalTo) EqualsMatcher else TypeMatcher
          )
        )
      }

      is Err -> primitiveValueResult
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'include' COMMA s=string { $rule = new IncludeMatcher($s.contents); $value = $s.contents; $type = ValueType.String; }
  private fun matchInclude() = if (matchChar(',')) {
    when (val stringResult = string()) {
      is Ok -> Ok(MatchingRuleResult(stringResult.value, ValueType.String, IncludeMatcher(stringResult.value.toString())))
      is Err -> stringResult
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'boolean' COMMA BOOLEAN_LITERAL { $rule = BooleanMatcher.INSTANCE; $value = $BOOLEAN_LITERAL.getText(); $type = ValueType.Boolean; }
  private fun matchBoolean() = if (matchChar(',')) {
    lexer.skipWhitespace()
    if (lexer.matchBoolean()) {
      Ok(MatchingRuleResult(lexer.lastMatch, ValueType.Boolean, BooleanMatcher))
    } else {
      Err("Was expecting a boolean value at index ${lexer.index}")
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'semver' COMMA s=string { $rule = SemverMatcher.INSTANCE; $value = $s.contents; $type = ValueType.String; }
  private fun matchSemver() = if (matchChar(',')) {
    when (val stringResult = string()) {
      is Ok -> Ok(MatchingRuleResult(stringResult.value, ValueType.String, SemverMatcher))
      is Err -> stringResult
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  // 'contentType' COMMA ct=string COMMA s=string { $rule = new ContentTypeMatcher($ct.contents); $value = $s.contents; $type = ValueType.Unknown; }
  private fun matchContentType() = if (matchChar(',')) {
    when (val ctResult = string()) {
      is Ok -> {
        if (ctResult.value != null) {
          if (matchChar(',')) {
            when (val stringResult = string()) {
              is Ok -> Ok(
                MatchingRuleResult(stringResult.value, ValueType.Unknown, ContentTypeMatcher(ctResult.value!!))
              )
              is Err -> stringResult
            }
          } else {
            Err("Was expecting a ',' at index ${lexer.index}")
          }
        } else {
          Err("Content type can not be null (at index ${lexer.index})")
        }
      }
      is Err -> ctResult
    }
  } else {
    Err("Was expecting a ',' at index ${lexer.index}")
  }

  // DOLLAR ref=string { $reference = new MatchingReference($ref.contents); $type = ValueType.Unknown; }
  private fun matchReference() = if (matchChar('$')) {
    when (val stringResult = string()) {
      is Ok -> if (stringResult.value != null) {
        Ok(MatchingRuleResult(null, ValueType.Unknown, null, null, MatchingReference(stringResult.value!!)))
      } else {
        Err("Matching reference value must not be null (at index ${lexer.index})")
      }
      is Err -> stringResult
    }
  } else {
    Err("Was expecting a '$' at index ${lexer.index}")
  }

  //  primitiveValue returns [ String value, ValueType type ] :
  //    string { $value = $string.contents; $type = ValueType.String; }
  //    | v=DECIMAL_LITERAL { $value = $v.getText(); $type = ValueType.Decimal; }
  //    | v=INTEGER_LITERAL { $value = $v.getText(); $type = ValueType.Integer; }
  //    | v=BOOLEAN_LITERAL { $value = $v.getText(); $type = ValueType.Boolean; }
  //    ;
  fun primitiveValue(): Result<Pair<String?, ValueType>, String> {
    lexer.skipWhitespace()
    return when {
      lexer.peekNextChar() == '\'' -> {
        when (val stringResult = string()) {
          is Ok -> Ok(stringResult.value to ValueType.String)
          is Err -> stringResult
        }
      }
      lexer.matchString("null") -> Ok(null to ValueType.String)
      lexer.matchDecimal() -> Ok(lexer.lastMatch to ValueType.Decimal)
      lexer.matchInteger() -> Ok(lexer.lastMatch to ValueType.Decimal)
      lexer.matchBoolean() -> Ok(lexer.lastMatch to ValueType.Boolean)
      else -> Err("Was expecting a primitive value at index ${lexer.index}")
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
      var ch = lexer.peekNextChar()
      var stringResult = ""
      while (ch != '\'' && ch != null) {
        stringResult += ch
        lexer.advance()
        ch = lexer.peekNextChar()
      }

      if (ch == '\'') {
        lexer.advance()
        Ok(stringResult)
      } else {
        Err("Unterminated string found at index ${lexer.index}")
      }
    } else {
      Err("Was expecting a string at index ${lexer.index}")
    }
  }
}
