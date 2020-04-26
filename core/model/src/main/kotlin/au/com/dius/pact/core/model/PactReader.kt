package au.com.dius.pact.core.model

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.util.HttpClientUtils
import au.com.dius.pact.core.pactbroker.util.HttpClientUtils.isJsonResponse
import au.com.dius.pact.core.support.CustomServiceUnavailableRetryStrategy
import au.com.dius.pact.core.support.HttpClient
import au.com.dius.pact.core.support.Json
import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.set
import com.github.salomonbrys.kotson.string
import com.github.zafarkhaja.semver.Version
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
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
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import kotlin.collections.set

private val logger = KotlinLogging.logger {}

data class InvalidHttpResponseException(override val message: String) : RuntimeException(message)

fun loadPactFromUrl(
  source: UrlPactSource,
  options: Map<String, Any>,
  http: CloseableHttpClient
): Pair<JsonElement, PactSource> {
  return when (source) {
    is BrokerUrlSource -> {
      val brokerClient = PactBrokerClient(source.pactBrokerUrl, options)
      val pactResponse = brokerClient.fetchPact(source.url, source.encodePath)
      pactResponse.pactFile to source.copy(attributes = pactResponse.links, options = options)
    }
    else -> when (val jsonResource = fetchJsonResource(http, source)) {
      is Ok -> jsonResource.value.first.obj to jsonResource.value.second
      is Err -> throw jsonResource.error
    }
  }
}

@Suppress("ThrowsCount")
fun fetchJsonResource(http: CloseableHttpClient, source: UrlPactSource):
  Result<Pair<JsonElement, UrlPactSource>, Exception> {
  val url = URL(source.url)
  return Result.of {
    when (url.protocol) {
      "file" -> {
        JsonParser.parseString(URL(source.url).readText()) to source
      }
      else -> {
        val httpGet = HttpGet(HttpClientUtils.buildUrl("", source.url, source.encodePath))
        httpGet.addHeader("Content-Type", "application/json")
        httpGet.addHeader("Accept", "application/hal+json, application/json")

        val response = http.execute(httpGet)
        if (response.statusLine.statusCode < 300) {
          val contentType = ContentType.getOrDefault(response.entity)
          if (isJsonResponse(contentType)) {
            return@of JsonParser.parseString(EntityUtils.toString(response.entity)) to source
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
  }
}

fun newHttpClient(baseUrl: String, options: Map<String, Any>): CloseableHttpClient {
  val retryStrategy = CustomServiceUnavailableRetryStrategy(5, 3000)
  val builder = HttpClients.custom().useSystemProperties().setServiceUnavailableRetryStrategy(retryStrategy)

  if (options["authentication"] is List<*>) {
    val authentication = options["authentication"] as List<*>
    when (val scheme = authentication.first().toString().toLowerCase()) {
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
    query.split("&")
      .filter { it.isNotEmpty() }.map { val nv = it.split("=", limit = 2); nv[0] to nv[1] }
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
interface PactReader {
  /**
   * Loads a pact file from either a File or a URL
   * @param source a File or a URL
   */
  fun loadPact(source: Any): Pact<*>

  /**
   * Loads a pact file from either a File or a URL
   * @param source a File or a URL
   * @param options to use when loading the pact
   */
  fun loadPact(source: Any, options: Map<String, Any>): Pact<*>
}

/**
 * Default implementation of PactReader
 */
object DefaultPactReader : PactReader, KLogging() {

  private const val CLASSPATH_URI_START = "classpath:"

  @JvmStatic
  lateinit var s3Client: Any

  override fun loadPact(source: Any) = loadPact(source, emptyMap())

  override fun loadPact(source: Any, options: Map<String, Any>): Pact<*> {
    val pactInfo = loadFile(source, options)
    val version = determineSpecVersion(pactInfo.first)
    val specVersion = Version.valueOf(version)
    return when (specVersion.majorVersion) {
      3 -> loadV3Pact(pactInfo.second, pactInfo.first.obj)
      else -> loadV2Pact(pactInfo.second, pactInfo.first.obj)
    }
  }

  @JvmStatic
  fun determineSpecVersion(pactInfo: JsonElement): String {
    var version = "2.0.0"
    if (pactInfo.obj.has("metadata")) {
      val metadata = pactInfo.obj["metadata"].obj
      version = when {
        metadata.has("pactSpecificationVersion") -> metadata["pactSpecificationVersion"].string
        metadata.has("pactSpecification") -> specVersion(metadata["pactSpecification"], version)
        metadata.has("pact-specification") -> specVersion(metadata["pact-specification"], version)
        else -> version
      }
    }
    if (version == "3.0") {
      version = "3.0.0"
    }
    return version
  }

  private fun specVersion(specification: JsonElement, defaultVersion: String): String {
    return if (specification.isJsonObject && specification.obj.has("version") &&
      specification.obj["version"].isJsonPrimitive) {
      specification.obj["version"].string
    } else {
      return defaultVersion
    }
  }

  @JvmStatic
  fun loadV3Pact(source: PactSource, pactJson: JsonObject): Pact<*> {
    if (pactJson.has("messages")) {
      return MessagePact.fromJson(pactJson, source)
    } else {
      val transformedJson = transformJson(pactJson)
      val provider = Provider.fromJson(transformedJson["provider"])
      val consumer = Consumer.fromJson(transformedJson["consumer"])

      val interactions = transformedJson["interactions"].array.map { i ->
        val request = extractRequest(i.obj["request"].obj)
        val response = extractResponse(i.obj["response"].obj)
        val providerStates = mutableListOf<ProviderState>()
        if (i.obj.has("providerStates")) {
          providerStates.addAll(i["providerStates"].array.map { ProviderState.fromJson(it) })
        } else if (i.obj.has("providerState")) {
          providerStates.add(ProviderState(Json.toString(i["providerState"])))
        }
        RequestResponseInteraction(Json.toString(i["description"]), providerStates, request, response,
          Json.toString(i.obj["_id"]))
      }

      return RequestResponsePact(provider, consumer, interactions.toMutableList(),
        BasePact.metaData(transformedJson["metadata"], PactSpecVersion.V3), source)
    }
  }

  @JvmStatic
  fun loadV2Pact(source: PactSource, pactJson: JsonObject): Pact<*> {
    val transformedJson = transformJson(pactJson)
    val provider = Provider.fromJson(transformedJson["provider"])
    val consumer = Consumer.fromJson(transformedJson["consumer"])

    val interactions = if (transformedJson.has("interactions"))
      transformedJson["interactions"].array.map { i ->
        val request = extractRequest(i.obj["request"].obj)
        val response = extractResponse(i.obj["response"].obj)
        RequestResponseInteraction(Json.toString(i["description"]),
          if (i.obj.has("providerState"))
            listOf(ProviderState(Json.toString(i.obj["providerState"])))
          else
            emptyList(),
          request, response, Json.toString(i.obj["_id"]))
      }
    else emptyList()

    return RequestResponsePact(provider, consumer, interactions.toMutableList(),
      BasePact.metaData(transformedJson["metadata"], PactSpecVersion.V2), source)
  }

  @JvmStatic
  fun extractResponse(responseJson: JsonObject): Response {
    formatBody(responseJson)
    return Response.fromJson(responseJson)
  }

  @JvmStatic
  fun extractRequest(requestJson: JsonObject): Request {
    formatBody(requestJson)
    return Request.fromJson(requestJson)
  }

  private fun formatBody(json: JsonElement) {
    if (json.isJsonObject && json.obj.has("body")) {
      val body = json.obj["body"]
      if (!body.isJsonNull && !body.isJsonPrimitive || (body.isJsonPrimitive && !body.asJsonPrimitive.isString)) {
        json.obj["body"] = body.toString()
      }
    }
  }

  @JvmStatic
  fun transformJson(pactJson: JsonObject): JsonObject {
    if (pactJson.has("interactions") && pactJson["interactions"].isJsonArray) {
      pactJson["interactions"] = jsonArray(pactJson["interactions"].array.map { i ->
        if (i.isJsonObject) {
          val interaction = jsonObject(i.obj.entrySet().map { entry ->
            when (entry.key) {
              "provider_state" -> "providerState" to entry.value
              "request" -> "request" to transformRequestResponseJson(entry.value.obj)
              "response" -> "response" to transformRequestResponseJson(entry.value.obj)
              else -> entry.toPair()
            }
          })
          interaction
        } else {
          i
        }
      })
    }

    if (pactJson.has("metadata") && pactJson["metadata"].isJsonObject) {
      pactJson["metadata"] = jsonObject(pactJson["metadata"].obj.entrySet().map { entry ->
        when (entry.key) {
          "pact-specification" -> "pactSpecification" to entry.value
          else -> entry.toPair()
        }
      })
    }

    return pactJson
  }

  private fun transformRequestResponseJson(requestJson: JsonObject): JsonObject {
    return jsonObject(requestJson.entrySet().map { (k, v) ->
      when (k) {
        "responseMatchingRules" -> "matchingRules" to v
        "requestMatchingRules" -> "matchingRules" to v
        "method" -> "method" to Json.toString(v).toUpperCase()
        else -> k to v
      }
    })
  }

  @Suppress("ReturnCount")
  private fun loadFile(source: Any, options: Map<String, Any> = emptyMap()): Pair<JsonElement, PactSource> {
    if (source is ClosurePactSource) {
      return loadFile(source.closure.get(), options)
    } else if (source is FileSource<*>) {
      return source.file.bufferedReader().use { JsonParser.parseReader(it) to source }
    } else if (source is InputStream || source is Reader || source is File) {
      return loadPactFromFile(source)
    } else if (source is BrokerUrlSource) {
      return HttpClient.newHttpClient(options["authentication"], URI(source.pactBrokerUrl), mutableMapOf()).first.use {
        loadPactFromUrl(source, options, it)
      }
    } else if (source is URL || source is UrlPactSource) {
      val urlSource = if (source is URL) UrlSource<Interaction>(source.toString()) else source as UrlPactSource
      return loadPactFromUrl(urlSource, options, newHttpClient(urlSource.url, options))
    } else if (source is String && source.toLowerCase().matches(Regex("(https?|file)://?.*"))) {
      val urlSource = UrlSource<Interaction>(source)
      return loadPactFromUrl(urlSource, options, newHttpClient(urlSource.url, options))
    } else if (source is String && source.toLowerCase().matches(Regex("s3://.*"))) {
      return loadPactFromS3Bucket(source)
    } else if (source is String && source.startsWith(CLASSPATH_URI_START)) {
      return loadPactFromClasspath(source.substring(CLASSPATH_URI_START.length))
    } else if (source is String && fileExists(source)) {
      val file = File(source)
      return file.bufferedReader().use { JsonParser.parseReader(it) to FileSource<Interaction>(file) }
    } else {
      try {
        return JsonParser.parseString(source.toString()) to UnknownPactSource
      } catch (e: JsonSyntaxException) {
        throw UnsupportedOperationException(
          "Unable to load pact file from '$source' as it is neither a json document, file, input stream, " +
          "reader or an URL", e)
      }
    }
  }

  private fun loadPactFromFile(source: Any): Pair<JsonElement, PactSource> {
    return when (source) {
      is InputStream -> JsonParser.parseReader(InputStreamReader(source)) to InputStreamPactSource
      is Reader -> JsonParser.parseReader(source) to ReaderPactSource
      is File -> source.bufferedReader().use { JsonParser.parseReader(it) } to FileSource<Interaction>(source)
      else -> throw IllegalArgumentException("loadPactFromFile expects either an InputStream, Reader or File. " +
        "Got a ${source.javaClass.name} instead")
    }
  }

  private fun loadPactFromS3Bucket(source: String): Pair<JsonElement, PactSource> {
    val amazonS3URIClass = Class.forName("com.amazonaws.services.s3.AmazonS3URI")
    val s3Uri = amazonS3URIClass.getConstructor(String::class.java).newInstance(source)
    val bucket = amazonS3URIClass.getMethod("getBucket").invoke(s3Uri).toString()
    val key = amazonS3URIClass.getMethod("getKey").invoke(s3Uri).toString()
    if (!DefaultPactReader::s3Client.isInitialized) {
      val amazonS3ClientBuilderClass = Class.forName("com.amazonaws.services.s3.AmazonS3ClientBuilder")
      s3Client = amazonS3ClientBuilderClass.getMethod("defaultClient").invoke(null)
    }
    val s3ClientClass = Class.forName("com.amazonaws.services.s3.AmazonS3")
    val s3Pact = s3ClientClass.getMethod("getObject", String::class.java, String::class.java)
      .invoke(s3Client, bucket, key)
    val s3ObjectClass = Class.forName("com.amazonaws.services.s3.model.S3Object")
    val objectContent = s3ObjectClass.getMethod("getObjectContent").invoke(s3Pact) as InputStream
    return JsonParser.parseReader(InputStreamReader(objectContent)) to S3PactSource(source)
  }

  private fun loadPactFromClasspath(source: String): Pair<JsonElement, PactSource> {
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(source)
    if (inputStream == null) {
      throw IllegalStateException("not found on classpath: $source")
    }
    return inputStream.use { loadPactFromFile(it) }
  }

  private fun fileExists(path: String) = File(path).exists()
}
