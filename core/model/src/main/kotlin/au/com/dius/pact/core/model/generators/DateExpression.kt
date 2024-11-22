package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.generators.expressions.Adjustment
import au.com.dius.pact.core.support.generators.expressions.DateBase
import au.com.dius.pact.core.support.generators.expressions.DateExpressionLexer
import au.com.dius.pact.core.support.generators.expressions.DateExpressionParser
import au.com.dius.pact.core.support.generators.expressions.DateOffsetType
import au.com.dius.pact.core.support.generators.expressions.Operation
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.DayOfWeek
import java.time.Month
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

data class ParsedDateExpression(val base: DateBase, val adjustments: MutableList<Adjustment<DateOffsetType>>)

object DateExpression {
  @Suppress("NestedBlockDepth")
  fun executeDateExpression(base: OffsetDateTime, expression: String?): Result<OffsetDateTime, String> {
    return if (!expression.isNullOrEmpty()) {
      return when (val result = parseDateExpression(expression)) {
        is Result.Err -> result
        is Result.Ok -> {
          var date = baseDate(result, base)
          result.value.adjustments.forEach {
            date = when (it.operation) {
              Operation.PLUS -> forwardDateBy(it, date)
              Operation.MINUS -> reverseDateBy(it, date)
            }
          }

          Result.Ok(date)
        }
      }
    } else {
      Result.Ok(base)
    }
  }

  @Suppress("ComplexMethod")
  private fun reverseDateBy(it: Adjustment<DateOffsetType>, date: OffsetDateTime): OffsetDateTime {
    return when (it.type) {
      DateOffsetType.DAY -> date.minusDays(it.value.toLong())
      DateOffsetType.WEEK -> date.minus(it.value.toLong(), ChronoUnit.WEEKS)
      DateOffsetType.MONTH -> date.minus(it.value.toLong(), ChronoUnit.MONTHS)
      DateOffsetType.YEAR -> date.minus(it.value.toLong(), ChronoUnit.YEARS)
      DateOffsetType.MONDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.MONDAY }
      DateOffsetType.TUESDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.TUESDAY }
      DateOffsetType.WEDNESDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.WEDNESDAY }
      DateOffsetType.THURSDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.THURSDAY }
      DateOffsetType.FRIDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.FRIDAY }
      DateOffsetType.SATURDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.SATURDAY }
      DateOffsetType.SUNDAY -> adjustDownTo(date) { d -> d.dayOfWeek == DayOfWeek.SUNDAY }
      DateOffsetType.JAN -> adjustMonthDownTo(date, Month.JANUARY)
      DateOffsetType.FEB -> adjustMonthDownTo(date, Month.FEBRUARY)
      DateOffsetType.MAR -> adjustMonthDownTo(date, Month.MARCH)
      DateOffsetType.APR -> adjustMonthDownTo(date, Month.APRIL)
      DateOffsetType.MAY -> adjustMonthDownTo(date, Month.MAY)
      DateOffsetType.JUNE -> adjustMonthDownTo(date, Month.JUNE)
      DateOffsetType.JULY -> adjustMonthDownTo(date, Month.JULY)
      DateOffsetType.AUG -> adjustMonthDownTo(date, Month.AUGUST)
      DateOffsetType.SEP -> adjustMonthDownTo(date, Month.SEPTEMBER)
      DateOffsetType.OCT -> adjustMonthDownTo(date, Month.OCTOBER)
      DateOffsetType.NOV -> adjustMonthDownTo(date, Month.NOVEMBER)
      DateOffsetType.DEC -> adjustMonthDownTo(date, Month.DECEMBER)
    }
  }

  @Suppress("ComplexMethod")
  private fun forwardDateBy(it: Adjustment<DateOffsetType>, date: OffsetDateTime): OffsetDateTime {
    return when (it.type) {
      DateOffsetType.DAY -> date.plusDays(it.value.toLong())
      DateOffsetType.WEEK -> date.plus(it.value.toLong(), ChronoUnit.WEEKS)
      DateOffsetType.MONTH -> date.plus(it.value.toLong(), ChronoUnit.MONTHS)
      DateOffsetType.YEAR -> date.plus(it.value.toLong(), ChronoUnit.YEARS)
      DateOffsetType.MONDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.MONDAY }
      DateOffsetType.TUESDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.TUESDAY }
      DateOffsetType.WEDNESDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.WEDNESDAY }
      DateOffsetType.THURSDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.THURSDAY }
      DateOffsetType.FRIDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.FRIDAY }
      DateOffsetType.SATURDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.SATURDAY }
      DateOffsetType.SUNDAY -> adjustUpTo(date) { d -> d.dayOfWeek == DayOfWeek.SUNDAY }
      DateOffsetType.JAN -> adjustMonthUpTo(date, Month.JANUARY)
      DateOffsetType.FEB -> adjustMonthUpTo(date, Month.FEBRUARY)
      DateOffsetType.MAR -> adjustMonthUpTo(date, Month.MARCH)
      DateOffsetType.APR -> adjustMonthUpTo(date, Month.APRIL)
      DateOffsetType.MAY -> adjustMonthUpTo(date, Month.MAY)
      DateOffsetType.JUNE -> adjustMonthUpTo(date, Month.JUNE)
      DateOffsetType.JULY -> adjustMonthUpTo(date, Month.JULY)
      DateOffsetType.AUG -> adjustMonthUpTo(date, Month.AUGUST)
      DateOffsetType.SEP -> adjustMonthUpTo(date, Month.SEPTEMBER)
      DateOffsetType.OCT -> adjustMonthUpTo(date, Month.OCTOBER)
      DateOffsetType.NOV -> adjustMonthUpTo(date, Month.NOVEMBER)
      DateOffsetType.DEC -> adjustMonthUpTo(date, Month.DECEMBER)
    }
  }

  private fun baseDate(result: Result.Ok<ParsedDateExpression>, base: OffsetDateTime): OffsetDateTime {
    return when (result.value.base) {
      DateBase.NOW, DateBase.TODAY -> base
      DateBase.YESTERDAY -> base.minusDays(1)
      DateBase.TOMORROW -> base.plusDays(1)
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

  private fun parseDateExpression(expression: String): Result<ParsedDateExpression, String> {
    val lexer = DateExpressionLexer(expression)
    val parser = DateExpressionParser(lexer)
    return when (val result = parser.expression()) {
      is Result.Err -> Result.Err("Error parsing expression: ${result.error}")
      is Result.Ok -> Result.Ok(ParsedDateExpression(result.value.first, result.value.second.toMutableList()))
    }
  }
}
