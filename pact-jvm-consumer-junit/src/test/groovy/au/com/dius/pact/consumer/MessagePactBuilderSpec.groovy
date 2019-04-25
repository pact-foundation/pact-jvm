package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.Matchers
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.model.v3.messaging.Message
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class MessagePactBuilderSpec extends Specification {

  def 'builder should close the DSL objects correctly'() {
    given:
    PactDslJsonBody getBody = new PactDslJsonBody()
    getBody
      .object('metadata')
        .stringType('messageId', 'test')
        .stringType('date', 'test')
        .stringType('contractVersion', 'test')
      .closeObject()
      .object('payload')
        .stringType('name', 'srm.countries.get')
        .stringType('iri', 'some_iri')
      .closeObject()
    .closeObject()

    Map<String, String> metadata = [
      'contentType': 'application/json',
      'destination': Matchers.regexp(~/\w+\d+/, 'X001')
    ]

    MessagePactBuilder builder = MessagePactBuilder.consumer('MessagePactBuilderSpec')
    builder.given('srm.countries.get_message')
      .expectsToReceive('srm.countries.get')
      .withContent(getBody)
      .withMetadata(metadata)

    when:
    def pact = builder.toPact()
    Message message = pact.interactions.first()
    def messageBody = new JsonSlurper().parseText(message.contents.valueAsString())
    def messageMetadata = message.metaData

    then:
    messageBody == [
      metadata: [
        date: 'test',
        messageId: 'test',
        contractVersion: 'test'
      ],
      payload: [
        iri: 'some_iri',
        name: 'srm.countries.get'
      ]
    ]
    messageMetadata == [contentType: 'application/json', destination: 'X001']
    message.matchingRules.rules.body.matchingRules.keySet() == [
      '$.metadata.messageId', '$.metadata.date', '$.metadata.contractVersion', '$.payload.name', '$.payload.iri'
    ] as Set
    message.matchingRules.rules.metadata.matchingRules.keySet() == [
      'destination'
    ] as Set
  }

  @Unroll
  def 'only set the content type if it has not already been set'() {
    given:
    def body = new PactDslJsonBody()
      .object('payload')
        .stringType('name', 'srm.countries.get')
        .stringType('iri', 'some_iri')
      .closeObject()

    Map<String, String> metadata = [
      (contentTypeAttr): 'application/json'
    ]

    when:
    def pact = MessagePactBuilder
      .consumer('MessagePactBuilderSpec')
      .given('srm.countries.get_message')
      .expectsToReceive('srm.countries.get')
      .withMetadata(metadata)
      .withContent(body).toPact()
    Message message = pact.interactions.first()
    def messageMetadata = message.metaData

    then:
    messageMetadata == [contentType: 'application/json']

    where:

    contentTypeAttr << ['contentType', 'contenttype', 'Content-Type', 'content-type']
  }

}
