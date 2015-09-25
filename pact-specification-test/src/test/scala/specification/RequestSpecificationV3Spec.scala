package specification

import au.com.dius.pact.model.PactSerializer
import org.json4s.{DefaultFormats, JValue}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.core.Fragments

@RunWith(classOf[JUnitRunner])
class RequestSpecificationV3Spec extends RequestSpecificationSpec {
  override def is = Fragments(fragments("/v3/request") :_*)

  override def extractRequestSpecification(testJson: JValue) = {
    implicit val formats = DefaultFormats
    PactRequestSpecification((testJson \ "match").extract[Boolean],
      (testJson \ "comment").extract[String],
      PactSerializer.extractRequestV3(testJson \ "expected"),
      PactSerializer.extractRequestV3(testJson \ "actual"))
  }
}
