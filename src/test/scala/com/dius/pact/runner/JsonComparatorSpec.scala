package com.dius.pact.runner

import org.specs2.mutable.Specification
import org.scalatest.Assertions

class JsonComparatorSpec extends Specification {

  "JsonComparator" should {
    def comparator = new JsonComparator with Assertions {}

    def compare(expected:String, actual:String, result:Boolean = true) = {
      comparator.compareJson(expected, actual) must beEqualTo(result)
    }

    "match empty objects" in {
      compare("{}", "{}")
    }

    "match empty arrays" in {
      compare("[]", "[]")
    }

    "mismatch object vs array" in {
      compare("{}", "[]", false)
    }

    "match array with ints" in {
      compare("[1,2,3]", "[1,2,3]")
    }

    "mismatch array with different values" in {
      compare("[1,2,3]", "[4,5,6]", false)
    }

  }

}
