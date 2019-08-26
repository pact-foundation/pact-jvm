package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.Json
import com.google.gson.JsonParser
import spock.lang.Issue
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

class PactWriterSpec extends Specification {

  def 'when writing pacts, do not include optional items that are missing'() {
    given:
    def request = new Request()
    def response = new Response()
    def interaction = new RequestResponseInteraction('test interaction', [], request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()

    when:
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw))
    def json = Json.INSTANCE.toMap(new JsonParser().parse(sw.toString()))
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
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw), PactSpecVersion.V3)
    def json = Json.INSTANCE.toMap(new JsonParser().parse(sw.toString()))
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
      [], request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()

    when:
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw))
    def json = Json.INSTANCE.toMap(new JsonParser().parse(sw.toString()))
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
      [], request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()

    when:
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw))
    def json = Json.INSTANCE.toMap(new JsonParser().parse(sw.toString()))
    def interactionJson = json.interactions.first()

    then:
    interactionJson.request.body == '"This is a string with letters ä, ü, ö and ß"'
    interactionJson.response.body == '"This is a string with letters ä, ü, ö and ß"'
  }

  def 'when writing a pact file to disk, merge the pact with any existing one'() {
    given:
    def request = new Request()
    def response = new Response()
    def interaction = new RequestResponseInteraction('test interaction',
      [], request, response)
    def interaction2 = new RequestResponseInteraction('test interaction two',
      [], request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def file = File.createTempFile('PactWriterSpec', '.json')

    when:
    DefaultPactWriter.INSTANCE.writePact(file, pact, PactSpecVersion.V3)
    pact.interactions = [interaction2]
    DefaultPactWriter.INSTANCE.writePact(file, pact, PactSpecVersion.V3)
    def json = file.withReader { Json.INSTANCE.toMap(new JsonParser().parse(it)) }

    then:
    json.interactions*.description == ['test interaction', 'test interaction two']

    cleanup:
    file.delete()
  }

  @RestoreSystemProperties
  def 'overwrite any existing pact file if the pact.writer.overwrite property is set'() {
    given:
    def request = new Request()
    def response = new Response()
    def interaction = new RequestResponseInteraction('test interaction',
      [], request, response)
    def interaction2 = new RequestResponseInteraction('test interaction two',
      [], request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def file = File.createTempFile('PactWriterSpec', '.json')
    System.setProperty('pact.writer.overwrite', 'true')

    when:
    DefaultPactWriter.INSTANCE.writePact(file, pact, PactSpecVersion.V3)
    pact.interactions = [interaction2]
    DefaultPactWriter.INSTANCE.writePact(file, pact, PactSpecVersion.V3)
    def json = file.withReader { Json.INSTANCE.toMap(new JsonParser().parse(it)) }

    then:
    json.interactions*.description == ['test interaction two']

    cleanup:
    file.delete()
  }

  @Issue('#877')
  def 'keep null attributes in the body'() {
    given:
    def request = new Request(body: OptionalBody.body(
      '{"settlement_summary": {"capture_submit_time": null,"captured_date": null}}'.bytes))
    def response = new Response(body: OptionalBody.body(
      '{"settlement_summary": {"capture_submit_time": null,"captured_date": null}}'.bytes))
    def interaction = new RequestResponseInteraction('test interaction with null values in bodies',
      [], request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()

    when:
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw))
    def json = Json.INSTANCE.toMap(new JsonParser().parse(sw.toString()))
    def interactionJson = json.interactions.first()

    then:
    interactionJson.request.body == [settlement_summary: [capture_submit_time: null, captured_date: null]]
    interactionJson.response.body == [settlement_summary: [capture_submit_time: null, captured_date: null]]
  }

  @Issue('#879')
  def 'when merging pact files, the original file must be read using UTF-8'() {
    given:
    def pactFile = File.createTempFile('PactWriterSpec', '.json')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [
      new RequestResponseInteraction('Request für ping')
    ])

    when:
    DefaultPactWriter.INSTANCE.writePact(pactFile, pact, PactSpecVersion.V3)
    DefaultPactWriter.INSTANCE.writePact(pactFile, pact, PactSpecVersion.V3)

    then:
    pactFile.withReader { Json.INSTANCE.toMap(new JsonParser().parse(it)) }.interactions[0].description ==
      'Request für ping'

    cleanup:
    pactFile.delete()
  }
}
