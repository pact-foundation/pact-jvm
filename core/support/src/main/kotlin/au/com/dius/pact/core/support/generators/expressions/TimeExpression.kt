package au.com.dius.pact.core.support.generators.expressions

import au.com.dius.pact.core.support.parsers.StringLexer
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

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
    if (baseResult is Ok && baseResult.value != null) {
      return when (val opResult = parseOp()) {
        is Ok -> if (opResult.value != null) {
          Ok(baseResult.value!! to opResult.value!!)
        } else {
          Ok(baseResult.value!! to emptyList())
        }
        is Err -> opResult
      }
    } else if (baseResult is Err) {
      return baseResult
    }

    when (val opResult = parseOp()) {
      is Ok -> if (opResult.value != null) {
        return Ok(timeBase to opResult.value!!)
      }
      is Err -> return opResult
    }

    val nextOrLastResult = parseNextOrLast()
    if (nextOrLastResult != null) {
      return when (val offsetResult = offset()) {
        is Ok -> {
          val adj = mutableListOf<Adjustment<TimeOffsetType>>()
          adj.add(Adjustment(offsetResult.value.first, offsetResult.value.second, nextOrLastResult))
          when (val opResult = parseOp()) {
            is Ok -> if (opResult.value != null) {
              adj.addAll(opResult.value!!)
              Ok(timeBase to adj)
            } else {
              Ok(timeBase to adj)
            }
            is Err -> opResult
          }
        }
        is Err -> offsetResult
      }
    }

    return if (lexer.empty) {
      Ok(timeBase to emptyList())
    } else {
      Err("Unexpected characters '${lexer.remainder}' at index ${lexer.index}")
    }
  }

  @Suppress("ReturnCount")
  private fun parseOp(): Result<List<Adjustment<TimeOffsetType>>?, String> {
    val adj = mutableListOf<Adjustment<TimeOffsetType>>()
    var opResult = op()
    if (opResult != null) {
      while (opResult != null) {
        when (val durationResult = duration()) {
          is Ok -> adj.add(durationResult.value.withOperation(opResult))
          is Err -> return durationResult
        }
        opResult = op()
      }
      return Ok(adj)
    }
    return Ok(null)
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
        is Ok -> Ok(TimeBase.of(intValue, hourResult.value))
        is Err -> Err(hourResult.error)
      }
    } else {
      when {
        lexer.matchString("now") -> Ok(TimeBase.Now)
        lexer.matchString("midnight") -> Ok(TimeBase.Midnight)
        lexer.matchString("noon") -> Ok(TimeBase.Noon)
        else -> Ok(null)
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
        lexer.matchString("am") -> Ok(ClockHour.AM)
        lexer.matchString("pm") -> Ok(ClockHour.PM)
        else -> Ok(ClockHour.NEXT)
      }
    } else {
      Err("Was expecting a clock hour at index ${lexer.index}")
    }
  }

  //duration returns [ Adjustment<TimeOffsetType> d ] : INT durationType { $d = new Adjustment<TimeOffsetType>($durationType.type, $INT.int); } ;
  fun duration(): Result<Adjustment<TimeOffsetType>, String> {
    lexer.skipWhitespace()

    val intResult = when (val result = lexer.parseInt()) {
      is Ok -> result.value
      is Err -> return result
    }

    val durationTypeResult = durationType()
    return if (durationTypeResult != null) {
      Ok(Adjustment(durationTypeResult, intResult))
    } else {
      Err("Was expecting a duration type at index ${lexer.index}")
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
      lexer.matchString("hour") -> Ok(TimeOffsetType.HOUR to 1)
      lexer.matchString("minute") -> Ok(TimeOffsetType.MINUTE to 1)
      lexer.matchString("second") -> Ok(TimeOffsetType.SECOND to 1)
      lexer.matchString("millisecond") -> Ok(TimeOffsetType.MILLISECOND to 1)
      else -> Err("Was expecting an offset type at index ${lexer.index}")
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
