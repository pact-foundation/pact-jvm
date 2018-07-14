package au.com.dius.pact.provider

import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Request
import groovy.json.JsonBuilder
import groovy.lang.Binding
import groovy.lang.Closure
import groovy.lang.GroovyShell
import mu.KLogging
import org.apache.http.Consts
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpOptions
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpTrace
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import scala.Function1
import java.lang.Boolean.getBoolean
import java.lang.reflect.Modifier
import java.net.URI
import java.net.URL
import java.net.URLDecoder

interface IHttpClientFactory {
  fun newClient(provider: Any?): CloseableHttpClient
}

interface IProviderInfo {
  val protocol: String
  val host: Any?
  val port: Any?
  val path: String
  val name: String

  val requestFilter: Any?
  val stateChangeRequestFilter: Any?
  val stateChangeUrl: URL?
  val stateChangeUsesBody: Boolean
  val stateChangeTeardown: Boolean
}

interface IConsumerInfo {
  val stateChange: Any?
  val stateChangeUsesBody: Boolean
}

/**
 * Client HTTP utility for providers
 */
open class ProviderClient(
  val provider: IProviderInfo,
  private val httpClientFactory: IHttpClientFactory
) {

  companion object : KLogging() {
    const val CONTENT_TYPE = "Content-Type"
    const val UTF8 = "UTF-8"
    const val REQUEST = "request"
    const val ACTION = "action"

    private fun invokeIfClosure(property: Any?) = if (property is Closure<*>) {
      property.call()
    } else {
      property
    }

    private fun convertToInteger(port: Any?) = if (port is Number) {
      port.toInt()
    } else {
      Integer.parseInt(port.toString())
    }

    @JvmStatic
    fun urlEncodedFormPost(request: Request) = request.method.toLowerCase() == "post" &&
      request.mimeType() == ContentType.APPLICATION_FORM_URLENCODED.mimeType

    private fun isFunctionalInterface(requestFilter: Any) =
      requestFilter::class.java.interfaces.any { it.isAnnotationPresent(FunctionalInterface::class.java) }

    @JvmStatic
    private fun stripTrailingSlash(basePath: String): String {
      return when {
        basePath == "/" -> ""
        basePath.isNotEmpty() && basePath.last() == '/' -> basePath.substring(0, basePath.length - 1)
        else -> basePath
      }
    }
  }

  open fun makeRequest(request: Request): Map<String, Any> {
    val httpclient = getHttpClient()
    val method = prepareRequest(request)
    return executeRequest(httpclient, method)
  }

  open fun executeRequest(httpclient: CloseableHttpClient, method: HttpUriRequest): Map<String, Any> {
    return httpclient.execute(method).use {
      handleResponse(it)
    }
  }

  open fun prepareRequest(request: Request): HttpUriRequest {
    logger.debug { "Making request for provider $provider:" }
    logger.debug { request.toString() }

    val method = newRequest(request)
    setupHeaders(request, method)
    setupBody(request, method)

    executeRequestFilter(method)

    return method
  }

  open fun executeRequestFilter(method: HttpRequest) {
    val requestFilter = provider.requestFilter
    if (requestFilter != null) {
      when (requestFilter) {
        is Closure<*> -> requestFilter.call(method)
        is Function1<*, *> -> (requestFilter as Function1<HttpRequest, *>).apply(method)
        is org.apache.commons.collections4.Closure<*> -> (requestFilter as org.apache.commons.collections4.Closure<Any>).execute(method)
        else -> {
          if (isFunctionalInterface(requestFilter)) {
            invokeJavaFunctionalInterface(requestFilter, method)
          } else {
            val binding = Binding()
            binding.setVariable(REQUEST, method)
            val shell = GroovyShell(binding)
            shell.evaluate(requestFilter as String)
          }
        }
      }
    }
  }

  private fun invokeJavaFunctionalInterface(functionalInterface: Any, httpRequest: HttpRequest) {
    val invokableMethods = functionalInterface::class.java.declaredMethods.filter { Modifier.isPublic(it.modifiers) }
    if (invokableMethods.size == 1) {
      val method = invokableMethods.first()
      val params = arrayOfNulls<Any?>(method.parameterCount)
      if (params.isNotEmpty()) {
        params[0] = httpRequest
      }
      method.isAccessible = true
      method.invoke(functionalInterface, *params)
      return
    }

    throw IllegalArgumentException("Java request filters must be either a Consumer or Function that takes at " +
      "least one HttpRequest parameter")
  }

  open fun setupBody(request: Request, method: HttpRequest) {
    if (method is HttpEntityEnclosingRequest) {
      if (urlEncodedFormPost(request) && request.query != null && request.query.isNotEmpty()) {
        val charset = Consts.UTF_8
        val parameters = request.query.flatMap { entry -> entry.value.map { BasicNameValuePair(entry.key, it) } }
        method.entity = UrlEncodedFormEntity(parameters, charset)
      } else if (request.body != null && request.body!!.isPresent()) {
        method.entity = StringEntity(request.body!!.orElse(""))
      }
    }
  }

  open fun setupHeaders(request: Request, method: HttpRequest) {
    val headers = request.headers
    if (headers != null && headers.isNotEmpty()) {
      headers.forEach { key, value ->
        method.addHeader(key, value)
      }
    }

    if (!method.containsHeader(CONTENT_TYPE) && request.body?.isPresent() == true) {
      method.addHeader(CONTENT_TYPE, "application/json")
    }
  }

  open fun makeStateChangeRequest(
    stateChangeUrl: Any?,
    state: ProviderState,
    postStateInBody: Boolean,
    isSetup: Boolean,
    stateChangeTeardown: Boolean
  ): CloseableHttpResponse? {
    return if (stateChangeUrl != null) {
      val httpclient = getHttpClient()
      val urlBuilder = if (stateChangeUrl is URI) {
        URIBuilder(stateChangeUrl)
      } else {
        URIBuilder(stateChangeUrl.toString())
      }
      val method: HttpPost?

      if (postStateInBody) {
        method = HttpPost(urlBuilder.build())
        val map = mutableMapOf<String, Any>("state" to state.name)
        if (state.params.isNotEmpty()) {
          map["params"] = state.params
        }
        if (stateChangeTeardown) {
          map["action"] = if (isSetup) "setup" else "teardown"
        }
        method.entity = StringEntity(JsonBuilder(map).toPrettyString(), ContentType.APPLICATION_JSON)
      } else {
        urlBuilder.setParameter("state", state.name)
        state.params.forEach { k, v -> urlBuilder.setParameter(k, v.toString()) }
        if (stateChangeTeardown) {
          if (isSetup) {
            urlBuilder.setParameter(ACTION, "setup")
          } else {
            urlBuilder.setParameter(ACTION, "teardown")
          }
        }
        method = HttpPost(urlBuilder.build())
      }

      if (provider.stateChangeRequestFilter != null) {
        when {
          provider.stateChangeRequestFilter is Closure<*> -> (provider.stateChangeRequestFilter as Closure<*>).call(method)
          provider.stateChangeRequestFilter is Function1<*, *> -> (provider.stateChangeRequestFilter as Function1<Any, Any>).apply(method)
          else -> {
            val binding = Binding()
            binding.setVariable(REQUEST, method)
            val shell = GroovyShell(binding)
            shell.evaluate(provider.stateChangeRequestFilter.toString())
          }
        }
      }

      httpclient.execute(method)
    } else {
      null
    }
  }

  fun getHttpClient() = httpClientFactory.newClient(provider)

  private fun handleResponse(httpResponse: HttpResponse): Map<String, Any> {
    logger.debug { "Received response: ${httpResponse.statusLine}" }
    val response = mutableMapOf<String, Any>("statusCode" to httpResponse.statusLine.statusCode)

    response["headers"] = httpResponse.allHeaders.associate { header -> header.name to header.value }

    val entity = httpResponse.entity
    if (entity != null) {
      val contentType = if (entity.contentType != null) {
        ContentType.parse(entity.contentType.value)
      } else {
        ContentType.APPLICATION_JSON
      }
      response["contentType"] = contentType
      response["data"] = EntityUtils.toString(entity, contentType.charset?.name() ?: UTF8)
    }

    logger.debug { "Response: $response" }

    return response
  }

  open fun newRequest(request: Request): HttpUriRequest {
    val scheme = provider.protocol
    val host = invokeIfClosure(provider.host)
    val port = convertToInteger(invokeIfClosure(provider.port))
    var path = stripTrailingSlash(provider.path)

    var urlBuilder = URIBuilder()
    if (systemPropertySet("pact.verifier.disableUrlPathDecoding")) {
      path += request.path
      urlBuilder = URIBuilder("$scheme://$host:$port$path")
    } else {
      path += URLDecoder.decode(request.path, UTF8)
      urlBuilder.scheme = provider.protocol
      urlBuilder.host = invokeIfClosure(provider.host)?.toString()
      urlBuilder.port = convertToInteger(invokeIfClosure(provider.port))
      urlBuilder.path = path
    }

    if (request.query != null && !urlEncodedFormPost(request)) {
      request.query.forEach { entry ->
        entry.value.forEach {
          urlBuilder.addParameter(entry.key, it)
        }
      }
    }

    val url = urlBuilder.build().toString()
    return when (request.method.toLowerCase()) {
      "post" -> HttpPost(url)
      "put" -> HttpPut(url)
      "options" -> HttpOptions(url)
      "delete" -> HttpDelete(url)
      "head" -> HttpHead(url)
      "patch" -> HttpPatch(url)
      "trace" -> HttpTrace(url)
      else -> HttpGet(url)
    }
  }

  open fun systemPropertySet(property: String) = getBoolean(property)
}
