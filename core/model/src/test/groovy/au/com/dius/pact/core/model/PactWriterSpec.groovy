package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
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
    def json = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw.toString()))
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
    def json = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw.toString()))
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
    def json = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw.toString()))
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
    def json = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw.toString()))
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
    def json = file.withReader { Json.INSTANCE.toMap(JsonParser.INSTANCE.parseReader(it)) }

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
    def json = file.withReader { Json.INSTANCE.toMap(JsonParser.INSTANCE.parseReader(it)) }

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
    def json = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw.toString()))
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
    pactFile.withReader { Json.INSTANCE.toMap(JsonParser.INSTANCE.parseReader(it)) }.interactions[0].description ==
      'Request für ping'

    cleanup:
    pactFile.delete()
  }

  @Issue('#1006')
  def 'when writing message pact files, the metadata values should be stored as JSON'() {
    given:
    def pactFile = File.createTempFile('PactWriterSpec', '.json')
    def pact = new MessagePact(new Provider(), new Consumer(), [
      new Message('Sample Message', [], OptionalBody.body('body'.bytes, ContentType.TEXT_PLAIN),
        new MatchingRulesImpl(), new Generators(), [test: [1, 2, 3]])
    ])

    when:
    DefaultPactWriter.INSTANCE.writePact(pactFile, pact, PactSpecVersion.V3)

    then:
    pactFile.withReader { Json.INSTANCE.toMap(JsonParser.INSTANCE.parseReader(it)) }.messages[0].metaData ==
      [test: [1, 2, 3]]

    cleanup:
    pactFile.delete()
  }

  @Issue('#1018')
  def 'encode the query parameters correctly with V2 pact files'() {
    given:
    def request = new Request('GET', '/', [
      'include[]': ['term', 'total_scores', 'license', 'is_public', 'needs_grading_count', 'permissions',
                    'current_grading_period_scores', 'course_image', 'favorites']
    ])
    def response = new Response()
    def interaction = new RequestResponseInteraction('test interaction with query parameters',
      [], request, response)
    def pact = new RequestResponsePact(new Provider('PactWriterSpecProvider'),
      new Consumer('PactWriterSpecConsumer'), [interaction])
    def sw = new StringWriter()
    def sw2 = new StringWriter()

    when:
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw), PactSpecVersion.V2)
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw2), PactSpecVersion.V3)
    def json = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw.toString()))
    def interactionJson = json.interactions.first()
    def json2 = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw2.toString()))
    def interactionJson2 = json2.interactions.first()

    then:
    interactionJson.request.query == 'include[]=term&include[]=total_scores&include[]=license&include[]=is_public' +
      '&include[]=needs_grading_count&include[]=permissions&include[]=current_grading_period_scores&include[]' +
      '=course_image&include[]=favorites'
    interactionJson2.request.query == [
      'include[]': ['term', 'total_scores', 'license', 'is_public', 'needs_grading_count', 'permissions',
                    'current_grading_period_scores', 'course_image', 'favorites']]
  }

  def 'writing V4 pacts with comments'() {
    def comments = [
      text: new JsonValue.Array([
        new JsonValue.StringValue('This allows me to specify just a bit more information about the interaction'.chars),
        new JsonValue.StringValue(('It has no functional impact, but can be displayed in the broker ' +
          'HTML page, and potentially in the test output').chars)
      ]),
      testname: new JsonValue.StringValue('example_test.groovy'.chars)
    ]
    given:
    def pact = new V4Pact(
      new Consumer('PactWriterSpecConsumer'),
      new Provider('PactWriterSpecProvider'),
      [
        new V4Interaction.SynchronousHttp('A1', 'A1', [], new HttpRequest(), new HttpResponse(), null,
          comments),
        new V4Interaction.AsynchronousMessage('A2', 'A2', OptionalBody.missing(), [:],
          new MatchingRulesImpl(), new Generators(), null, [], comments)
      ])
    def sw = new StringWriter()

    when:
    DefaultPactWriter.INSTANCE.writePact(pact, new PrintWriter(sw))
    def json = Json.INSTANCE.toMap(JsonParser.INSTANCE.parseString(sw.toString()))
    def interactionJson = json['interactions'][0]
    def interaction2Json = json['interactions'][1]

    then:
    interactionJson['comments']['testname'] == 'example_test.groovy'
    interactionJson['comments']['text'] == [
      'This allows me to specify just a bit more information about the interaction',
      'It has no functional impact, but can be displayed in the broker HTML page, and potentially in the test output'
    ]
    interaction2Json['comments']['testname'] == 'example_test.groovy'
    interaction2Json['comments']['text'] == [
      'This allows me to specify just a bit more information about the interaction',
      'It has no functional impact, but can be displayed in the broker HTML page, and potentially in the test output'
    ]
  }
}
