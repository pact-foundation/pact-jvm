package au.com.dius.pact.core.model

import au.com.dius.pact.com.github.michaelbull.result.Err
import au.com.dius.pact.com.github.michaelbull.result.Ok
import au.com.dius.pact.com.github.michaelbull.result.Result
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.pactbroker.CustomServiceUnavailableRetryStrategy
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.util.HttpClientUtils
import au.com.dius.pact.core.pactbroker.util.HttpClientUtils.isJsonResponse
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.AmazonS3URI
import com.github.salomonbrys.kotson.toMap
import com.github.zafarkhaja.semver.Version
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import groovy.json.JsonException
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import mu.KLogging
import mu.KotlinLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.net.URL
import java.net.URLDecoder

private val logger = KotlinLogging.logger {}

private val ACCEPT_JSON = mutableMapOf("requestProperties" to mutableMapOf("Accept" to "application/json"))

data class InvalidHttpResponseException(override val message: String) : RuntimeException(message)

fun loadPactFromUrl(source: UrlPactSource, options: Map<String, Any>, http: CloseableHttpClient?): Pair<Map<String, Any>, PactSource> {
  return when (source) {
    is BrokerUrlSource -> {
      val brokerClient = PactBrokerClient(source.pactBrokerUrl, options)
      val pactResponse = brokerClient.fetchPact(source.url)
      pactResponse.pactFile as Map<String, Any> to source.copy(attributes = pactResponse.links, options = options)
    }
    else -> if (options.containsKey("authentication")) {
      val jsonResource = fetchJsonResource(http!!, source)
      when (jsonResource) {
        is Ok -> jsonResource.value.first.asJsonObject.toMap() to jsonResource.value.second
        is Err -> throw jsonResource.error
      }
    } else {
      JsonSlurper().parse(URL(source.url), ACCEPT_JSON) as Map<String, Any> to source
    }
  }
}

fun fetchJsonResource(http: CloseableHttpClient, source: UrlPactSource):
  Result<Pair<JsonElement, UrlPactSource>, Exception> {
  return Result.of {
    val httpGet = HttpGet(HttpClientUtils.buildUrl("", source.url, true))
    httpGet.addHeader("Content-Type", "application/json")
    httpGet.addHeader("Accept", "application/hal+json, application/json")

    val response = http.execute(httpGet)
    if (response.statusLine.statusCode < 300) {
      val contentType = ContentType.getOrDefault(response.entity)
      if (isJsonResponse(contentType)) {
        return@of JsonParser().parse(EntityUtils.toString(response.entity)) to source
      } else {
        throw InvalidHttpResponseException("Expected a JSON response, but got '$contentType'")
      }
    } else {
      when (response.statusLine.statusCode) {
        404 -> throw InvalidHttpResponseException("No JSON document found at source '$source'")
        else -> throw InvalidHttpResponseException("Request to source '$source' failed with response " +
          "'${response.statusLine}'")
      }
    }
  }
}

fun newHttpClient(baseUrl: String, options: Map<String, Any>): CloseableHttpClient {
  val retryStrategy = CustomServiceUnavailableRetryStrategy(5, 3000)
  val builder = HttpClients.custom().useSystemProperties().setServiceUnavailableRetryStrategy(retryStrategy)

  if (options["authentication"] is List<*>) {
    val authentication = options["authentication"] as List<*>
    val scheme = authentication.first().toString().toLowerCase()
    when (scheme) {
      "basic" -> {
        if (authentication.size > 2) {
          val credsProvider = BasicCredentialsProvider()
          val uri = URI(baseUrl)
          credsProvider.setCredentials(AuthScope(uri.host, uri.port),
            UsernamePasswordCredentials(authentication[1].toString(), authentication[2].toString()))
          builder.setDefaultCredentialsProvider(credsProvider)
        } else {
          logger.warn { "Basic authentication requires a username and password, ignoring." }
        }
      }
      else -> logger.warn { "Only supports basic authentication, got '$scheme', ignoring." }
    }
  } else if (options.containsKey("authentication")) {
    logger.warn { "Authentication options needs to be a list of values, got '${options["authentication"]}', ignoring." }
  }

  return builder.build()
}

/**
 * Parses the query string into a Map
 */
@JvmOverloads
fun queryStringToMap(query: String?, decode: Boolean = true): Map<String, List<String>> {
  return if (query.isNullOrEmpty()) {
    emptyMap()
  } else {
    query!!.split("&").filter { it.isNotEmpty() }.map { val nv = it.split("=", limit = 2); nv[0] to nv[1] }
      .fold(mutableMapOf<String, MutableList<String>>()) { map, nameAndValue ->
      val name = if (decode) URLDecoder.decode(nameAndValue.first, "UTF-8") else nameAndValue.first
      val value = if (decode) URLDecoder.decode(nameAndValue.second, "UTF-8") else nameAndValue.second
      if (map.containsKey(name)) {
        map[name]!!.add(value)
      } else {
        map[name] = mutableListOf(value)
      }
      map
    }
  }
}

/**
 * Class to load a Pact from a JSON source using a version strategy
 */
object PactReader: KLogging() {

  const val CLASSPATH_URI_START = "classpath:"

  /**
   * Loads a pact file from either a File or a URL
   * @param source a File or a URL
   */
  @JvmOverloads
  @JvmStatic
  fun loadPact(options: Map<String, Any> = emptyMap(), source: Any): Pact<out Interaction> {
    val pactInfo = loadFile(source, options)
    var version = "2.0.0"
    val metadata = pactInfo.first["metadata"] as Map<String, Any>?
    val specification = metadata?.get("pactSpecification") ?: metadata?.get("pact-specification")
    if (specification is Map<*, *> && specification.containsKey("version")) {
      version = specification["version"].toString()
    }
    if (version == "3.0") {
      version = "3.0.0"
    }
    val specVersion = Version.valueOf(version)
    return when (specVersion.majorVersion) {
      3 -> loadV3Pact(pactInfo.second, pactInfo.first.toMutableMap())
      else -> loadV2Pact(pactInfo.second, pactInfo.first.toMutableMap())
    }
  }

  fun loadV3Pact(source: PactSource, pactJson: MutableMap<String, Any>): Pact<out Interaction> {
    if (pactJson.containsKey("messages")) {
      return MessagePact.fromMap(pactJson, source)
    } else {
      val transformedJson = transformJson(pactJson)
      val provider = Provider.fromMap(transformedJson["provider"] as Map<String, Any>? ?: emptyMap())
      val consumer = Consumer.fromMap(transformedJson["consumer"] as Map<String, Any>? ?: emptyMap())

      val interactions = (transformedJson["interactions"] as List<MutableMap<String, Any>>).map { i ->
        val request = extractRequest(i["request"] as MutableMap<String, Any>)
        val response = extractResponse(i["response"] as MutableMap<String, Any>)
        val providerStates = mutableListOf<ProviderState>()
        if (i.containsKey("providerStates")) {
          providerStates.addAll((i["providerStates"] as List<Map<String, Any>>).map { ProviderState.fromMap(it) })
        } else if (i.containsKey("providerState")) {
          providerStates.add(ProviderState(i["providerState"].toString()))
        }
        RequestResponseInteraction(i["description"].toString(), providerStates, request, response)
      }

      return RequestResponsePact(provider, consumer, interactions.toMutableList(), emptyMap(), source)
    }
  }

  fun loadV2Pact(source: PactSource, pactJson: MutableMap<String, Any>): Pact<out Interaction> {
    val transformedJson = transformJson(pactJson)
    val provider = Provider.fromMap(transformedJson["provider"] as Map<String, Any>? ?: emptyMap())
    val consumer = Consumer.fromMap(transformedJson["consumer"] as Map<String, Any>? ?: emptyMap())

    val interactions = (transformedJson["interactions"] as List<MutableMap<String, Any>>).map { i ->
      val request = extractRequest(i["request"] as MutableMap<String, Any>)
      val response = extractResponse(i["response"] as MutableMap<String, Any>)
      RequestResponseInteraction(i["description"].toString(),
        if (i.containsKey("providerState")) listOf(ProviderState(i["providerState"].toString())) else emptyList(),
        request, response)
    }

    return RequestResponsePact(provider, consumer, interactions.toMutableList(), emptyMap(), source)
  }

  fun extractResponse(responseJson: MutableMap<String, Any>): Response {
    extractBody(responseJson)
    return Response.fromMap(responseJson)
  }

  fun extractRequest(requestJson: MutableMap<String, Any>): Request {
    extractBody(requestJson)
    return Request.fromMap(requestJson)
  }

  private fun extractBody(json: MutableMap<String, Any>) {
    if (json.containsKey("body") && json["body"] != null && json["body"] !is String) {
      json["body"] = JsonOutput.toJson(json["body"])
    }
  }

  fun transformJson(pactJson: MutableMap<String, Any>): Map<String, Any> {
    pactJson["interactions"] = (pactJson["interactions"] as List<Map<String, Any>>).map { i ->
      val interaction = i.entries.associate { entry ->
        when (entry.key) {
          "provider_state" -> "providerState" to entry.value
          "request" -> "request" to transformRequestResponseJson(entry.value as MutableMap<String, Any>)
          "response" -> "response" to transformRequestResponseJson(entry.value as MutableMap<String, Any>)
          else -> entry.toPair()
        }
      }.toMutableMap()
      if (i.containsKey("providerState") && i["providerState"] != null) {
        interaction["providerState"] = i["providerState"] as Any
      }
      interaction
    }
    return pactJson
  }

  private fun transformRequestResponseJson(requestJson: MutableMap<String, Any>): MutableMap<String, Any> {
    return requestJson.entries.associate { (k, v) ->
      when (k) {
        "responseMatchingRules" -> "matchingRules" to v
        "requestMatchingRules" -> "matchingRules" to v
        "method" -> "method" to v.toString().toUpperCase()
        else -> k to v
      }
    }.toMutableMap()
  }

  private fun loadFile(source: Any, options: Map<String, Any> = emptyMap()): Pair<Map<String, Any>, PactSource> {
    if (source is ClosurePactSource) {
      return loadFile(source.closure.get(), options)
    } else if (source is FileSource<*>) {
      return JsonSlurper().parse(source.file) as Map<String, Any> to source
    } else if (source is InputStream || source is Reader || source is File) {
      return loadPactFromFile(source)
    } else if (source is BrokerUrlSource) {
      return loadPactFromUrl(source, options, null)
    } else if (source is URL || source is UrlPactSource) {
      val urlSource = if (source is URL) UrlSource<Interaction>(source.toString()) else source as UrlPactSource
      return loadPactFromUrl(urlSource, options, newHttpClient(urlSource.url, options))
    } else if (source is String && source.toLowerCase().matches(Regex("(https?|file)://?.*"))) {
      val urlSource = UrlSource<Interaction>(source)
      return loadPactFromUrl(urlSource, options, newHttpClient(urlSource.url, options))
    } else if (source is String && source.toLowerCase().matches(Regex("s3://.*"))) {
      return loadPactFromS3Bucket(source, options)
    } else if (source is String && source.startsWith(CLASSPATH_URI_START)) {
      return loadPactFromClasspath(source.substring(CLASSPATH_URI_START.length))
    } else if (source is String && fileExists(source)) {
      val file = File(source)
      return JsonSlurper().parse(file) as Map<String, Any> to FileSource<Interaction>(file)
    } else {
      try {
        return JsonSlurper().parseText(source.toString()) as Map<String, Any> to UnknownPactSource
      } catch (e: JsonException) {
        throw UnsupportedOperationException(
          "Unable to load pact file from '$source' as it is neither a json document, file, input stream, " +
          "reader or an URL", e)
      }
    }
  }

  private fun loadPactFromFile(source: Any): Pair<Map<String, Any>, PactSource> {
    return when (source) {
      is InputStream -> JsonSlurper().parse(source) as Map<String, Any> to InputStreamPactSource
      is Reader -> JsonSlurper().parse(source) as Map<String, Any> to ReaderPactSource
      is File -> JsonSlurper().parse(source) as Map<String, Any> to FileSource<Interaction>(source)
      else -> throw IllegalArgumentException("loadPactFromFile expects either an InputStream, Reader or File. " +
        "Got a ${source.javaClass.name} instead")
    }
  }

  private fun loadPactFromS3Bucket(source: String, options: Map<String, Any>): Pair<Map<String, Any>, PactSource> {
    val s3Uri = AmazonS3URI(source)
    val client = s3Client()
    val s3Pact = client.getObject(s3Uri.bucket, s3Uri.key)
    return JsonSlurper().parse(s3Pact.objectContent) as Map<String, Any> to S3PactSource(source)
  }

  private fun loadPactFromClasspath(source: String): Pair<Map<String, Any>, PactSource> {
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(source)
    if (inputStream == null) {
      throw IllegalStateException("not found on classpath: $source")
    }
    return inputStream.use { loadPactFromFile(it) }
  }

  private fun fileExists(path: String) = File(path).exists()

  private fun s3Client() = AmazonS3ClientBuilder.defaultClient()
}
