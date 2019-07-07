package au.com.dius.pact.model.generators

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.com.github.michaelbull.result.Ok
import au.com.dius.pact.com.github.michaelbull.result.Result
import au.com.dius.pact.com.github.michaelbull.result.get
import au.com.dius.pact.com.github.michaelbull.result.map
import au.com.dius.pact.com.github.michaelbull.result.mapError
import mu.KLogging
import java.lang.Integer.parseInt
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object DateTimeExpression : KLogging() {
  fun executeExpression(base: OffsetDateTime, expression: String?): Result<OffsetDateTime, String> {
    return if (!expression.isNullOrEmpty()) {
      val split = expression.split("@", limit = 2)
      if (split.size > 1) {
        val datePart = DateExpression.executeDateExpression(base, split[0])
        val timePart = TimeExpression.executeTimeExpression(base.toOffsetTime(), split[1])
        when {
          datePart is Err<String> && timePart is Err<String> -> datePart.mapError { "$it, " +
            Regex("1:(\\d+)").replace(timePart.error) { mr ->
              val pos = parseInt(mr.groupValues[1])
              "1:${pos + split[0].length + 1}"
            }
          }
          datePart is Err<String> -> datePart
          timePart is Err<String> -> timePart.mapError {
            Regex("1:(\\d+)").replace(timePart.error) { mr ->
              val pos = parseInt(mr.groupValues[1])
              "1:${pos + split[0].length + 1}"
            }
          }
          else -> datePart.map { it.truncatedTo(ChronoUnit.DAYS).with(timePart.get()!!.toLocalTime()) }
        }
      } else {
        DateExpression.executeDateExpression(base, split[0])
      }
    } else {
      Ok(base)
    }
  }
}
