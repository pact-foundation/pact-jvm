package com.dius.pact.runner

case class PactConfiguration(providerBaseUrl: String, stateChangeUrl: String, timeoutSeconds: Int)