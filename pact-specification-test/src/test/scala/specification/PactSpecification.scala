package specification

import au.com.dius.pact.model.Request

case class PactSpecification(`match`: Boolean, comment: String, expected: Request, actual: Request)
