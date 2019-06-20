package au.com.dius.pact.provider.maven

import au.com.dius.pact.model.UrlSource
import au.com.dius.pact.provider.ConsumerInfo
import groovy.transform.ToString

/**
 * Consumer Info for maven projects
 */
@ToString(includeSuperProperties = true)
class Consumer extends ConsumerInfo {

  URL getPactUrl() {
    if (pactSource instanceof UrlSource) {
      new URL(pactSource.url)
    } else {
      new URL(pactSource.toString())
    }
  }

  void setPactUrl(URL pactUrl) {
    pactSource = new UrlSource(pactUrl.toString())
  }
}
