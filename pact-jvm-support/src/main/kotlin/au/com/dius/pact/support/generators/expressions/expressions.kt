package au.com.dius.pact.support.generators.expressions

enum class DateBase {
  NOW, TODAY, YESTERDAY, TOMORROW
}

sealed class TimeBase {
  object Now: TimeBase()
  object Midnight: TimeBase()
  object Noon: TimeBase()
  data class Am(val hour: Int): TimeBase()
  data class Pm(val hour: Int): TimeBase()
  data class Next(val hour: Int): TimeBase()

  companion object {
    @JvmStatic
    fun of(hour: Int, ch: ClockHour): TimeBase {
      return when (ch) {
        ClockHour.AM -> when (hour) {
          in 1..12 -> Am(hour)
          else -> throw IllegalArgumentException("$hour is an invalid hour of the day")
        }
        ClockHour.PM -> when (hour) {
          in 1..12 -> Pm(hour)
          else -> throw IllegalArgumentException("$hour is an invalid hour of the day")
        }
        ClockHour.NEXT -> when (hour) {
          in 1..12 -> Next(hour)
          else -> throw IllegalArgumentException("$hour is an invalid hour of the day")
        }
      }
    }
  }
}

enum class ClockHour {
  AM, PM, NEXT
}

enum class Operation {
  PLUS, MINUS
}

enum class DateOffsetType {
  DAY, WEEK, MONTH, YEAR, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, JAN, FEB, MAR, APR, MAY,
  JUNE, JULY, AUG, SEP, OCT, NOV, DEC
}

enum class TimeOffsetType {
  HOUR, MINUTE, SECOND, MILLISECOND
}

data class Adjustment<T> @JvmOverloads constructor (
  val type: T,
  val value: Int,
  val operation: Operation = Operation.PLUS
) {

  fun withOperation(operation: Operation) = this.copy(operation = operation)
}
