package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatch, HttpPart, DiffConfig}

trait BodyMatcher {
  def matchBody(expected: HttpPart, actual: HttpPart, diffConfig: DiffConfig) : List[BodyMismatch]
}
