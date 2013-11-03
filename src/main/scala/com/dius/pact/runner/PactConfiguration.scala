package com.dius.pact.runner

import com.dius.pact.model.Provider
import com.dius.pact.runner.http.Client
import scala.concurrent.ExecutionContext

case class PactConfiguration(providerUrls: Map[String, String], http:Client)(implicit context:ExecutionContext) {

  def setupHook(provider:Provider) = new HttpSetupHook(s"${providerUrls(provider.name)}/setup", http)

  def service(provider:Provider) = new Service(providerUrls(provider.name), http)

}