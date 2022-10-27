package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.generators.expressions.Adjustment
import au.com.dius.pact.core.support.generators.expressions.Operation
import au.com.dius.pact.core.support.generators.expressions.TimeBase
import au.com.dius.pact.core.support.generators.expressions.TimeExpressionLexer
import au.com.dius.pact.core.support.generators.expressions.TimeExpressionParser
import au.com.dius.pact.core.support.generators.expressions.TimeOffsetType
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class ParsedTimeExpression(val base: TimeBase, val adjustments: MutableList<Adjustment<TimeOffsetType>>)

object TimeExpression : KLogging() {
  fun executeTimeExpression(base: OffsetDateTime, expression: String?): Result<OffsetDateTime, String> {
    return if (!expression.isNullOrEmpty()) {
      return when (val result = parseTimeExpression(expression)) {
        is Err -> result
        is Ok -> {
          val midnight = OffsetDateTime.of(base.toLocalDate(), LocalTime.MIDNIGHT, ZoneOffset.from(base))
          val noon = OffsetDateTime.of(base.toLocalDate(), LocalTime.NOON, ZoneOffset.from(base))
          var time = when (val valBase = result.value.base) {
            TimeBase.Now -> base
            TimeBase.Midnight -> midnight
            TimeBase.Noon -> noon
            is TimeBase.Am -> midnight.plusHours(valBase.hour.toLong())
            is TimeBase.Pm -> noon.plusHours(valBase.hour.toLong())
            is TimeBase.Next -> if (base.isBefore(noon))
              noon.plusHours(valBase.hour.toLong())
              else midnight.plusHours(valBase.hour.toLong())
          }

          result.value.adjustments.forEach {
            when (it.operation) {
              Operation.PLUS -> {
                time = when (it.type) {
                  TimeOffsetType.HOUR -> time.plusHours(it.value.toLong())
                  TimeOffsetType.MINUTE -> time.plusMinutes(it.value.toLong())
                  TimeOffsetType.SECOND -> time.plusSeconds(it.value.toLong())
                  TimeOffsetType.MILLISECOND -> time.plus(it.value.toLong(), ChronoUnit.MILLIS)
                }
              }
              Operation.MINUS -> {
                time = when (it.type) {
                  TimeOffsetType.HOUR -> time.minusHours(it.value.toLong())
                  TimeOffsetType.MINUTE -> time.minusMinutes(it.value.toLong())
                  TimeOffsetType.SECOND -> time.minusSeconds(it.value.toLong())
                  TimeOffsetType.MILLISECOND -> time.minus(it.value.toLong(), ChronoUnit.MILLIS)
                }
              }
            }
          }

          Ok(time)
        }
      }
    } else {
      Ok(base)
    }
  }

  private fun parseTimeExpression(expression: String): Result<ParsedTimeExpression, String> {
    val lexer = TimeExpressionLexer(expression)
    val parser = TimeExpressionParser(lexer)
    return when (val result = parser.expression()) {
      is Err -> Err("Error parsing expression: ${result.error}")
      is Ok -> Ok(ParsedTimeExpression(result.value.first, result.value.second.toMutableList()))
    }
  }
}
