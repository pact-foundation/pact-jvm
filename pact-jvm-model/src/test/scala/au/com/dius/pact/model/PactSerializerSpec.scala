package au.com.dius.pact.model

import au.com.dius.pact.com.typesafe.scalalogging.StrictLogging
import groovy.json.JsonSlurper
import org.specs2.mutable.Specification
import java.io.{InputStream, StringWriter, PrintWriter}

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PactSerializerSpec extends Specification with StrictLogging {

  sequential

  def loadTestFile(name: String): InputStream = this.getClass.getClassLoader.getResourceAsStream(name)

   "PactSerializer" should {

     "serialize pact" in {
       val sw = new StringWriter()
       val pactString = new JsonSlurper().parseText(scala.io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString.trim)

       PactWriter.writePact(ModelFixtures.pact, new PrintWriter(sw), PactSpecVersion.V2)
       val json = new JsonSlurper().parseText(sw.toString.trim)
       json must beEqualTo(pactString)
     }

     "serialize V3 pact" in {
       val sw = new StringWriter()
       val pactString = new JsonSlurper().parseText(scala.io.Source.fromInputStream(loadTestFile("test_pact_v3.json")).mkString)

       PactWriter.writePact(ModelFixtures.pact, new PrintWriter(sw), PactSpecVersion.V3)
       val json = new JsonSlurper().parseText(sw.toString)
       json must beEqualTo(pactString)
     }

     "serialize pact with matchers" in {
       val sw = new StringWriter()
       val pactString = new JsonSlurper().parseText(scala.io.Source.fromInputStream(loadTestFile("test_pact_matchers.json")).mkString)

       PactWriter.writePact(ModelFixtures.pactWithMatchers, new PrintWriter(sw), PactSpecVersion.V3)
       val json = new JsonSlurper().parseText(sw.toString)
       json must beEqualTo(pactString)
     }

     "serialize pact converts methods to uppercase" in {
       val sw = new StringWriter()
       val pactString = new JsonSlurper().parseText(scala.io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString.trim)

       PactWriter.writePact(ModelFixtures.pactWithLowercaseMethods, new PrintWriter(sw),
         PactSpecVersion.V2)
       val json = new JsonSlurper().parseText(sw.toString.trim)
       json must beEqualTo(pactString)
     }

     "deserialize pact" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact.json"))
       pact must beEqualTo(ModelFixtures.pact)
     }

     "deserialize V3 pact" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_v3.json")).asInstanceOf[RequestResponsePact]
       pact must beEqualTo(ModelFixtures.pact)
     }

     "deserialize pact with matchers" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_matchers.json"))
       pact must beEqualTo(ModelFixtures.pactWithMatchers)
     }

     "deserialize pact with matchers on old format" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_matchers_old_format.json"))
       pact must beEqualTo(ModelFixtures.pactWithMatchers)
     }

     "deserialize converts http methods to upper case" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_lowercase_method.json"))
       pact must beEqualTo(ModelFixtures.pact)
     }

     "deserialize should not convert fields called 'body'" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_with_bodies.json")).asInstanceOf[RequestResponsePact]
       pact.getInteractions.get(0).getRequest.getBody.getValue must beEqualTo(("{\n" +
         "  \"body\" : [ 1, 2, 3 ],\n" +
         "  \"complete\" : {\n" +
         "    \"body\" : 123456,\n" +
         "    \"certificateUri\" : \"http://...\",\n" +
         "    \"issues\" : {\n" +
         "      \"idNotFound\" : { }\n" +
         "    },\n" +
         "    \"nevdis\" : {\n" +
         "      \"body\" : null,\n" +
         "      \"colour\" : null,\n" +
         "      \"engine\" : null\n" +
         "    }\n" +
         "  }\n" +
         "}").replaceAll("\\s+", ""))
     }

     "deserialize pact with no bodies" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_no_bodies.json")).asInstanceOf[RequestResponsePact]
       pact must beEqualTo(ModelFixtures.pactWithNoBodies)
     }

     "deserialize pact with query in old format" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_query_old_format.json"))
       pact must beEqualTo(ModelFixtures.pact)
     }

     "deserialize pact with no version" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_no_version.json"))
       pact must beEqualTo(ModelFixtures.pact)
     }

     "deserialize pact with no specification version" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_no_spec_version.json"))
       pact must beEqualTo(ModelFixtures.pact)
     }

     "deserialize pact with no metadata" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_no_metadata.json"))
       pact must beEqualTo(ModelFixtures.pact)
     }

     "deserialize pact with encoded query string" in {
       val pact = PactReader.loadPact(loadTestFile("test_pact_encoded_query.json"))
       pact must beEqualTo(ModelFixtures.pactDecodedQuery)
     }
   }
}
