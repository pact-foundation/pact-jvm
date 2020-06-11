package io.pactfoundation.consumer.dsl

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import groovy.json.JsonSlurper
import spock.lang.Issue
import spock.lang.Specification

class LambdaDslJsonBodySpec extends Specification {

  @Issue('#1107')
  def 'handle datetimes with Zone IDs'() {
    given:
    def body = new LambdaDslJsonBody(new PactDslJsonBody())

    when:
    body.datetime('test', "yyyy-MM-dd'T'HH:mmx'['VV']'")
    def result = new JsonSlurper().parseText(body.pactDslObject.toString())

    then:
    result.test ==~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}[-+]\d+\[\w+(\/\w+)?]/
  }
}
