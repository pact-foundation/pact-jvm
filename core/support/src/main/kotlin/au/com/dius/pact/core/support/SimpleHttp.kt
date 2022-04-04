package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.Json.toMap
import au.com.dius.pact.core.support.json.JsonParser
import org.apache.hc.client5.http.classic.methods.HttpDelete
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI

/**
 * Simple HTTP class to support tests needing HTTP requests
 */
class SimpleHttp(private val baseUrl: String) {
  private var client: CloseableHttpClient = HttpClients.custom().useSystemProperties().build()

  @JvmOverloads
  fun get(
    path: String = "/",
    query: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap()
  ): Response {
    val httpGet = HttpGet(buildUrl(baseUrl, path, query))
    for ((key, value) in  headers) {
      httpGet.addHeader(key, value)
    }
    return Response(client.execute(httpGet))
  }

  @JvmOverloads
  fun post(
    path: String,
    body: String,
    contentType: String,
    headers: Map<String, String> = emptyMap()
  ): Response {
    val httpPost = HttpPost(buildUrl(baseUrl, path, emptyMap()))
    for ((key, value) in  headers) {
      httpPost.addHeader(key, value)
    }
    httpPost.entity = StringEntity(body, ContentType.parse(contentType))
    return Response(client.execute(httpPost))
  }

  @JvmOverloads
  fun put(
    path: String,
    body: String,
    contentType: String,
    headers: Map<String, String> = emptyMap()
  ): Response {
    val httpPut = HttpPut(buildUrl(baseUrl, path, emptyMap()))
    for ((key, value) in  headers) {
      httpPut.addHeader(key, value)
    }
    httpPut.entity = StringEntity(body, ContentType.parse(contentType))
    return Response(client.execute(httpPut))
  }

  fun delete(path: String): Response {
    val httpDelete = HttpDelete(buildUrl(baseUrl, path, emptyMap()))
    return Response(client.execute(httpDelete))
  }

  companion object {
    fun buildUrl(base: String, path: String, query: Map<String, String>): URI {
      val queryStr = if (query.isNotEmpty()) {
        "?" + query.entries.joinToString("&") { "${it.key}=${it.value}" }
      } else ""
      return HttpClientUtils.buildUrl(base, path + queryStr, false)
    }
  }
}

class Response(val response: CloseableHttpResponse) {
  val statusCode = response.code
  val hasBody = response.entity != null && response.entity.contentLength > 0
  val contentLength = response.entity?.contentLength ?: 0
  val contentType: String = if (response.entity != null && response.entity.contentType != null) {
    response.entity.contentType
  } else "text/plain"

  fun getReader(): Reader {
    return InputStreamReader(response.entity.content)
  }

  fun getInputStream(): InputStream {
    return response.entity.content
  }

  fun getHeaders(): Map<String, List<String>> {
    val headers = mutableMapOf<String, MutableList<String>>()
    for (header in response.headers) {
      if (headers.containsKey(header.name)) {
        headers[header.name]!!.add(header.value)
      } else {
        headers[header.name] = mutableListOf(header.value)
      }
    }
    return headers
  }

  fun bodyToMap(): Map<String, Any?> {
    return when (response.entity.contentType) {
      "application/x-www-form-urlencoded" -> {
        response.entity.content.reader().readText().split('&').map {
          val values = it.split('=', limit = 2)
          values[0] to values[1]
        }.associate { it }
      }
      "application/json" -> {
        toMap(JsonParser.parseStream(response.entity.content))
      }
      else -> emptyMap()
    }
  }
}
