package au.com.dius.pact.provider.junitsupport.loader

/**
 * Allows the Pact URL to be overridden (for example, when verifying a Pact from a Webhook call)
 */
interface OverrideablePactLoader : PactLoader {
  fun overridePactUrl(pactUrl: String, consumer: String)
}
