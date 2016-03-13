package specification

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.core.Fragments

//@RunWith(classOf[JUnitRunner])
class ResponseSpecificationV1_1Spec extends ResponseSpecificationSpec {
  override def is = Fragments(fragments("/v1.1/response/") :_*)
}
