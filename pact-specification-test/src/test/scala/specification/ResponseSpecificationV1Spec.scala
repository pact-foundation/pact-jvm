package specification

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Fragments

@RunWith(classOf[JUnitRunner])
class ResponseSpecificationV1Spec extends ResponseSpecificationSpec {

  override def is: Fragments = Fragments.create(fragments("/v1/response/") :_*)
  
}
