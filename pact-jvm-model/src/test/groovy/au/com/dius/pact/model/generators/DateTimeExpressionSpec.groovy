package au.com.dius.pact.model.generators

import spock.lang.Specification
import spock.lang.Unroll

import java.time.OffsetDateTime
import java.time.ZoneOffset

@SuppressWarnings('LineLength')
class DateTimeExpressionSpec extends Specification {

    private dateTime

    def setup() {
        dateTime = OffsetDateTime.of(2000, 01, 01, 10, 0, 0, 0, ZoneOffset.UTC)
    }

    @Unroll
    def 'date expression test - #expression'() {
        expect:
        DateTimeExpression.INSTANCE.executeExpression(dateTime, expression).value.toString() == expected

        where:

        expression                         | expected
        ''                                 | '2000-01-01T10:00Z'
        'now'                              | '2000-01-01T10:00Z'
        'today'                            | '2000-01-01T10:00Z'
        'yesterday'                        | '1999-12-31T10:00Z'
        'tomorrow'                         | '2000-01-02T10:00Z'
        '+ 1 day'                          | '2000-01-02T10:00Z'
        '+ 1 week'                         | '2000-01-08T10:00Z'
        '- 2 weeks'                        | '1999-12-18T10:00Z'
        '+ 4 years'                        | '2004-01-01T10:00Z'
        'tomorrow+ 4 years'                | '2004-01-02T10:00Z'
        'next week'                        | '2000-01-08T10:00Z'
        'last month'                       | '1999-12-01T10:00Z'
        'next fortnight'                   | '2000-01-15T10:00Z'
        'next monday'                      | '2000-01-03T10:00Z'
        'last wednesday'                   | '1999-12-29T10:00Z'
        'next mon'                         | '2000-01-03T10:00Z'
        'last december'                    | '1999-12-01T10:00Z'
        'next jan'                         | '2001-01-01T10:00Z'
        'next june + 2 weeks'              | '2000-06-15T10:00Z'
        'last mon + 2 weeks'               | '2000-01-10T10:00Z'
        '+ 1 day - 2 weeks'                | '1999-12-19T10:00Z'
        'last december + 2 weeks + 4 days' | '1999-12-19T10:00Z'
    }

    @Unroll
    def 'time expression test - #expression'() {
        expect:
        DateTimeExpression.INSTANCE.executeExpression(dateTime, expression).value.toString() == expected

        where:

        expression                    | expected
        '@ now'                       | '2000-01-01T10:00Z'
        '@ midnight'                  | '2000-01-01T00:00Z'
        '@ noon'                      | '2000-01-01T12:00Z'
        '@ 2 o\'clock'                | '2000-01-01T14:00Z'
        '@ 12 o\'clock am'            | '2000-01-01T12:00Z'
        '@ 1 o\'clock pm'             | '2000-01-01T13:00Z'
        '@ + 1 hour'                  | '2000-01-01T11:00Z'
        '@ - 2 minutes'               | '2000-01-01T09:58Z'
        '@ + 4 seconds'               | '2000-01-01T10:00:04Z'
        '@ + 4 milliseconds'          | '2000-01-01T10:00:00.004Z'
        '@ midnight+ 4 minutes'       | '2000-01-01T00:04Z'
        '@ next hour'                 | '2000-01-01T11:00Z'
        '@ last minute'               | '2000-01-01T09:59Z'
        '@ now + 2 hours - 4 minutes' | '2000-01-01T11:56Z'
        '@  + 2 hours - 4 minutes'    | '2000-01-01T11:56Z'
    }

    @Unroll
    def 'datetime expression test - #expression'() {
        expect:
        DateTimeExpression.INSTANCE.executeExpression(dateTime, expression).value.toString() == expected

        where:

        expression                                         | expected
        'today @ 1 o\'clock'                               | '2000-01-01T13:00Z'
        'yesterday @ midnight'                             | '1999-12-31T00:00Z'
        'yesterday @ midnight - 1 hour'                    | '1999-12-31T23:00Z'
        'tomorrow @ now'                                   | '2000-01-02T10:00Z'
        '+ 1 day @ noon'                                   | '2000-01-02T12:00Z'
        '+ 1 week @ +1 hour'                               | '2000-01-08T11:00Z'
        '- 2 weeks @ now + 1 hour'                         | '1999-12-18T11:00Z'
        '+ 4 years @ midnight'                             | '2004-01-01T00:00Z'
        'tomorrow+ 4 years @ 3 o\'clock + 40 milliseconds' | '2004-01-02T15:00:00.040Z'
        'next week @ next hour'                            | '2000-01-08T11:00Z'
        'last month @ last hour'                           | '1999-12-01T09:00Z'
    }

    @Unroll
    def 'datetime expression error test'() {
        expect:
        DateTimeExpression.INSTANCE.executeExpression(dateTime, expression).error ==~ expected

        where:

        expression      | expected
        '+'             | 'Error parsing expression: line 1:1 mismatched input \'<EOF>\' expecting INT'
        'now +'         | 'Error parsing expression: line 1:5 mismatched input \'<EOF>\' expecting INT'
        'tomorr'        | /^Error parsing expression.*/
        'now @ +'       | 'Error parsing expression: line 1:7 mismatched input \'<EOF>\' expecting INT'
        '+ @ +'         | 'Error parsing expression: line 1:2 mismatched input \'<EOF>\' expecting INT, Error parsing expression: line 1:5 mismatched input \'<EOF>\' expecting INT'
        'now+ @ now +'  | 'Error parsing expression: line 1:5 mismatched input \'<EOF>\' expecting INT, Error parsing expression: line 1:12 mismatched input \'<EOF>\' expecting INT'
        'now @ now +'   | 'Error parsing expression: line 1:11 mismatched input \'<EOF>\' expecting INT'
        'now @ noo'     | /^Error parsing expression.*/
    }
}
