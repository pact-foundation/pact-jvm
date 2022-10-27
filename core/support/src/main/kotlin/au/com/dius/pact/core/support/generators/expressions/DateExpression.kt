package au.com.dius.pact.core.support.generators.expressions

import au.com.dius.pact.core.support.parsers.StringLexer
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class DateExpressionLexer(expression: String): StringLexer(expression) {
  fun parseDateBase(): DateBase? {
    return when {
      matchString("now") -> DateBase.NOW
      matchString("today") -> DateBase.TODAY
      matchString("yesterday") -> DateBase.YESTERDAY
      matchString("tomorrow") -> DateBase.TOMORROW
      else -> null
    }
  }

  fun parseOperation(): Operation? {
    return when {
      matchChar('+') -> Operation.PLUS
      matchChar('-') -> Operation.MINUS
      else -> null
    }
  }

  fun parseDateOffsetType(): DateOffsetType? {
    return when {
      matchRegex(DAYS) != null -> DateOffsetType.DAY
      matchRegex(WEEKS) != null -> DateOffsetType.WEEK
      matchRegex(MONTHS) != null -> DateOffsetType.MONTH
      matchRegex(YEARS) != null -> DateOffsetType.YEAR
      else -> null
    }
  }

  companion object {
    val DAYS = Regex("^days?")
    val WEEKS = Regex("^weeks?")
    val MONTHS = Regex("^months?")
    val YEARS = Regex("^years?")
  }
}

@Suppress("MaxLineLength")
class DateExpressionParser(private val lexer: DateExpressionLexer) {
  //expression returns [ DateBase dateBase = DateBase.NOW, List<Adjustment<DateOffsetType>> adj = new ArrayList<>() ] : ( base { $dateBase = $base.t; }
  //    | op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
  //    | base { $dateBase = $base.t; } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
  //    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }
  //    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }  ( op duration {
  //        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
  //    } )*
  //    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); }
  //    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); } ( op duration {
  //        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
  //    } )*
  //    ) EOF
  //    ;
  @Suppress("ComplexMethod", "ReturnCount")
  fun expression(): Result<Pair<DateBase, List<Adjustment<DateOffsetType>>>, String> {
    val dateBase = DateBase.NOW

    val baseResult = base()
    if (baseResult != null) {
      return when (val opResult = parseOp()) {
        is Ok -> if (opResult.value != null) {
          Ok(baseResult to opResult.value!!)
        } else {
          Ok(baseResult to emptyList())
        }
        is Err -> opResult
      }
    }

    when (val opResult = parseOp()) {
      is Ok -> if (opResult.value != null) {
        return Ok(dateBase to opResult.value!!)
      }
      is Err -> return opResult
    }

    val nextOrLastResult = parseNextOrLast()
    if (nextOrLastResult != null) {
      return when (val offsetResult = offset()) {
        is Ok -> {
          val adj = mutableListOf<Adjustment<DateOffsetType>>()
          adj.add(Adjustment(offsetResult.value.first, offsetResult.value.second, nextOrLastResult))
          when (val opResult = parseOp()) {
            is Ok -> if (opResult.value != null) {
              adj.addAll(opResult.value!!)
              Ok(dateBase to adj)
            } else {
              Ok(dateBase to adj)
            }
            is Err -> opResult
          }
        }
        is Err -> offsetResult
      }
    }

    return if (lexer.empty) {
      Ok(dateBase to emptyList())
    } else {
      Err("Error parsing expression: Unexpected characters '${lexer.remainder}' at ${lexer.index}")
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

  @Suppress("ReturnCount")
  private fun parseOp(): Result<List<Adjustment<DateOffsetType>>?, String> {
    val adj = mutableListOf<Adjustment<DateOffsetType>>()
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

  //base returns [ DateBase t ] : 'now' { $t = DateBase.NOW; }
  //    | 'today' { $t = DateBase.TODAY; }
  //    | 'yesterday' { $t = DateBase.YESTERDAY; }
  //    | 'tomorrow' { $t = DateBase.TOMORROW; }
  //    ;
  fun base(): DateBase? {
    lexer.skipWhitespace()
    return lexer.parseDateBase()
  }

  //duration returns [ Adjustment<DateOffsetType> d ] : INT durationType { $d = new Adjustment<DateOffsetType>($durationType.type, $INT.int); } ;
  fun duration(): Result<Adjustment<DateOffsetType>, String> {
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

  //durationType returns [ DateOffsetType type ] : 'day' { $type = DateOffsetType.DAY; }
  //    | DAYS { $type = DateOffsetType.DAY; }
  //    | 'week' { $type = DateOffsetType.WEEK; }
  //    | WEEKS { $type = DateOffsetType.WEEK; }
  //    | 'month' { $type = DateOffsetType.MONTH; }
  //    | MONTHS { $type = DateOffsetType.MONTH; }
  //    | 'year' { $type = DateOffsetType.YEAR; }
  //    | YEARS { $type = DateOffsetType.YEAR; }
  //    ;
  fun durationType(): DateOffsetType? {
    lexer.skipWhitespace()
    return lexer.parseDateOffsetType()
  }

  //op returns [ Operation o ] : '+' { $o = Operation.PLUS; }
  //    | '-' { $o = Operation.MINUS; }
  fun op(): Operation? {
    lexer.skipWhitespace()
    return lexer.parseOperation()
  }

  //offset returns [ DateOffsetType type, int val = 1 ] : 'day' { $type = DateOffsetType.DAY; }
  //    | 'week' { $type = DateOffsetType.WEEK; }
  //    | 'month' { $type = DateOffsetType.MONTH; }
  //    | 'year' { $type = DateOffsetType.YEAR; }
  //    | 'fortnight' { $type = DateOffsetType.WEEK; $val = 2; }
  //    | 'monday' { $type = DateOffsetType.MONDAY; }
  //    | 'mon' { $type = DateOffsetType.MONDAY; }
  //    | 'tuesday' { $type = DateOffsetType.TUESDAY; }
  //    | 'tues' { $type = DateOffsetType.TUESDAY; }
  //    | 'wednesday' { $type = DateOffsetType.WEDNESDAY; }
  //    | 'wed' { $type = DateOffsetType.WEDNESDAY; }
  //    | 'thursday' { $type = DateOffsetType.THURSDAY; }
  //    | 'thurs' { $type = DateOffsetType.THURSDAY; }
  //    | 'friday' { $type = DateOffsetType.FRIDAY; }
  //    | 'fri' { $type = DateOffsetType.FRIDAY; }
  //    | 'saturday' { $type = DateOffsetType.SATURDAY; }
  //    | 'sat' { $type = DateOffsetType.SATURDAY; }
  //    | 'sunday' { $type = DateOffsetType.SUNDAY; }
  //    | 'sun' { $type = DateOffsetType.SUNDAY; }
  //    | 'january' { $type = DateOffsetType.JAN; }
  //    | 'jan' { $type = DateOffsetType.JAN; }
  //    | 'february' { $type = DateOffsetType.FEB; }
  //    | 'feb' { $type = DateOffsetType.FEB; }
  //    | 'march' { $type = DateOffsetType.MAR; }
  //    | 'mar' { $type = DateOffsetType.MAR; }
  //    | 'april' { $type = DateOffsetType.APR; }
  //    | 'apr' { $type = DateOffsetType.APR; }
  //    | 'may' { $type = DateOffsetType.MAY; }
  //    | 'june' { $type = DateOffsetType.JUNE; }
  //    | 'jun' { $type = DateOffsetType.JUNE; }
  //    | 'july' { $type = DateOffsetType.JULY; }
  //    | 'jul' { $type = DateOffsetType.JULY; }
  //    | 'august' { $type = DateOffsetType.AUG; }
  //    | 'aug' { $type = DateOffsetType.AUG; }
  //    | 'september' { $type = DateOffsetType.SEP; }
  //    | 'sep' { $type = DateOffsetType.SEP; }
  //    | 'october' { $type = DateOffsetType.OCT; }
  //    | 'oct' { $type = DateOffsetType.OCT; }
  //    | 'november' { $type = DateOffsetType.NOV; }
  //    | 'nov' { $type = DateOffsetType.NOV; }
  //    | 'december' { $type = DateOffsetType.DEC; }
  //    | 'dec' { $type = DateOffsetType.DEC; }
  //    ;
  @Suppress("ComplexMethod")
  fun offset(): Result<Pair<DateOffsetType, Int>, String> {
    lexer.skipWhitespace()
    return when {
      lexer.matchString("day") -> Ok(DateOffsetType.DAY to 1)
      lexer.matchString("week") -> Ok(DateOffsetType.WEEK to 1)
      lexer.matchString("month") -> Ok(DateOffsetType.MONTH to 1)
      lexer.matchString("year") -> Ok(DateOffsetType.YEAR to 1)
      lexer.matchString("fortnight") -> Ok(DateOffsetType.WEEK to 2)
      lexer.matchString("monday") -> Ok(DateOffsetType.MONDAY to 1)
      lexer.matchString("mon") -> Ok(DateOffsetType.MONDAY to 1)
      lexer.matchString("tuesday") -> Ok(DateOffsetType.TUESDAY to 1)
      lexer.matchString("tues") -> Ok(DateOffsetType.TUESDAY to 1)
      lexer.matchString("wednesday") -> Ok(DateOffsetType.WEDNESDAY to 1)
      lexer.matchString("wed") -> Ok(DateOffsetType.WEDNESDAY to 1)
      lexer.matchString("thursday") -> Ok(DateOffsetType.THURSDAY to 1)
      lexer.matchString("thurs") -> Ok(DateOffsetType.THURSDAY to 1)
      lexer.matchString("friday") -> Ok(DateOffsetType.FRIDAY to 1)
      lexer.matchString("fri") -> Ok(DateOffsetType.FRIDAY to 1)
      lexer.matchString("saturday") -> Ok(DateOffsetType.SATURDAY to 1)
      lexer.matchString("sat") -> Ok(DateOffsetType.SATURDAY to 1)
      lexer.matchString("sunday") -> Ok(DateOffsetType.SUNDAY to 1)
      lexer.matchString("sun") -> Ok(DateOffsetType.SUNDAY to 1)
      lexer.matchString("january") -> Ok(DateOffsetType.JAN to 1)
      lexer.matchString("jan") -> Ok(DateOffsetType.JAN to 1)
      lexer.matchString("february") -> Ok(DateOffsetType.FEB to 1)
      lexer.matchString("feb") -> Ok(DateOffsetType.FEB to 1)
      lexer.matchString("march") -> Ok(DateOffsetType.MAR to 1)
      lexer.matchString("mar") -> Ok(DateOffsetType.MAR to 1)
      lexer.matchString("april") -> Ok(DateOffsetType.APR to 1)
      lexer.matchString("apr") -> Ok(DateOffsetType.APR to 1)
      lexer.matchString("may") -> Ok(DateOffsetType.MAY to 1)
      lexer.matchString("june") -> Ok(DateOffsetType.JUNE to 1)
      lexer.matchString("jun") -> Ok(DateOffsetType.JUNE to 1)
      lexer.matchString("july") -> Ok(DateOffsetType.JULY to 1)
      lexer.matchString("jul") -> Ok(DateOffsetType.JULY to 1)
      lexer.matchString("august") -> Ok(DateOffsetType.AUG to 1)
      lexer.matchString("aug") -> Ok(DateOffsetType.AUG to 1)
      lexer.matchString("september") -> Ok(DateOffsetType.SEP to 1)
      lexer.matchString("sep") -> Ok(DateOffsetType.SEP to 1)
      lexer.matchString("october") -> Ok(DateOffsetType.OCT to 1)
      lexer.matchString("oct") -> Ok(DateOffsetType.OCT to 1)
      lexer.matchString("november") -> Ok(DateOffsetType.NOV to 1)
      lexer.matchString("nov") -> Ok(DateOffsetType.NOV to 1)
      lexer.matchString("december") -> Ok(DateOffsetType.DEC to 1)
      lexer.matchString("dec") -> Ok(DateOffsetType.DEC to 1)
      else -> Err("Was expecting an offset type at index ${lexer.index}")
    }
  }
}
