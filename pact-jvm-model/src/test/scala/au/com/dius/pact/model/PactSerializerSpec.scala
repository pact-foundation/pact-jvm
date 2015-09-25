package au.com.dius.pact.model

import org.specs2.mutable.Specification
import java.io.{InputStream, StringWriter, PrintWriter}

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PactSerializerSpec extends Specification {

  def loadTestFile(name: String): InputStream = this.getClass.getClassLoader.getResourceAsStream(name)

   "PactSerializer" should {

     "serialize pact" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString.trim

       PactSerializer.serialize(Fixtures.pact, new PrintWriter(sw))
       val json = sw.toString.trim
       json must beEqualTo(pactString)
     }

     "serialize V3 pact" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact_v3.json")).mkString

       PactSerializer.serialize(Fixtures.pact, new PrintWriter(sw), PactConfig(3))
       val json = sw.toString
       json must beEqualTo(pactString)
     }

     "serialize pact with matchers" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact_matchers.json")).mkString

       PactSerializer.serialize(Fixtures.pactWithMatchers, new PrintWriter(sw), PactConfig(3))
       val json = sw.toString
       json must beEqualTo(pactString)
     }

     "serialize pact converts methods to uppercase" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString.trim

       PactSerializer.serialize(Fixtures.pact.copy(interactions = Fixtures.pact.interactions.map(
         interaction => interaction.copy(request = Fixtures.request.copy(method = "get")))), new PrintWriter(sw))
       val json = sw.toString.trim
       json must beEqualTo(pactString)
     }

     "deserialize pact" in {
       val pact = PactSerializer.from(loadTestFile("test_pact.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize V3 pact" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_v3.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize pact with matchers" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_matchers.json"))
       pact must beEqualTo(Fixtures.pactWithMatchers)
     }

     "deserialize pact with matchers on old format" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_matchers_old_format.json"))
       pact must beEqualTo(Fixtures.pactWithMatchers)
     }

     "deserialize converts http methods to upper case" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_lowercase_method.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize should not convert fields called 'body'" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_with_bodies.json"))
       pact.interactions.head.request.body.get must beEqualTo(("{\n" +
         "  \"complete\" : {\n" +
         "    \"certificateUri\" : \"http://...\",\n" +
         "    \"issues\" : {\n" +
         "      \"idNotFound\" : { }\n" +
         "    },\n" +
         "    \"nevdis\" : {\n" +
         "      \"body\" : null,\n" +
         "      \"colour\" : null,\n" +
         "      \"engine\" : null\n" +
         "    },\n" +
         "    \"body\" : 123456\n" +
         "  },\n" +
         "  \"body\" : [ 1, 2, 3 ]\n" +
         "}").replaceAll("\\s+", ""))
     }

     "deserialize pact with no bodies" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_no_bodies.json"))
       pact must beEqualTo(Fixtures.pactWithNoBodies)
     }

     "deserialize pact with query in old format" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_query_old_format.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize pact with no version" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_no_version.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize pact with no specification version" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_no_spec_version.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize pact with no metadata" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_no_metadata.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize pact with encoded query string" in {
       val pact = PactSerializer.from(loadTestFile("test_pact_encoded_query.json"))
       pact must beEqualTo(Fixtures.pactDecodedQuery)
     }
   }
}
