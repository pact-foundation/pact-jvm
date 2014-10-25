package au.com.dius.pact.model

import org.specs2.mutable.Specification
import java.io.{InputStream, StringWriter, PrintWriter}

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.json4s.jackson.JsonMethods._
import org.json4s.JObject

@RunWith(classOf[JUnitRunner])
class PactSerializerSpec extends Specification {

  def loadTestFile(name: String): InputStream = {
    this.getClass.getClassLoader.getResourceAsStream(name)
  }

   "PactSerializer" should {
     "serialize pact" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString

       Fixtures.pact.serialize(new PrintWriter(sw))
       val json = sw.toString
       json must beEqualTo(pactString)
     }

     "serialize pact with matchers" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact_matchers.json")).mkString

       Fixtures.pactWithMatchers.serialize(new PrintWriter(sw))
       val json = sw.toString
       json must beEqualTo(pactString)
     }

     "serialize pact converts methods to uppercase" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString

       Fixtures.pact.copy(interactions = Fixtures.pact.interactions.map(
         interaction => interaction.copy(request = Fixtures.request.copy(method = "get"))))
         .serialize(new PrintWriter(sw))
       val json = sw.toString
       json must beEqualTo(pactString)
     }

     "serialize pact URL decodes the path" in {
       val sw = new StringWriter()
       val pactString = scala.io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString

       Fixtures.pact.copy(interactions = Fixtures.pact.interactions.map(
         interaction => interaction.copy(request = Fixtures.request.copy(path = "%2Fpath%2FTEST+PATH%2F2014-14-06+23%3A22%3A21"))))
         .serialize(new PrintWriter(sw))
       val pact = parse(sw.toString)
       val request = pact.asInstanceOf[JObject].values("interactions").asInstanceOf[List[Map[String, Any]]].head("request")
       request.asInstanceOf[Map[String, Any]]("path") must beEqualTo("/path/TEST PATH/2014-14-06 23:22:21")
     }

     "deserialize pact" in {
       val pact = Pact.from(loadTestFile("test_pact.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize pact with matchers" in {
       val pact = Pact.from(loadTestFile("test_pact_matchers.json"))
       pact must beEqualTo(Fixtures.pactWithMatchers)
     }

     "deserialize converts http methods to upper case" in {
       val pact = Pact.from(loadTestFile("test_pact_lowercase_method.json"))
       pact must beEqualTo(Fixtures.pact)
     }
   }
}
