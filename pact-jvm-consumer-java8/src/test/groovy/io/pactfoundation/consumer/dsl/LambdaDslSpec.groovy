package io.pactfoundation.consumer.dsl

import au.com.dius.pact.consumer.dsl.PactDslJsonArray
import spock.lang.Issue
import spock.lang.Specification

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

}
