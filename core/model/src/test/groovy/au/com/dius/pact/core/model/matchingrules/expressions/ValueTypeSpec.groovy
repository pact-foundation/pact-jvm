package au.com.dius.pact.core.model.matchingrules.expressions

import spock.lang.Specification
import spock.lang.Unroll

class ValueTypeSpec extends Specification {
  @Unroll
  def 'merging types'() {
    expect:
    typeA.merge(typeB) == result

    where:

    typeA             | typeB             || result
    ValueType.String  | ValueType.Unknown || ValueType.String
    ValueType.Unknown | ValueType.String  || ValueType.String
    ValueType.Unknown | ValueType.Number  || ValueType.Number
    ValueType.Number  | ValueType.Unknown || ValueType.Number
    ValueType.Unknown | ValueType.Integer || ValueType.Integer
    ValueType.Integer | ValueType.Unknown || ValueType.Integer
    ValueType.Unknown | ValueType.Decimal || ValueType.Decimal
    ValueType.Decimal | ValueType.Unknown || ValueType.Decimal
    ValueType.Unknown | ValueType.Boolean || ValueType.Boolean
    ValueType.Boolean | ValueType.Unknown || ValueType.Boolean
    ValueType.Unknown | ValueType.Unknown || ValueType.Unknown
    ValueType.String  | ValueType.String  || ValueType.String
    ValueType.Number  | ValueType.Number  || ValueType.Number
    ValueType.Integer | ValueType.Integer || ValueType.Integer
    ValueType.Decimal | ValueType.Decimal || ValueType.Decimal
    ValueType.Boolean | ValueType.Boolean || ValueType.Boolean
    ValueType.Number  | ValueType.String  || ValueType.String
    ValueType.Integer | ValueType.String  || ValueType.String
    ValueType.Decimal | ValueType.String  || ValueType.String
    ValueType.Boolean | ValueType.String  || ValueType.String
    ValueType.String  | ValueType.Number  || ValueType.String
    ValueType.String  | ValueType.Integer || ValueType.String
    ValueType.String  | ValueType.Decimal || ValueType.String
    ValueType.String  | ValueType.Boolean || ValueType.String
    ValueType.Number  | ValueType.Integer || ValueType.Integer
    ValueType.Number  | ValueType.Decimal || ValueType.Decimal
    ValueType.Number  | ValueType.Boolean || ValueType.Number
    ValueType.Integer | ValueType.Number  || ValueType.Integer
    ValueType.Integer | ValueType.Decimal || ValueType.Decimal
    ValueType.Integer | ValueType.Boolean || ValueType.Integer
    ValueType.Decimal | ValueType.Number  || ValueType.Decimal
    ValueType.Decimal | ValueType.Integer || ValueType.Decimal
    ValueType.Decimal | ValueType.Boolean || ValueType.Decimal
    ValueType.Boolean | ValueType.Number  || ValueType.Number
    ValueType.Boolean | ValueType.Integer || ValueType.Integer
    ValueType.Boolean | ValueType.Decimal || ValueType.Decimal
  }
}
