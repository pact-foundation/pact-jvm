package au.com.dius.pact.core.support

import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.net.URIBuilder
import java.net.URI

object HttpClientUtils {
  val URL_REGEX = Regex("([^:]+):\\/\\/([^\\/:]+)(:\\d+)?(.*)")

  /**
   * Constructs a URI from a base URL plus a URL path
   * @param baseUrl The base URL for relative paths. If using absolute URLs, pass an empty string
   * @param url The URL. If a path, it will be relative to the base URL
   * @param encodePath If the path should be URI encoded, defaults to true
   */
  @JvmOverloads
  fun buildUrl(baseUrl: String, url: String, encodePath: Boolean = true): URI {
    val match = URL_REGEX.matchEntire(url)
    return if (match != null) {
      val (scheme, host, port, path) = match.destructured
      val builder = URIBuilder().setScheme(scheme).setHost(host)
      if (port.isNotEmpty()) {
        builder.port = port.substring(1).toInt()
      }
      if (encodePath) {
        builder.setPath(path).build()
      } else {
        URI(builder.build().toString() + path)
      }
    } else {
      if (encodePath) {
        val builder = URIBuilder(baseUrl)
        pathCombiner(builder, url)
      } else {
        URI(baseUrl + url).normalize()
      }
    }
  }

  fun pathCombiner(builder: URIBuilder, url: String): URI {
    val path = builder.path
    return if (path != null) {
      if (path.endsWith("/") && url.startsWith("/")) {
        builder.setPath(path.trimEnd('/') + url).build()
      } else {
        builder.setPath(path + url).build()
      }
    } else {
      builder.setPath(url).build()
    }
  }

  fun isJsonResponse(contentType: ContentType) = contentType.mimeType == "application/json" ||
    contentType.mimeType == "application/hal+json"
}
