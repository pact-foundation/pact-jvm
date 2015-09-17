package specification

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.core.Fragments

@RunWith(classOf[JUnitRunner])
class ResponseSpecificationV2Spec extends ResponseSpecificationSpec {
  override def is = Fragments(fragments("/v2/response/") :_*)
}
