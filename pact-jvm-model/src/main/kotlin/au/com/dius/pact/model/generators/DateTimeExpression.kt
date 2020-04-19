package au.com.dius.pact.model.generators

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import mu.KLogging
import java.lang.Integer.parseInt
import java.time.OffsetDateTime

object DateTimeExpression : KLogging() {
  fun executeExpression(base: OffsetDateTime, expression: String?): Result<OffsetDateTime, String> {
    return if (!expression.isNullOrEmpty()) {
      val split = expression.split("@", limit = 2)
      if (split.size > 1) {
        val datePart = DateExpression.executeDateExpression(base, split[0])
        val timePart = if (datePart is Ok<OffsetDateTime>)
          TimeExpression.executeTimeExpression(datePart.value, split[1])
        else
          TimeExpression.executeTimeExpression(base, split[1])
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
          else -> timePart
        }
      } else {
        DateExpression.executeDateExpression(base, split[0])
      }
    } else {
      Ok(base)
    }
  }
}
