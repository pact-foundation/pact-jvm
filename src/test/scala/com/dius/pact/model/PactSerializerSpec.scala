package com.dius.pact.model

import org.specs2.mutable.Specification
import java.io.{File, StringWriter, PrintWriter}
import org.json4s.DefaultFormats

class PactSerializerSpec extends Specification {
   "PactSerializer" should {
     "serialize pact" in {
       val sw = new StringWriter()
       val pactString = io.Source.fromFile(new File("src/test/resources/test_pact.json")).mkString

       Fixtures.pact.serialize(new PrintWriter(sw))
       val json = sw.toString
       json must beEqualTo(pactString)
     }

     "deserialize pact" in {
       val pact = Pact.from(new File("src/test/resources/test_pact.json"))
       pact must beEqualTo(Fixtures.pact)
     }
   }
}
