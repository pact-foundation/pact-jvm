package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody

class LambdaDslSpec extends Specification {

  def testArrayMinMaxLike() {
    given:
    String pactDslJson = PactDslJsonArray.arrayMinMaxLike(2, 10)
      .stringType('foo')
      .close().body

    when:
    def actualPactDsl = LambdaDsl.newJsonArrayMinMaxLike(2, 10) { o ->
      o.object { oo -> oo.stringType('foo') }
    }.build()
    String actualJson = actualPactDsl.body

    then:
    actualJson == pactDslJson
  }

  @Issue('#749')
  @SuppressWarnings('UnnecessaryObjectReferences')
  def 'newJsonArrayMinMaxLike should propagate the matchers to all items'() {
    given:
    Consumer<LambdaDslObject> snackJsonResponseFragment = { snackObject ->
      snackObject.numberType('id', 1)
      snackObject.timestamp('created', "yyyy-MM-dd'T'HH:mm:ss.SSS")
      snackObject.timestamp('lastModified', "yyyy-MM-dd'T'HH:mm:ss.SSS")
      snackObject.stringType('creator', 'Loren')
      snackObject.numberType('quantity', 5)
      snackObject.stringType('description', 'donuts')
      snackObject.object('location') { locationObject ->
        locationObject.numberType('floor', 5)
        locationObject.stringType('room', 'south kitchen')
      }
    }
    Consumer<LambdaDslJsonArray> array = { rootArray -> rootArray.object(snackJsonResponseFragment) }

    when:
    def result = LambdaDsl.newJsonArrayMinMaxLike(2, 2, array).build()
    def result2 = LambdaDsl.newJsonArrayMinLike(2, array).build()
    def result3 = LambdaDsl.newJsonArrayMaxLike(2, array).build()

    then:
    result.matchers.matchingRules.keySet() == [
      '', '[*].id', '[*].created', '[*].lastModified', '[*].creator',
      '[*].quantity', '[*].description', '[*].location.floor', '[*].location.room'
    ] as Set
    result2.matchers.matchingRules.keySet() == [
      '', '[*].id', '[*].created', '[*].lastModified', '[*].creator',
      '[*].quantity', '[*].description', '[*].location.floor', '[*].location.room'
    ] as Set
    result3.matchers.matchingRules.keySet() == [
      '', '[*].id', '[*].created', '[*].lastModified', '[*].creator',
      '[*].quantity', '[*].description', '[*].location.floor', '[*].location.room'
    ] as Set
  }

  @Issue('#778')
  def 'each key like should handle primitive values'() {
    /*
    {
      "offer": {
        "prices": {
          "DE": 1620
        },
        "shippingCosts": {
          "DE": {
            "cia": 300
          }
        }
    }
     */

    given:
    Consumer<LambdaDslObject> jsonObject = { object ->
      object.object('offer') { offer ->
        offer.object('prices') { prices ->
          prices.eachKeyLike('DE', PactDslJsonRootValue.numberType(1620))
        }
        offer.object('shippingCosts') { shippingCosts ->
          shippingCosts.eachKeyLike('DE') { cost ->
            cost.numberValue('cia', 300)
          }
        }
      }
    }

    when:
    def result = LambdaDsl.newJsonBody(jsonObject).build()

    then:
    result.matchers.matchingRules.keySet() == ['$.offer.prices', '$.offer.prices.*', '$.offer.shippingCosts'] as Set
    result.toString() == '{"offer":{"prices":{"DE":1620},"shippingCosts":{"DE":{"cia":300}}}}'

  }

  @Issue('#829')
  def 'supports arrays of primitives in objects'() {
    given:
    Consumer<LambdaDslObject> object = { object ->
      object.eachLike('componentsIds', PactDslJsonRootValue.stringType('A1'))
      object.eachLike('componentsIds2', PactDslJsonRootValue.stringType('A1'), 5)
    }

    when:
    def result = LambdaDsl.newJsonBody(object).build()

    then:
    result.body.toString() == '{"componentsIds":["A1"],"componentsIds2":["A1","A1","A1","A1","A1"]}'
    result.matchers.matchingRules.keySet() == ['$.componentsIds', '$.componentsIds[*]', '$.componentsIds2',
                                               '$.componentsIds2[*]'] as Set
  }

  @Issue('#829')
  def 'supports arrays of primitives in arrays'() {
    given:
    Consumer<LambdaDslJsonArray> array = { array ->
      array.eachLike(PactDslJsonRootValue.stringType('A1'))
      array.eachLike(PactDslJsonRootValue.stringType('A1'), 5)
    }

    when:
    def result = LambdaDsl.newJsonArray(array).build()

    then:
    result.body.toString() == '[["A1"],["A1","A1","A1","A1","A1"]]'
    result.matchers.matchingRules.keySet() == ['[0]', '[0][*]', '[1]', '[1][*]'] as Set
  }

  def 'supports date and time expressions'() {
    given:
    Consumer<LambdaDslObject> object = { object ->
      object.dateExpression('dateExp', 'today + 1 day')
      object.timeExpression('timeExp', 'now + 1 hour')
      object.datetimeExpression('datetimeExp', 'today + 1 hour')
    }

    when:
    def result = LambdaDsl.newJsonBody(object).build()

    then:
    result.matchers.toMap(PactSpecVersion.V3) == [
      '$.dateExp': [matchers: [[match: 'date', date: 'yyyy-MM-dd']], combine: 'AND'],
      '$.timeExp': [matchers: [[match: 'time', time: 'HH:mm:ss']], combine: 'AND'],
      '$.datetimeExp': [matchers: [[match: 'timestamp', timestamp: "yyyy-MM-dd'T'HH:mm:ss"]], combine: 'AND']
    ]
    result.generators.toMap(PactSpecVersion.V3) == [
      body: [
        '$.dateExp': [type: 'Date', format: 'yyyy-MM-dd', expression: 'today + 1 day'],
        '$.timeExp': [type: 'Time', format: 'HH:mm:ss', expression: 'now + 1 hour'],
        '$.datetimeExp': [type: 'DateTime', format: "yyyy-MM-dd'T'HH:mm:ss", expression: 'today + 1 hour']
      ]
    ]
  }

  def 'supports date and time expressions with arrays'() {
    given:
    Consumer<LambdaDslJsonArray> array = { array ->
      array.dateExpression('today + 1 day')
      array.timeExpression('now + 1 hour')
      array.datetimeExpression('today + 1 hour')
    }

    when:
    def result = LambdaDsl.newJsonArray(array).build()

    then:
    result.matchers.toMap(PactSpecVersion.V3) == [
      '[0]': [matchers: [[match: 'date', date: 'yyyy-MM-dd']], combine: 'AND'],
      '[1]': [matchers: [[match: 'time', time: 'HH:mm:ss']], combine: 'AND'],
      '[2]': [matchers: [[match: 'timestamp', timestamp: "yyyy-MM-dd'T'HH:mm:ss"]], combine: 'AND']
    ]

    result.generators.toMap(PactSpecVersion.V3) == [
      body: [
        '[0]': [type: 'Date', format: 'yyyy-MM-dd', expression: 'today + 1 day'],
        '[1]': [type: 'Time', format: 'HH:mm:ss', expression: 'now + 1 hour'],
        '[2]': [type: 'DateTime', format: "yyyy-MM-dd'T'HH:mm:ss", expression: 'today + 1 hour']
      ]
    ]
  }

  @Issue('#908')
  def 'serialise number values correctly'() {
    given:
    Consumer<LambdaDslJsonBody> body = { o ->
      o.numberValue('number', 1)
      o.numberValue('long', 1L)
      o.numberValue('bigdecimal', 1.1G)
      o.numberValue('bigint', 1G)
    }

    when:
    def result = LambdaDsl.newJsonBody(body).build()

    then:
    result.body.toString() == '{"bigdecimal":1.1,"bigint":1,"long":1,"number":1}'
  }

  @Issue('#910')
  def 'serialise date values correctly'() {
    given:
    def date = new Date(949323600000L)
    def zonedDateTime = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneId.of('UTC'))
    def format = 'yyyy-MM-dd'
    def date3 = zonedDateTime.format(DateTimeFormatter.ofPattern(format))
    FastDateFormat instance = FastDateFormat.getInstance(format, TimeZone.getTimeZone('UTC'))
    def date1 = instance.format(date)
    Consumer<LambdaDslJsonBody> body = { o ->
      o.date('date1', format, date, TimeZone.getTimeZone('UTC'))
      o.date('date3', format, zonedDateTime)
    }

    when:
    def result = LambdaDsl.newJsonBody(body).build()

    then:
    result.body.toString() == '{"date1":"' + date1 + '","date3":"' + date3 + '"}'
  }

  @Unroll
  def 'generates a root array with ignore-order #expectedMatcher.class.simpleName matching'() {
    given:
    def subject = LambdaDsl."$method"(*params) {
      it.stringValue('a')
        .stringType('b')
    }.build().close()

    when:
    def result = subject.body.toString()

    then:
    result == '["a","b"]'
    subject.matchers.matchingRules == [
        '$': new MatchingRuleGroup([expectedMatcher]),
        '$[1]': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ]

    where:

    method                        | params | expectedMatcher
    'newJsonArrayUnordered'       | []     | EqualsIgnoreOrderMatcher.INSTANCE
    'newJsonArrayMinUnordered'    | [2]    | new MinEqualsIgnoreOrderMatcher(2)
    'newJsonArrayMaxUnordered'    | [4]    | new MaxEqualsIgnoreOrderMatcher(4)
    'newJsonArrayMinMaxUnordered' | [2, 4] | new MinMaxEqualsIgnoreOrderMatcher(2, 4)
  }

  @Issue('#1318')
  def 'array contains with simple values'() {
    given:
    def body = newJsonBody { o ->
      o.arrayContaining('output') { a ->
        a.stringType('a').numberType(100)
      }
    }.build()

    expect:
    body.toString() == '{"output":["a",100]}'
    body.matchers.toMap(PactSpecVersion.V3) == [
      '$.output': [
        matchers: [
          [
            match: 'arrayContains', variants: [
              [index: 0, rules: ['$': [matchers: [[match: 'type']], combine: 'AND']], generators: [:]],
              [index: 1, rules: ['$': [matchers: [[match: 'number']], combine: 'AND']], generators: [:]]
            ]
          ]
        ], combine: 'AND'
      ]
    ]
  }

  @Issue('#1318')
  @SuppressWarnings(['LineLength'])
  def 'array contains with simple values and generators'() {
    given:
    def body = newJsonBody { o ->
      o.arrayContaining('output') { a ->
        a.date('yyyy-MM-dd')
          .stringValue('test')
          .uuid()
      }
    }.build()
    def date = DateFormatUtils.ISO_DATE_FORMAT.format(new Date(DslPart.DATE_2000))

    expect:
    body.toString() == '{"output":["' + date + '","test","e2490de5-5bd3-43d5-b7c4-526e33f71304"]}'
    body.matchers.toMap(PactSpecVersion.V3) == [
      '$.output': [
        matchers: [
          [
            match: 'arrayContains',
            variants: [
              [
                index: 0,
                rules: ['$': [matchers: [[match: 'date', date: 'yyyy-MM-dd']], combine: 'AND']],
                generators: ['$': [type: 'DateTime', format: 'yyyy-MM-dd']]
              ],
              [index: 1, rules: [:], generators: [:]],
              [
                index: 2,
                rules: ['$': [matchers: [[match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']], combine: 'AND']],
                generators: ['$': [type: 'Uuid']]
              ]
            ]
          ]
        ], combine: 'AND'
      ]
    ]
  }
}
