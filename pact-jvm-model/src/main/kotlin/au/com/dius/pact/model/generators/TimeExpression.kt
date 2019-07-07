package au.com.dius.pact.model.generators

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.com.github.michaelbull.result.Ok
import au.com.dius.pact.com.github.michaelbull.result.Result
import au.com.dius.pact.support.generators.expressions.Adjustment
import au.com.dius.pact.support.generators.expressions.Operation
import au.com.dius.pact.support.generators.expressions.TimeBase
import au.com.dius.pact.support.generators.expressions.TimeExpressionLexer
import au.com.dius.pact.support.generators.expressions.TimeExpressionParser
import au.com.dius.pact.support.generators.expressions.TimeOffsetType
import mu.KLogging
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class ParsedTimeExpression(val base: TimeBase, val adjustments: MutableList<Adjustment<TimeOffsetType>>)

object TimeExpression : KLogging() {
  fun executeTimeExpression(base: OffsetTime, expression: String?): Result<OffsetTime, String> {
    return if (!expression.isNullOrEmpty()) {
      return when (val result = parseTimeExpression(expression)) {
        is Err -> result
        is Ok -> {
          val midnight = OffsetTime.of(LocalTime.MIDNIGHT, ZoneOffset.from(base))
          val noon = OffsetTime.of(LocalTime.NOON, ZoneOffset.from(base))
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
    val charStream = CharStreams.fromString(expression)
    val lexer = TimeExpressionLexer(charStream)
    val tokens = CommonTokenStream(lexer)
    val parser = TimeExpressionParser(tokens)
    val errorListener = ErrorListener()
    parser.addErrorListener(errorListener)
    val result = parser.expression()
    return if (errorListener.errors.isNotEmpty()) {
      Err("Error parsing expression: ${errorListener.errors.joinToString(", ")}")
    } else {
      Ok(ParsedTimeExpression(result.timeBase, result.adj))
    }
  }
}
