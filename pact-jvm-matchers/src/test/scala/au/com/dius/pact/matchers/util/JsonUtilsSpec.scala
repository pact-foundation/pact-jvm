package au.com.dius.pact.matchers.util

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JsonUtilsSpec extends Specification {

  "Parsing JSON bodies" in {

    "handles a normal JSON body" in {
      JsonUtils.parseJsonString(
        "{\"password\":\"123456\",\"firstname\":\"Brent\",\"booleam\":\"true\",\"username\":\"bbarke\",\"lastname\":\"Barker\"}"
      ) must beEqualTo(Map("username" -> "bbarke", "firstname" -> "Brent", "lastname" -> "Barker",
        "booleam" -> "true", "password" -> "123456"))
    }

    "handles a String" in {
      JsonUtils.parseJsonString("\"I am a string\"") must beEqualTo("I am a string")
    }

    "handles a Number" in {
      JsonUtils.parseJsonString("1234") must beEqualTo(1234)
    }

    "handles a Boolean" in {
      JsonUtils.parseJsonString("true") must beEqualTo(true)
    }

    "handles a Null" in {
      JsonUtils.parseJsonString("null") must beNull
    }

    "handles an array" in {
      JsonUtils.parseJsonString("[1, 2, 3, 4]") must beEqualTo(Seq(1, 2, 3, 4))
    }

    "handles an empty body" in {
      JsonUtils.parseJsonString("") must beNull
    }

  }

}
