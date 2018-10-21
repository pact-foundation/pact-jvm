package au.com.dius.pact.provider.broker

import au.com.dius.pact.pactbroker.HalClientBase
import com.google.gson.JsonElement
import groovy.transform.Canonical
import groovy.util.logging.Slf4j

/**
 * HAL client for navigating the HAL links
 */
@Slf4j
@Canonical
class HalClient extends HalClientBase {

  /**
   * @deprecated Use httpClient from the base class
   */
  @Deprecated
  def http

  HalClient(
    String baseUrl,
    Map<String, ?> options) {
    super(baseUrl, options)
  }

  HalClient(String baseUrl) {
    super(baseUrl)
  }

  def methodMissing(String name, args) {
    super.initPathInfo()
    JsonElement matchingLink = super.pathInfo['_links'][name]
    if (matchingLink != null) {
      if (args && args.last() instanceof Closure) {
        if (matchingLink.isJsonArray()) {
          return matchingLink.each(args.last() as Closure)
        }
        return args.last().call(matchingLink)
      }
      return matchingLink
    }
    throw new MissingMethodException(name, this.class, args)
  }

}
