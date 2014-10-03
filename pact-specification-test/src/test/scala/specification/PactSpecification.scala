package specification

import au.com.dius.pact.model.{Response, Request}

case class PactRequestSpecification(`match`: Boolean, comment: String, expected: Request, actual: Request)
case class PactResponseSpecification(`match`: Boolean, comment: String, expected: Response, actual: Response)
