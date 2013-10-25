package com.dius.pact.runner

trait HttpCalls {
  def url(remoteUrl:String) = play.api.libs.ws.WS.url(remoteUrl)
}
