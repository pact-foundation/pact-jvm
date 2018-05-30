package au.com.dius.pact.matchers

import au.com.dius.pact.model.HttpPart

interface BodyMatcher {
  fun matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): List<BodyMismatch>
}
