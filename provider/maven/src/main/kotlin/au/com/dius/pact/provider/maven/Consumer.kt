package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.PactVerification
import java.net.URL

/**
 * Consumer Info for maven projects
 */
class Consumer(
  override var name: String = "",
  override var stateChange: Any? = null,
  override var stateChangeUsesBody: Boolean = true,
  override var packagesToScan: List<String> = emptyList(),
  override var verificationType: PactVerification? = null,
  override var pactSource: Any? = null,
  override var pactFileAuthentication: List<Any?> = emptyList()
) : ConsumerInfo(name, stateChange, stateChangeUsesBody, packagesToScan, verificationType, pactSource, pactFileAuthentication) {

  fun getPactUrl() = if (pactSource is UrlSource<*>) {
    URL((pactSource as UrlSource<*>).url)
  } else {
    URL(pactSource.toString())
  }

  fun setPactUrl(pactUrl: URL) {
    pactSource = UrlSource<Interaction>(pactUrl.toString())
  }
}
