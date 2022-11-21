package au.com.dius.pact.core.support.generators.expressions

import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.parsers.StringLexer

class TimeExpressionLexer(expression: String): StringLexer(expression) {
  companion object {
    val HOURS = Regex("^hours?")
    val SECONDS = Regex("^seconds?")
    val MINUTES = Regex("^minutes?")
    val MILLISECONDS = Regex("^milliseconds?")
  }
}

@Suppress("MaxLineLength")
class TimeExpressionParser(private val lexer: TimeExpressionLexer) {
  //expression returns [ TimeBase timeBase = TimeBase.Now.INSTANCE, List<Adjustment<TimeOffsetType>> adj = new ArrayList<>() ] : ( base { $timeBase = $base.t; }
  //    | op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
  //    | base { $timeBase = $base.t; } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
  //    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }
  //    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }  ( op duration {
  //        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
  //    } )*
  //    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); }
  //    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); } ( op duration {
  //        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
  //    } )*
  //    ) EOF
  @Suppress("ComplexMethod", "ReturnCount")
  fun expression(): Result<Pair<TimeBase, List<Adjustment<TimeOffsetType>>>, String> {
    val timeBase = TimeBase.Now

    val baseResult = base()
    if (baseResult is Result.Ok && baseResult.value != null) {
      return when (val opResult = parseOp()) {
        is Result.Ok -> if (opResult.value != null) {
          Result.Ok(baseResult.value to opResult.value)
        } else {
          Result.Ok(baseResult.value to emptyList())
        }
        is Result.Err -> opResult
      }
    } else if (baseResult is Result.Err) {
      return baseResult
    }

    when (val opResult = parseOp()) {
      is Result.Ok -> if (opResult.value != null) {
        return Result.Ok(timeBase to opResult.value)
      }
      is Result.Err -> return opResult
    }

    val nextOrLastResult = parseNextOrLast()
    if (nextOrLastResult != null) {
      return when (val offsetResult = offset()) {
        is Result.Ok -> {
          val adj = mutableListOf<Adjustment<TimeOffsetType>>()
          adj.add(Adjustment(offsetResult.value.first, offsetResult.value.second, nextOrLastResult))
          when (val opResult = parseOp()) {
            is Result.Ok -> if (opResult.value != null) {
              adj.addAll(opResult.value)
              Result.Ok(timeBase to adj)
            } else {
              Result.Ok(timeBase to adj)
            }
            is Result.Err -> opResult
          }
        }
        is Result.Err -> offsetResult
      }
    }

    return if (lexer.empty) {
      Result.Ok(timeBase to emptyList())
    } else {
      Result.Err("Unexpected characters '${lexer.remainder}' at index ${lexer.index}")
    }
  }

  @Suppress("ReturnCount")
  private fun parseOp(): Result<List<Adjustment<TimeOffsetType>>?, String> {
    val adj = mutableListOf<Adjustment<TimeOffsetType>>()
    var opResult = op()
    if (opResult != null) {
      while (opResult != null) {
        when (val durationResult = duration()) {
          is Result.Ok -> adj.add(durationResult.value.withOperation(opResult))
          is Result.Err -> return durationResult
        }
        opResult = op()
      }
      return Result.Ok(adj)
    }
    return Result.Ok(null)
  }

  //base returns [ TimeBase t ] : 'now' { $t = TimeBase.Now.INSTANCE; }
  //    | 'midnight' { $t = TimeBase.Midnight.INSTANCE; }
  //    | 'noon' { $t = TimeBase.Noon.INSTANCE; }
  //    | INT oclock { $t = TimeBase.of($INT.int, $oclock.h); }
  //    ;
  fun base(): Result<TimeBase?, String> {
    lexer.skipWhitespace()

    val result = lexer.matchRegex(StringLexer.INT)
    return if (result != null) {
      val intValue = result.toInt()
      when (val hourResult = oclock()) {
        is Result.Ok -> Result.Ok(TimeBase.of(intValue, hourResult.value))
        is Result.Err -> Result.Err(hourResult.error)
      }
    } else {
      when {
        lexer.matchString("now") -> Result.Ok(TimeBase.Now)
        lexer.matchString("midnight") -> Result.Ok(TimeBase.Midnight)
        lexer.matchString("noon") -> Result.Ok(TimeBase.Noon)
        else -> Result.Ok(null)
      }
    }
  }

  //oclock returns [ ClockHour h ] : 'o\'clock' 'am' { $h = ClockHour.AM; }
  //    | 'o\'clock' 'pm' { $h = ClockHour.PM; }
  //    | 'o\'clock' { $h = ClockHour.NEXT; }
  fun oclock(): Result<ClockHour, String> {
    lexer.skipWhitespace()
    return if (lexer.matchString("o'clock")) {
      lexer.skipWhitespace()
      when {
        lexer.matchString("am") -> Result.Ok(ClockHour.AM)
        lexer.matchString("pm") -> Result.Ok(ClockHour.PM)
        else -> Result.Ok(ClockHour.NEXT)
      }
    } else {
      Result.Err("Was expecting a clock hour at index ${lexer.index}")
    }
  }

  //duration returns [ Adjustment<TimeOffsetType> d ] : INT durationType { $d = new Adjustment<TimeOffsetType>($durationType.type, $INT.int); } ;
  fun duration(): Result<Adjustment<TimeOffsetType>, String> {
    lexer.skipWhitespace()

    val intResult = when (val result = lexer.parseInt()) {
      is Result.Ok -> result.value
      is Result.Err -> return result
    }

    val durationTypeResult = durationType()
    return if (durationTypeResult != null) {
      Result.Ok(Adjustment(durationTypeResult, intResult))
    } else {
      Result.Err("Was expecting a duration type at index ${lexer.index}")
    }
  }

  //durationType returns [ TimeOffsetType type ] : 'hour' { $type = TimeOffsetType.HOUR; }
  //    | HOURS { $type = TimeOffsetType.HOUR; }
  //    | 'minute' { $type = TimeOffsetType.MINUTE; }
  //    | MINUTES { $type = TimeOffsetType.MINUTE; }
  //    | 'second' { $type = TimeOffsetType.SECOND; }
  //    | SECONDS { $type = TimeOffsetType.SECOND; }
  //    | 'millisecond' { $type = TimeOffsetType.MILLISECOND; }
  //    | MILLISECONDS { $type = TimeOffsetType.MILLISECOND; }
  //    ;
  fun durationType(): TimeOffsetType? {
    lexer.skipWhitespace()
    return when {
      lexer.matchRegex(TimeExpressionLexer.HOURS) != null -> TimeOffsetType.HOUR
      lexer.matchRegex(TimeExpressionLexer.MINUTES) != null -> TimeOffsetType.MINUTE
      lexer.matchRegex(TimeExpressionLexer.SECONDS) != null -> TimeOffsetType.SECOND
      lexer.matchRegex(TimeExpressionLexer.MILLISECONDS) != null -> TimeOffsetType.MILLISECOND
      else -> null
    }
  }

  //op returns [ Operation o ] : '+' { $o = Operation.PLUS; }
  //    | '-' { $o = Operation.MINUS; }
  //    ;
  fun op(): Operation? {
    lexer.skipWhitespace()
    return when {
      lexer.matchChar('+') -> Operation.PLUS
      lexer.matchChar('-') -> Operation.MINUS
      else -> null
    }
  }

  //offset returns [ TimeOffsetType type, int val = 1 ] : 'hour' { $type = TimeOffsetType.HOUR; }
  //    | 'minute' { $type = TimeOffsetType.MINUTE; }
  //    | 'second' { $type = TimeOffsetType.SECOND; }
  //    | 'millisecond' { $type = TimeOffsetType.MILLISECOND; }
  //    ;
  fun offset(): Result<Pair<TimeOffsetType, Int>, String> {
    lexer.skipWhitespace()
    return when {
      lexer.matchString("hour") -> Result.Ok(TimeOffsetType.HOUR to 1)
      lexer.matchString("minute") -> Result.Ok(TimeOffsetType.MINUTE to 1)
      lexer.matchString("second") -> Result.Ok(TimeOffsetType.SECOND to 1)
      lexer.matchString("millisecond") -> Result.Ok(TimeOffsetType.MILLISECOND to 1)
      else -> Result.Err("Was expecting an offset type at index ${lexer.index}")
    }
  }

  private fun parseNextOrLast(): Operation? {
    lexer.skipWhitespace()
    return when {
      lexer.matchString("next") -> Operation.PLUS
      lexer.matchString("last") -> Operation.MINUS
      else -> null
    }
  }
}
