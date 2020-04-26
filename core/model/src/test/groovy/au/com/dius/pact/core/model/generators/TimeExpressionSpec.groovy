package au.com.dius.pact.model.generators

import au.com.dius.pact.core.model.generators.TimeExpression
import spock.lang.Specification
import spock.lang.Unroll

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimeExpressionSpec extends Specification {

  private time

  def setup() {
    time = OffsetDateTime.of(2000, 4, 3, 10, 0, 0, 0, ZoneOffset.UTC)
  }

  @Unroll
  def 'time expression test - #expression'() {
    expect:
    TimeExpression.INSTANCE.executeTimeExpression(time, expression).value
      .format(DateTimeFormatter.ISO_LOCAL_TIME) == expected

    where:

    expression                         | expected
    ''                                 | '10:00:00'
    'now'                              | '10:00:00'
    'midnight'                         | '00:00:00'
    'noon'                             | '12:00:00'
    '1 o\'clock'                       | '13:00:00'
    '1 o\'clock am'                    | '01:00:00'
    '1 o\'clock pm'                    | '13:00:00'
    '+ 1 hour'                         | '11:00:00'
    '- 2 minutes'                      | '09:58:00'
    '+ 4 seconds'                      | '10:00:04'
    '+ 4 milliseconds'                 | '10:00:00.004'
    'midnight+ 4 minutes'              | '00:04:00'
    'next hour'                        | '11:00:00'
    'last minute'                      | '09:59:00'
    'now + 2 hours - 4 minutes'        | '11:56:00'
    ' + 2 hours - 4 minutes'           | '11:56:00'
  }

  @Unroll
  def 'time expression error test'() {
    expect:
    TimeExpression.INSTANCE.executeTimeExpression(time, expression).error ==~ expected

    where:

    expression | expected
    '+'        | 'Error parsing expression: line 1:1 mismatched input \'<EOF>\' expecting INT'
    'now +'    | 'Error parsing expression: line 1:5 mismatched input \'<EOF>\' expecting INT'
    'noo'      | /^Error parsing expression.*/
  }
}
