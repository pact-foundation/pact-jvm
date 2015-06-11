package au.com.dius.pact.model

import org.specs2.mutable.Specification
import java.io.{InputStream, StringWriter, PrintWriter}

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

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

     "deserialize pact" in {
       val pact = Pact.from(loadTestFile("test_pact.json"))
       pact must beEqualTo(Fixtures.pact)
     }

     "deserialize pact with matchers" in {
       val pact = Pact.from(loadTestFile("test_pact_matchers.json"))
       pact must beEqualTo(Fixtures.pactWithMatchers)
     }

     "deserialize pact with matchers on old format" in {
       val pact = Pact.from(loadTestFile("test_pact_matchers_old_format.json"))
       pact must beEqualTo(Fixtures.pactWithMatchers)
     }

     "deserialize converts http methods to upper case" in {
       val pact = Pact.from(loadTestFile("test_pact_lowercase_method.json"))
       pact must beEqualTo(Fixtures.pact)
     }
   }
}
