package au.com.dius.pact.model.generators

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.com.github.michaelbull.result.Ok
import au.com.dius.pact.com.github.michaelbull.result.Result
import au.com.dius.pact.support.generators.expressions.DateBase
import au.com.dius.pact.support.generators.expressions.DateExpressionLexer
import au.com.dius.pact.support.generators.expressions.DateExpressionParser
import au.com.dius.pact.support.generators.expressions.Adjustment
import au.com.dius.pact.support.generators.expressions.OffsetType
import au.com.dius.pact.support.generators.expressions.Operation
import mu.KLogging
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import java.time.DayOfWeek
import java.time.Month
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

data class ParsedExpression(val base: DateBase, val adjustments: MutableList<Adjustment>)

class ErrorListener(val errors: MutableList<String> = mutableListOf()) : BaseErrorListener() {
  override fun syntaxError(
    recognizer: Recognizer<*, *>,
    offendingSymbol: Any,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
    errors.add("line $line:$charPositionInLine $msg")
  }
}

object DateExpression : KLogging() {
  fun executeDateExpression(base: OffsetDateTime, expression: String?): Result<OffsetDateTime, String> {
    return if (!expression.isNullOrEmpty()) {
      val result = parseDateExpression(expression)
      return when (result) {
        is Err -> result
        is Ok -> {
          var date = when (result.value.base) {
            DateBase.NOW, DateBase.TODAY -> base
            DateBase.YESTERDAY -> base.minusDays(1)
            DateBase.TOMORROW -> base.plusDays(1)
          }

          result.value.adjustments.forEach {
            when (it.operation) {
              Operation.PLUS -> {
                date = when (it.type) {
                  OffsetType.DAY -> date.plusDays(it.value.toLong())
                  OffsetType.WEEK -> date.plus(it.value.toLong(), ChronoUnit.WEEKS)
                  OffsetType.MONTH -> date.plus(it.value.toLong(), ChronoUnit.MONTHS)
                  OffsetType.YEAR -> date.plus(it.value.toLong(), ChronoUnit.YEARS)
                  OffsetType.MONDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.MONDAY }
                  OffsetType.TUESDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.TUESDAY }
                  OffsetType.WEDNESDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.WEDNESDAY }
                  OffsetType.THURSDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.THURSDAY }
                  OffsetType.FRIDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.FRIDAY }
                  OffsetType.SATURDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.SATURDAY }
                  OffsetType.SUNDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.SUNDAY }
                  OffsetType.JAN -> adjustMonthUpTo(date, Month.JANUARY)
                  OffsetType.FEB -> adjustMonthUpTo(date, Month.FEBRUARY)
                  OffsetType.MAR -> adjustMonthUpTo(date, Month.MARCH)
                  OffsetType.APR -> adjustMonthUpTo(date, Month.APRIL)
                  OffsetType.MAY -> adjustMonthUpTo(date, Month.MAY)
                  OffsetType.JUNE -> adjustMonthUpTo(date, Month.JUNE)
                  OffsetType.JULY -> adjustMonthUpTo(date, Month.JULY)
                  OffsetType.AUG -> adjustMonthUpTo(date, Month.AUGUST)
                  OffsetType.SEP -> adjustMonthUpTo(date, Month.SEPTEMBER)
                  OffsetType.OCT -> adjustMonthUpTo(date, Month.OCTOBER)
                  OffsetType.NOV -> adjustMonthUpTo(date, Month.NOVEMBER)
                  OffsetType.DEC -> adjustMonthUpTo(date, Month.DECEMBER)
                }
              }
              Operation.MINUS -> {
                date = when (it.type) {
                  OffsetType.DAY -> date.minusDays(it.value.toLong())
                  OffsetType.WEEK -> date.minus(it.value.toLong(), ChronoUnit.WEEKS)
                  OffsetType.MONTH -> date.minus(it.value.toLong(), ChronoUnit.MONTHS)
                  OffsetType.YEAR -> date.minus(it.value.toLong(), ChronoUnit.YEARS)
                  OffsetType.MONDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.MONDAY }
                  OffsetType.TUESDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.TUESDAY }
                  OffsetType.WEDNESDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.WEDNESDAY }
                  OffsetType.THURSDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.THURSDAY }
                  OffsetType.FRIDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.FRIDAY }
                  OffsetType.SATURDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.SATURDAY }
                  OffsetType.SUNDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.SUNDAY }
                  OffsetType.JAN -> adjustMonthDownTo(date, Month.JANUARY)
                  OffsetType.FEB -> adjustMonthDownTo(date, Month.FEBRUARY)
                  OffsetType.MAR -> adjustMonthDownTo(date, Month.MARCH)
                  OffsetType.APR -> adjustMonthDownTo(date, Month.APRIL)
                  OffsetType.MAY -> adjustMonthDownTo(date, Month.MAY)
                  OffsetType.JUNE -> adjustMonthDownTo(date, Month.JUNE)
                  OffsetType.JULY -> adjustMonthDownTo(date, Month.JULY)
                  OffsetType.AUG -> adjustMonthDownTo(date, Month.AUGUST)
                  OffsetType.SEP -> adjustMonthDownTo(date, Month.SEPTEMBER)
                  OffsetType.OCT -> adjustMonthDownTo(date, Month.OCTOBER)
                  OffsetType.NOV -> adjustMonthDownTo(date, Month.NOVEMBER)
                  OffsetType.DEC -> adjustMonthDownTo(date, Month.DECEMBER)
                }
              }
            }
          }

          Ok(date)
        }
      }
    } else {
      Ok(base)
    }
  }

  private fun adjustMonthDownTo(date: OffsetDateTime, month: Month): OffsetDateTime {
    val d = date.minusMonths(1).withDayOfMonth(1)
    return adjustDownTo(d, OffsetDateTime::minusMonths) { it.month == month }
  }

  private fun adjustMonthUpTo(date: OffsetDateTime, month: Month): OffsetDateTime {
    val d = date.plusMonths(1).withDayOfMonth(1)
    return adjustUpTo(d, OffsetDateTime::plusMonths) { it.month == month }
  }

  private fun adjustUpTo(
    date: OffsetDateTime,
    adjuster: (OffsetDateTime, Long) -> OffsetDateTime = OffsetDateTime::plusDays,
    stopCondition: (OffsetDateTime) -> Boolean
  ) = adjustDateTime(date, stopCondition, adjuster)

  private fun adjustDownTo(
    date: OffsetDateTime,
    adjuster: (OffsetDateTime, Long) -> OffsetDateTime = OffsetDateTime::minusDays,
    stopCondition: (OffsetDateTime) -> Boolean
  ) = adjustDateTime(date, stopCondition, adjuster)

  private fun adjustDateTime(
    date: OffsetDateTime,
    stopCondition: (OffsetDateTime) -> Boolean,
    adjuster: (OffsetDateTime, Long) -> OffsetDateTime
  ): OffsetDateTime {
    var result = date
    while (!stopCondition(result)) {
      result = adjuster(result, 1)
    }
    return result
  }

  private fun parseDateExpression(expression: String): Result<ParsedExpression, String> {
    val charStream = CharStreams.fromString(expression)
    val lexer = DateExpressionLexer(charStream)
    val tokens = CommonTokenStream(lexer)
    val parser = DateExpressionParser(tokens)
    val errorListener = ErrorListener()
    parser.addErrorListener(errorListener)
    val result = parser.expression()
    return if (errorListener.errors.isNotEmpty()) {
      Err("Error parsing expression: ${errorListener.errors.joinToString(", ")}")
    } else {
      Ok(ParsedExpression(result.dateBase, result.adj))
    }
  }
}
