package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import groovy.json.JsonSlurper
import spock.lang.Specification

class PactWriterSpec extends Specification {

  def 'when writing pacts, do not include optional items that are missing'() {
    given:
    def request = new Request()
    def response = new Response()
    def interaction = new RequestResponseInteraction('test interaction', null, request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()

    when:
    PactWriter.writePact(pact, new PrintWriter(sw))
    def json = new JsonSlurper().parseText(sw.toString())
    def interactionJson = json.interactions.first()

    then:
    !interactionJson.containsKey('providerState')
    !interactionJson.request.containsKey('body')
    !interactionJson.request.containsKey('query')
    !interactionJson.request.containsKey('headers')
    !interactionJson.request.containsKey('matchingRules')
    !interactionJson.request.containsKey('generators')
    !interactionJson.response.containsKey('body')
    !interactionJson.response.containsKey('headers')
    !interactionJson.response.containsKey('generators')
  }

  def 'when writing message pacts, do not include optional items that are missing'() {
    given:
    def message = new Message('test interaction')
    def pact = new MessagePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [message])
    def sw = new StringWriter()

    when:
    PactWriter.writePact(pact, new PrintWriter(sw), PactSpecVersion.V3)
    def json = new JsonSlurper().parseText(sw.toString())
    def messageJson = json.messages.first()

    then:
    !messageJson.containsKey('providerState')
    !messageJson.containsKey('contents')
    !messageJson.containsKey('matchingRules')
    !messageJson.containsKey('generators')
  }

  def 'when writing pacts, do not parse JSON string bodies'() {
    given:
    def request = new Request(body: OptionalBody.body('"This is a string"'.bytes))
    def response = new Response(body: OptionalBody.body('"This is a string"'.bytes))
    def interaction = new RequestResponseInteraction('test interaction with JSON string bodies',
      null, request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()

    when:
    PactWriter.writePact(pact, new PrintWriter(sw))
    def json = new JsonSlurper().parseText(sw.toString())
    def interactionJson = json.interactions.first()

    then:
    interactionJson.request.body == '"This is a string"'
    interactionJson.response.body == '"This is a string"'
  }

  def 'handle non-ascii characters correctly'() {
    given:
    def request = new Request(body: OptionalBody.body('"This is a string with letters ä, ü, ö and ß"'.bytes))
    def response = new Response(body: OptionalBody.body('"This is a string with letters ä, ü, ö and ß"'.bytes))
    def interaction = new RequestResponseInteraction('test interaction with non-ascii characters in bodies',
      null, request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()

    when:
    PactWriter.writePact(pact, new PrintWriter(sw))
    def json = new JsonSlurper().parseText(sw.toString())
    def interactionJson = json.interactions.first()

    then:
    interactionJson.request.body == '"This is a string with letters ä, ü, ö and ß"'
    interactionJson.response.body == '"This is a string with letters ä, ü, ö and ß"'
  }

}
