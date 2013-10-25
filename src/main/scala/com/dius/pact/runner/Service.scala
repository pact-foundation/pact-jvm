package com.dius.pact.runner

import com.dius.pact.model.{Response, Request, Provider}

class Service(provider:Provider, http:HttpCalls) {
  def invoke(request:Request):Response = ???
}
