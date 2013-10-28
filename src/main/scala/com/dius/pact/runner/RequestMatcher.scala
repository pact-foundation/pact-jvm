package com.dius.pact.runner

import com.dius.pact.model.{Request, Response}

trait RequestMatcher {
  def responseFor(request:Request):Response
}
