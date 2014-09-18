package au.com.dius.pact.model

import au.com.dius.pact.model.waitingforscalaz._
import org.json4s._
import org.json4s.jackson.JsonMethods._

object JsonDiff {
  case class DiffConfig(allowUnexpectedKeys: Boolean = true, structural: Boolean = false)

  val defaultConfig = DiffConfig()

  def diff(expected: JValue, actual: JValue, config: DiffConfig): Diff = {

    //TODO: fix this when we update the pact specification to use proper overrides
    val structuralFilter:PartialFunction[JField, JField] = if(config.structural) {
      case (key, JString(_)) => (key, JString("anyString"))
      case (key, JBool(_)) => (key, JBool(true))
      case (key, JInt(_)) => (key, JInt(0))
    } else {
      case doNothing => doNothing
    }

    val expectedJson = expected transformField structuralFilter

    val expectedKeysFilter:JValue => JValue = if(config.allowUnexpectedKeys) {
      (a:JValue) => filterAdditionalKeys(expectedJson, a)
    } else {
      (a:JValue) => a
    }

    val actualJson = expectedKeysFilter(actual transformField structuralFilter)

    DiffPatched.diff(expectedJson, actualJson)
  }

  def diff(expected: String, actual: String, config: DiffConfig): Diff = {
    diff(parse(expected), parse(actual), config)
  }

  val noChange           = Diff(JNothing, JNothing, JNothing)
  def changed(o:JValue) = Diff(o, JNothing, JNothing)
  def added(o:JValue)   = Diff(JNothing, o, JNothing)
  def missing(o:JValue) = Diff(JNothing, JNothing, o)
  def autoParse(s: String) = parse(s)

  /**
   * returns a copy of actual with any keys that aren't present in expected filtered out
   */
  def filterAdditionalKeys(expected:JValue, actual:JValue): JValue = {
    (expected, actual) match {
      case (JObject(ef), JObject(f)) => JObject(filterMatching(ef, f))
      case (JArray(ef), JArray(f)) => JArray(filterMatchingArray(ef, f))
      case _ => actual
    }
  }

  /**
   * remove additional keys from actual fields
   */
  def filterMatching(expectedFields: List[JField], actualFields: List[JField]): List[JField] = {
    actualFields.flatMap {
      case JField(name, value) => {
        expectedFields.find(_._1 == name).map { ef =>
          JField(name, filterAdditionalKeys(ef._2, value))
        }
      }
    }
  }

  /**
   * arrays keep all values
   */
  def filterMatchingArray(expectedValues: List[JsonAST.JValue], actualValues: List[JsonAST.JValue]): List[JsonAST.JValue] = {
    Align(expectedValues, actualValues).flatMap {
      case This(ev) => None
      case That(av) => Some(av)
      case Both(ev, av) => Some(filterAdditionalKeys(ev, av))
    }
  }
}

