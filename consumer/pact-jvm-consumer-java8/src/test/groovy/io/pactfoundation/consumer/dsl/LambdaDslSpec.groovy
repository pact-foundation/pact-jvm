package io.pactfoundation.consumer.dsl

import au.com.dius.pact.consumer.dsl.PactDslJsonArray
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue
import au.com.dius.pact.core.model.PactSpecVersion
import spock.lang.Issue
import spock.lang.Specification

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.function.Consumer

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
    result.matchers.matchingRules.keySet() == ['.offer.prices.*', '.offer.shippingCosts.*'] as Set
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
    result.matchers.matchingRules.keySet() == ['.componentsIds', '.componentsIds[*]', '.componentsIds2',
                                               '.componentsIds2[*]'] as Set
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
      '.dateExp': [matchers: [[match: 'date', date: 'yyyy-MM-dd']], combine: 'AND'],
      '.timeExp': [matchers: [[match: 'time', time: 'HH:mm:ss']], combine: 'AND'],
      '.datetimeExp': [matchers: [[match: 'timestamp', timestamp: "yyyy-MM-dd'T'HH:mm:ss"]], combine: 'AND']
    ]
    result.generators.toMap(PactSpecVersion.V3) == [
      body: [
        '.dateExp': [type: 'Date', format: 'yyyy-MM-dd', expression: 'today + 1 day'],
        '.timeExp': [type: 'Time', format: 'HH:mm:ss', expression: 'now + 1 hour'],
        '.datetimeExp': [type: 'DateTime', format: "yyyy-MM-dd'T'HH:mm:ss", expression: 'today + 1 hour']
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
    result.body.toString() == '{"number":1,"bigdecimal":1.1,"bigint":1,"long":1}'
  }

  @Issue('#910')
  def 'serialise date values correctly'() {
    given:
    Consumer<LambdaDslJsonBody> body = { o ->
      o.date('date1', 'yyyy-MM-dd', new Date(949323600000L))
      o.date('date3', 'yyyy-MM-dd', ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")))
    }

    when:
    def result = LambdaDsl.newJsonBody(body).build()

    then:
    result.body.toString() == '{"date3":"2000-01-01","date1":"2000-02-01"}'
  }

}
