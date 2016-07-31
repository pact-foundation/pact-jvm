package au.com.dius.pact.model

import groovy.json.JsonSlurper
import scala.collection.JavaConversions
import spock.lang.Specification

class PactSerialiserSpec extends Specification {

  def loadTestFile(String name) {
    PactSerialiserSpec.classLoader.getResourceAsStream(name)
  }

  def 'PactSerialiser must serialise pact'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)

    when:
    PactWriter.writePact(ModelFixtures.pact(), new PrintWriter(sw), PactSpecVersion.V2)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must serialise V3 pact'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact_v3.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)

    when:
    PactWriter.writePact(ModelFixtures.pact(), new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must serialise pact with matchers'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact_matchers.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)

    when:
    PactWriter.writePact(ModelFixtures.pactWithMatchers(), new PrintWriter(sw), PactSpecVersion.V3)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must convert methods to uppercase'() {
    given:
    def sw = new StringWriter()
    def testPactJson = loadTestFile('test_pact.json').text.trim()
    def testPact = new JsonSlurper().parseText(testPactJson)
    def pact = new RequestResponsePact(ModelFixtures.provider(), ModelFixtures.consumer(),
      JavaConversions.seqAsJavaList(ModelFixtures.interactionsWithLowerCaseMethods().toSeq()))

    when:
    PactWriter.writePact(pact, new PrintWriter(sw), PactSpecVersion.V2)
    def actualPactJson = sw.toString().trim()
    def actualPact = new JsonSlurper().parseText(actualPactJson)

    then:
    actualPact == testPact
  }

  def 'PactSerialiser must de-serialise pact'() {
    expect:
    pact == ModelFixtures.pact()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact.json'))
  }

  def 'PactSerialiser must de-serialise V3 pact'() {
    expect:
    pact == ModelFixtures.pact()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_v3.json'))
  }

  def 'PactSerialiser must de-serialise pact with matchers'() {
    expect:
    pact == ModelFixtures.pactWithMatchers()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_matchers.json'))
  }

  def 'PactSerialiser must de-serialise pact matchers in old format'() {
    expect:
    pact == ModelFixtures.pactWithMatchers()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_matchers_old_format.json'))
  }

  def 'PactSerialiser must convert http methods to upper case'() {
    expect:
    pact == ModelFixtures.pact()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_lowercase_method.json'))
  }

  def 'PactSerialiser must not convert fields called \'body\''() {
    expect:
    pactBody == new JsonSlurper().parseText('{\n' +
      '  "body" : [ 1, 2, 3 ],\n' +
      '  "complete" : {\n' +
      '    "body" : 123456,\n' +
      '    "certificateUri" : "http://...",\n' +
      '    "issues" : {\n' +
      '      "idNotFound" : { }\n' +
      '    },\n' +
      '    "nevdis" : {\n' +
      '      "body" : null,\n' +
      '      "colour" : null,\n' +
      '      "engine" : null\n' +
      '    }\n' +
      '  }\n' +
      '}')

    where:
    pactBody = new JsonSlurper().parseText(
      PactReader.loadPact(loadTestFile('test_pact_with_bodies.json')).interactions[0].request.body.value )
  }

  def 'PactSerialiser must deserialise pact with no bodies'() {
    expect:
    pact == ModelFixtures.pactWithNoBodies()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_bodies.json'))
  }

  def 'PactSerialiser must deserialise pact with query in old format'() {
    expect:
    pact == ModelFixtures.pact()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_query_old_format.json'))
  }

  def 'PactSerialiser must deserialise pact with no version'() {
    expect:
    pact == ModelFixtures.pact()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_version.json'))
  }

  def 'PactSerialiser must deserialise pact with no specification version'() {
    expect:
    pact == ModelFixtures.pact()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_spec_version.json'))
  }

  def 'PactSerialiser must deserialise pact with no metadata'() {
    expect:
    pact == ModelFixtures.pact()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_no_metadata.json'))
  }

  def 'PactSerialiser must deserialise pact with encoded query string'() {
    expect:
    pact == ModelFixtures.pactDecodedQuery()

    where:
    pact = PactReader.loadPact(loadTestFile('test_pact_encoded_query.json'))
  }

}
