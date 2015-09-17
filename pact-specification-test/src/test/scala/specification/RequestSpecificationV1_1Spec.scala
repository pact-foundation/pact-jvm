package specification

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.core.Fragments

@RunWith(classOf[JUnitRunner])
class RequestSpecificationV1_1Spec extends RequestSpecificationSpec {
  override def is = Fragments(fragments("/v1.1/request/") :_*)
}
