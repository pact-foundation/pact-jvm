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
       val pactString = io.Source.fromInputStream(loadTestFile("test_pact.json")).mkString

       Fixtures.pact.serialize(new PrintWriter(sw))
       val json = sw.toString
       json must beEqualTo(pactString)
     }

     "deserialize pact" in {
       val pact = Pact.from(loadTestFile("test_pact.json"))
       pact must beEqualTo(Fixtures.pact)
     }
   }
}
