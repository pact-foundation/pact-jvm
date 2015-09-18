package specification

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.core.Fragments

@RunWith(classOf[JUnitRunner])
class RequestSpecificationV3Spec extends RequestSpecificationSpec {
  override def is = Fragments(fragments("/v3/request") :_*)
}
