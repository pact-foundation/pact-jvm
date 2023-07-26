package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.mapError
import io.github.oshai.kotlinlogging.KLogging
import java.lang.Integer.parseInt
import java.time.OffsetDateTime

object DateTimeExpression : KLogging() {
  fun executeExpression(base: OffsetDateTime, expression: String?): Result<OffsetDateTime, String> {
    return if (!expression.isNullOrEmpty()) {
      val split = expression.split("@", limit = 2)
      if (split.size > 1) {
        val datePart = DateExpression.executeDateExpression(base, split[0])
        val timePart = if (datePart is Result.Ok<OffsetDateTime>)
          TimeExpression.executeTimeExpression(datePart.value, split[1])
        else
          TimeExpression.executeTimeExpression(base, split[1])
        when {
          datePart is Result.Err<String> && timePart is Result.Err<String> -> datePart.mapError { "$it, " +
            Regex("index (\\d+)").replace(timePart.error) { mr ->
              val pos = parseInt(mr.groupValues[1])
              "index ${pos + split[0].length + 1}"
            }
          }
          datePart is Result.Err<String> -> datePart
          timePart is Result.Err<String> -> timePart.mapError {
            Regex("index (\\d+)").replace(timePart.error) { mr ->
              val pos = parseInt(mr.groupValues[1])
              "index ${pos + split[0].length + 1}"
            }
          }
          else -> timePart
        }
      } else {
        DateExpression.executeDateExpression(base, split[0])
      }
    } else {
      Result.Ok(base)
    }
  }
}
