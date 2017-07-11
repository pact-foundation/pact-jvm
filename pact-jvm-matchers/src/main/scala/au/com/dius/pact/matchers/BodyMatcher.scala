package au.com.dius.pact.matchers

import java.util.Optional

import au.com.dius.pact.model.{BodyMismatch, HttpPart}

trait BodyMatcher {
  def matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean) : List[BodyMismatch]

  def toScalaOption(body: Optional[String]) = {
    if (body.isPresent) Some(body.get)
    else None
  }
}
