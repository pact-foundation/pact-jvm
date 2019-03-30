package au.com.dius.pact.model.generators

import spock.lang.Specification
import spock.lang.Unroll

import java.time.OffsetDateTime
import java.time.ZoneOffset

class DateExpressionSpec extends Specification {

    private dateTime

    def setup() {
        dateTime = OffsetDateTime.of(2000, 01, 01, 0, 0, 0, 0, ZoneOffset.UTC)
    }

    @Unroll
    def 'date expression test - #expression'() {
        expect:
        DateExpression.INSTANCE.executeDateExpression(dateTime, expression).value.toString() == expected

        where:

        expression                         | expected
        ''                                 | '2000-01-01T00:00Z'
        'now'                              | '2000-01-01T00:00Z'
        'today'                            | '2000-01-01T00:00Z'
        'yesterday'                        | '1999-12-31T00:00Z'
        'tomorrow'                         | '2000-01-02T00:00Z'
        '+ 1 day'                          | '2000-01-02T00:00Z'
        '- 2 weeks'                        | '1999-12-18T00:00Z'
        '+ 4 years'                        | '2004-01-01T00:00Z'
        'tomorrow+ 4 years'                | '2004-01-02T00:00Z'
        'next week'                        | '2000-01-08T00:00Z'
        'last month'                       | '1999-12-01T00:00Z'
        'next fortnight'                   | '2000-01-15T00:00Z'
        'next monday'                      | '2000-01-03T00:00Z'
        'last wednesday'                   | '1999-12-29T00:00Z'
        'next mon'                         | '2000-01-03T00:00Z'
        'last december'                    | '1999-12-01T00:00Z'
        'next jan'                         | '2001-01-01T00:00Z'
        'next june + 2 weeks'              | '2000-06-15T00:00Z'
        'last mon + 2 weeks'               | '2000-01-10T00:00Z'
        '+ 1 day - 2 weeks'                | '1999-12-19T00:00Z'
        'last december + 2 weeks + 4 days' | '1999-12-19T00:00Z'
    }

    @Unroll
    def 'date expression error test'() {
        expect:
        DateExpression.INSTANCE.executeDateExpression(dateTime, expression).error ==~ expected

        where:

        expression | expected
        '+'        | 'Error parsing expression: line 1:1 mismatched input \'<EOF>\' expecting INT'
        'now +'    | 'Error parsing expression: line 1:5 mismatched input \'<EOF>\' expecting INT'
        'tomorr'   | /^Error parsing expression.*/
    }
}
