package com.dius.pact.model

import org.specs2.mutable.Specification
import org.json4s.Diff
import org.json4s.JsonAST._
import org.json4s.JsonAST.JInt

object JsonAstMagic {
  implicit def jObj[T](t:(String, T))(implicit c:(T) => JValue) = JObject(t._1 -> c(t._2))
  implicit def jInt(i:Int) = JInt(i)
  implicit def jStr(s:String) = JString(s)
  implicit def jBool(b:Boolean) = JBool(b)
//  implicit def jNoop(j:JValue) = j
  
  def jArr[T](n:T *)(implicit c:(T) => JValue):JArray = JArray(n.map(c(_)).toList)
}

class JsonDiffSpec extends Specification {
  import JsonDiff._
  import JsonAstMagic._

  val noChange           = Diff(JNothing, JNothing, JNothing)
  def changed(o:JValue) = Diff(o, JNothing, JNothing)
  def added(o:JValue)   = Diff(JNothing, o, JNothing)
  def missing(o:JValue) = Diff(JNothing, JNothing, o)

  def testDiff(expected:String, actual:String, expectedDiff: Diff = noChange)(implicit config: DiffConfig) = {
    def toString(diff:Diff):String = {
      import org.json4s.jackson.JsonMethods._
      compact(render(JObject("changed" -> diff.changed, "added" -> diff.added, "removed" -> diff.deleted)))
    }

    toString(diff(expected, actual, config)) must beEqualTo(toString(expectedDiff))
  }

  "structural behaviour" should {
    implicit val conf = DiffConfig(structural = true)
    "match similar ints" in {
      val expected = """{"a":1}"""
      val actual = """{"a":2}"""
      testDiff(expected, actual)
    }

    "allow unexpected keys" in {
      val expected = """{"a":1}"""
      val actual = """{"a":2, "b": true}"""
      testDiff(expected, actual)
    }

    "expecting key to be present with nil value and not finding key" should {
      val expected =  """{"a": null}"""
      val actual = """{}"""
      "returns the key in the diff" in {
        testDiff(expected, actual, missing("a" -> JNull))
      }
    }

    "classes match" should {
      val expected = """{"a": "a string", "b": 1, "c": null, "d": [{"e": "thing"}], "f": {"g": 10}, "h": false}"""
      val actual = """{"a": "another string", "b": 2, "c": null, "d": [{"e": "something"}], "f": {"g": 100}, "h": true}"""
      "returns an empty hash" in {
        testDiff(expected, actual)
      }
    }

    "when a key is not found" should {
      val actual = """{"a": "blah"}"""
      val expected = """{"b": "blah"}"""
      val difference = missing("b" -> "anyString")
      "returns the difference" in {
        testDiff(expected, actual, difference)
      }
    }

    "when a number is expected" should {
      val expected =  """{"a": 1}"""
    
      "and a string is found" should {
        val actual =  """{"a": "a string"}"""
        val difference = changed("a" -> "anyString")
        "returns the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "and nil is found" should {
        val actual =  """{"a": null}"""
        val difference  =  changed("a" -> JNull)
        "returns the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "and a hash is found" should {
        val actual =  """{"a": {"b": 1}}"""
        val difference =  changed("a" -> jObj("b" -> 0))
        "returns the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "and an array is found" should {
        val actual =  """{"a": [1] }"""
        val difference =  changed("a" -> jArr(1))
        "returns the diff" in {
          testDiff(expected, actual, difference)
        }
      }
    }

    "when an array is expected" should {
      val expected = """{"a": [1, 2]}"""

      "when an item with differing class values is found" should {
        val actual =  """{"a": [1, false]}"""
        val difference = changed("a" -> false)
        "returns the diff" in {
          testDiff(expected, actual, difference)
        }
      }
    }

    "when null is expected" should {
      val expected =  """{"a": null}"""

      "and a string is found" should {
        val actual =  """{"a": "a string"}"""
        val difference =  changed("a" -> "anyString")
        "returns the diff" in {
          testDiff(expected, actual, difference)
        }
      }
    }
  }



  "forbid unexpected keys" should {
    val expected = """{"a":1}"""
    val actual = """{"a":1, "b":2}"""
    val difference = added("b" -> 2)
    val expectedKeysOnly = DiffConfig(allowUnexpectedKeys = false)

    "returns it in the diff" in {
      testDiff(expected, actual, difference)(expectedKeysOnly)
    }
  }

  //TODO: implement a better way of fancy matching
  //  "fancy matchers" should {
//    "expecting a string matching a regexp and not finding key" should {
//      val expected = """{"a": "/b/"}"""
//      val actual = """{}"""
//      val difference =  missing("a" -> "anyString")
//      "returns the diff" in {
//        testDiff(expected, actual, difference)
//      }
//    }

    //    "when a term is expected" should {
    //      val expected =  """{"a": Pact::Term.new(:matcher => /p/, :generate => 'apple')}"""
    //      "and a non matching string is found" should {
    //        val actual =  {a: 'banana'}
    //        val difference =  {:a=>Pact::Matchers::Difference.new(/p/,"banana")}
    //        "returns the diff" in {
    //          testDiff(expected, actual, difference)
    //        }
    //      }
    //    }

  //
  //  "when the expected value is a String matcher" should {
  //
  //    }
  //
  //  "when the expected value is a Number matcher" should {
  //
  //    }
  //  "when the expected value is an array with a matcher" should {
  //
  //    }
  //  "when the expected value is a hash with a matcher" should {
  //
  //    }
  //  "where a Pact::Term is found that matches the actual value" should {
  //    val expected = """ {:a => Pact::Term.new(:matcher => /a/, :generate => 'apple')} """
  //  val actual =  {:a => "apple" }
  //
  //  "does not include this in the diff" in {
  //    expect(diff(expected, actual)).to eq({})
  //  }
  //  }
  //  "when an array that matches the Pact::Term is found" should {
  //    val expected = """ [Pact::Term.new(:matcher => /4/, :generate => '4'),"5","6"] """
  //  val actual =  ["4","5","6"]
  //
  //  "includes this in the diff" in {
  //    expect(diff(expected, actual)).to eq({})
  //  }

  //  }

  "default behaviour" should {
    implicit val config = DiffConfig()

    "when expected is longer than the actual" should {
      val expected = """[1, 2, 3]"""
      val actual = """[1, 2]"""
      val difference = missing(jArr(3))
      "returns the diff" in {
        testDiff(expected, actual, difference)
      }
    }

    "when actual array is longer than the expected" should {
      val expected = """[1]"""
      val actual =  """[1, 2]"""
      val difference =  added(jArr(2))
      "returns the diff" in {
        testDiff(expected, actual, difference)
      }
    }

    "where an expected value is a non-empty string" should {
      val expected = """ {"a": "a", "b": "b"} """

      "and the actual value is an empty string" should {
        val actual =  """ {"a": "a", "b": ""} """

        "includes this in the diff" in {
          testDiff(expected, actual, changed("b" -> "" ))
        }
      }
    }

    "when the expected value is a hash" should {
      val expected = """{"c": {"a":"b"}} """

      "when the actual value is an array" should {
        val actual = """{"c":[1]}"""
        val difference =  changed("c" -> jArr(1))
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is an hash" should {
        val actual =  """{"c": {"b": "c"}}"""
        val difference =  missing("c" -> jObj("a" -> "b"))
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is an number" should {
        val actual =  """{"c":1}"""
        val difference =  changed("c" -> 1)
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is a string" should {
        val actual =  """{"c":"Thing"}"""
        val difference =  changed("c" -> "Thing")
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is the same" should {
        val actual = """{"c": {"a":"b"}}"""
        "should return an empty hash" in {
          testDiff(expected, actual)
        }
      }
    }

    "when the expected value is an array" should {
      val expected = """ {"a": [1]} """

      "when the actual value is an array" should {
        val actual =  """{"a": [2]}"""
        val difference =  changed("a" -> 2)
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is an hash" should {
        val actual =  """{"a": {"b": "c"}}"""
        val difference =  changed("a" -> jObj("b" -> "c"))
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is an number" should {
        val actual =  """{"a": 1}"""
        val difference =  changed("a" -> 1)
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is a string" should {
        val actual =  """{"a": "Thing"}"""
        val difference =  changed("a" -> "Thing")
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is the same" should {
        val actual = """{"a": [1] }"""
        "should return an empty hash" in {
          testDiff(expected, actual)
        }
      }
    }

    "when the expected value is a string" should {
      val expected = """{"a": "Thing"}"""

      "when the actual value is an array" should {
        val actual =  """{"a": [2]}"""
        val difference =  changed("a" -> jArr(2))
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is an hash" should {
        val actual =  """{"a": {"b": "c"}}"""
        val difference =  changed("a" -> jObj("b" -> "c"))
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is an number" should {
        val actual =  """{"a": 1}"""
        val difference =  changed("a" -> 1)
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is a string" should {
        val actual =  """{"a": "Another Thing"}"""
        val difference =  changed("a" -> "Another Thing")
        "should return the diff" in {
          testDiff(expected, actual, difference)
        }
      }

      "when the actual value is the same" should {
        val actual = """{"a": "Thing" }"""
        "should return an empty hash" in {
          testDiff(expected, actual)
        }
      }
    }

    "when the expected value is a number" should {
      val expected = """{"a": 1}"""
       
      "when the actual value is an array" should {
        val actual =  """{"a": [2]}""" 
        "should return the diff" in {
          testDiff(expected, actual, changed("a" -> jArr(2)))
        }
      }

      "when the actual value is an hash" should {
        val actual =  """{"a": {"b": "c"}}""" 
        "should return the diff" in {
          testDiff(expected, actual, changed("a" -> jObj("b" -> "c")))
        }
      }

      "when the actual value is an number" should {
        val actual =  """{"a": 2}""" 
        "should return the diff" in {
          testDiff(expected, actual, changed("a" -> 2))
        }
      }

      "when the actual value is a string" should {
        val actual =  """{"a": "Thing"}"""
        "should return the diff" in {
          testDiff(expected, actual, changed("a" -> "Thing"))
        }
      }

      "when the actual value is the same" should {
        val actual = """{"a": 1}""" 
        "should return an empty hash" in {
          testDiff(expected, actual)
        }
      }
    }

    "when an expected value is null but not nil is found" should {
      val expected = """ {"a": null} """
      val actual =  """{"a": "blah"}"""
      val difference =  changed("a" -> "blah")
      "should return the diff" in {
        testDiff(expected, actual, difference)
      }
    }

    "a deep mismatch" should {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      val expected = """ {"a":  {"b": { "c": [1,2]}, "d": { "e": "apple"}}, "f": 1, "g": {"h": 99}} """
      val actual =  """{"a":  {"b": { "c": [1,2]}, "d": { "e": "food"}}, "f": "thing"}"""

      val change = """{"a": {"d": {"e": "food" }}, "f": "thing"}"""
      val difference =  Diff(changed = parse(change), deleted = "g" -> jObj("h" -> 99), added=JNothing)

      "should return the diff" in {
        testDiff(expected, actual, difference)
      }
    }

    "arrays of objects" should {
      val expected = """ {"a" : [{"b": 1}, {"c": 2, "d": 3}]} """
      "ignore additional keys" in {
        val actual = """ {"a" : [{"b": 1, "d": 3}, {"c": 2, "d": 3}]}"""
        testDiff(expected, actual)
      }

      "match by class" in {
        val actual = """ {"a" : [{"b": 9}, {"c": 2, "d": 3}]}"""
        testDiff(expected, actual)
      }

      "fail on missing keys" in {
        val actual = """ {"a" : [{"b": 1}, {"c": 2}]}"""
        testDiff(expected, actual, missing("a" -> jObj("d" -> 3)))
      }
    }
  }
}
