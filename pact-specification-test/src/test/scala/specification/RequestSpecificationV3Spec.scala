package specification

import au.com.dius.pact.model.PactReader
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.core.Fragments

@RunWith(classOf[JUnitRunner])
class RequestSpecificationV3Spec extends RequestSpecificationSpec {
  override def is = Fragments(fragments("/v3/request") :_*)

  override def extractRequestSpecification(testJson: java.util.Map[String, AnyRef]) = {
    PactRequestSpecification(testJson.get("match").asInstanceOf[Boolean],
      testJson.get("comment").toString,
      PactReader.extractRequestV3(testJson.get("expected")),
      PactReader.extractRequestV3(testJson.get("actual")))
  }
}
