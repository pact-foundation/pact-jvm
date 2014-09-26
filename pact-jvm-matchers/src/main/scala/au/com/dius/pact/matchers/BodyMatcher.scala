package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatch, HttpPart}
import au.com.dius.pact.model.JsonDiff.DiffConfig

trait BodyMatcher {
  def matchBody(expected: HttpPart, actual: HttpPart, diffConfig: DiffConfig) : List[BodyMismatch]
}
