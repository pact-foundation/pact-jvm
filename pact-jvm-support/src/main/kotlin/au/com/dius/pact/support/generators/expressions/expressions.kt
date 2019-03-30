package au.com.dius.pact.support.generators.expressions

enum class DateBase {
  NOW, TODAY, YESTERDAY, TOMORROW
}

enum class Operation {
  PLUS, MINUS
}

enum class OffsetType {
  DAY, WEEK, MONTH, YEAR, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, JAN, FEB, MAR, APR, MAY,
  JUNE, JULY, AUG, SEP, OCT, NOV, DEC
}

data class Adjustment @JvmOverloads constructor (
  val type: OffsetType,
  val value: Int,
  val operation: Operation = Operation.PLUS
) {

  fun withOperation(operation: Operation) = this.copy(operation = operation)
}
