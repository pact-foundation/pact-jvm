package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.pactbroker.PactBrokerResult
import au.com.dius.pact.core.pactbroker.util.HttpClientUtils
import au.com.dius.pact.core.pactbroker.util.HttpClientUtils.isJsonResponse
import au.com.dius.pact.core.support.Auth
import au.com.dius.pact.core.support.Utils
import au.com.dius.pact.core.support.HttpClient
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.Version
import au.com.dius.pact.core.support.json.JsonException
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import au.com.dius.pact.core.support.json.map
import au.com.dius.pact.core.support.jsonArray
import au.com.dius.pact.core.support.jsonObject
import au.com.dius.pact.core.support.unwrap
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.expect
import com.github.michaelbull.result.runCatching
import mu.KLogging
import mu.KotlinLogging
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.util.TimeValue
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import kotlin.collections.set
import kotlin.text.isNotEmpty

private val logger = KotlinLogging.logger {}

data class InvalidHttpResponseException(override val message: String) : RuntimeException(message)

fun loadPactFromUrl(
  source: UrlPactSource,
  options: Map<String, Any>,
  http: CloseableHttpClient
): Pair<JsonValue.Object, PactSource> {
  return when (source) {
    is BrokerUrlSource -> {
      val insecureTLS = Utils.lookupInMap(options, "insecureTLS", Boolean::class.java, false)
      val brokerClient = PactBrokerClient(source.pactBrokerUrl, options.toMutableMap(),
        PactBrokerClientConfig(insecureTLS = insecureTLS))
      val pactResponse = brokerClient.fetchPact(source.url, source.encodePath)
      pactResponse.pactFile to source.copy(attributes = pactResponse.links, options = options, tag = source.tag)
    }
    else -> when (val jsonResource = fetchJsonResource(http, source)) {
      is Ok -> if (jsonResource.value.first is JsonValue.Object) {
        jsonResource.value.first.asObject()!! to jsonResource.value.second
      } else {
        throw UnsupportedOperationException("Was expected a JSON document, got ${jsonResource.value}")
      }
      is Err -> throw jsonResource.error
    }
  }
}

@Suppress("ThrowsCount")
fun fetchJsonResource(http: CloseableHttpClient, source: UrlPactSource):
  Result<Pair<JsonValue, UrlPactSource>, Throwable> {
  val url = URL(source.url)
  return runCatching {
    when (url.protocol) {
      "file" -> JsonParser.parseString(URL(source.url).readText()) to source
      else -> {
        val httpGet = HttpGet(HttpClientUtils.buildUrl("", source.url, source.encodePath))
        httpGet.addHeader("Content-Type", "application/json")
        httpGet.addHeader("Accept", "application/hal+json, application/json")

        val response = http.execute(httpGet)
        if (response.code < 300) {
          val contentType = ContentType.parseLenient(response.entity.contentType)
          if (isJsonResponse(contentType)) {
            JsonParser.parseString(EntityUtils.toString(response.entity)) to source
          } else {
            throw InvalidHttpResponseException("Expected a JSON response, but got '$contentType'")
          }
        } else {
          when (response.code) {
            404 -> throw InvalidHttpResponseException("No JSON document found at source '$source'")
            else -> throw InvalidHttpResponseException("Request to source '$source' failed with response " +
              "${response.code}")
          }
        }
      }
    }
  }
}

@Deprecated("Use HttpClient.newHttpClient instead")
fun newHttpClient(baseUrl: String, options: Map<String, Any>): CloseableHttpClient {
  val builder = HttpClients.custom().useSystemProperties()
    .setRetryStrategy(DefaultHttpRequestRetryStrategy(5, TimeValue.ofMilliseconds(3000)))

  when {
    options["authentication"] is Auth -> {
      when (val auth = options["authentication"] as Auth) {
        is Auth.BasicAuthentication -> basicAuth(baseUrl, auth.username, auth.password, builder)
        is Auth.BearerAuthentication -> {
          builder.setDefaultHeaders(listOf(BasicHeader("Authorization", "Bearer " + auth.token)))
        }
      }
    }
    options["authentication"] is List<*> -> {
      val authentication = options["authentication"] as List<*>
      when (val scheme = authentication.first().toString().toLowerCase()) {
        "basic" -> {
          if (authentication.size > 2) {
            basicAuth(baseUrl, authentication[1].toString(), authentication[2].toString(), builder)
          } else {
            logger.warn { "Basic authentication requires a username and password, ignoring." }
          }
        }
        else -> logger.warn { "Only supports basic authentication, got '$scheme', ignoring." }
      }
    }
    options.containsKey("authentication") -> {
      logger.warn { "Authentication options needs to be a Auth class or a list of values, " +
        "got '${options["authentication"]}', ignoring." }
    }
  }

  return builder.build()
}

private fun basicAuth(baseUrl: String, username: String, password: String, builder: HttpClientBuilder) {
  val credsProvider = BasicCredentialsProvider()
  val uri = URI(baseUrl)
  credsProvider.setCredentials(AuthScope(uri.host, uri.port),
    UsernamePasswordCredentials(username, password.toCharArray()))
  builder.setDefaultCredentialsProvider(credsProvider)
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
  fun loadPact(source: Any): Pact

  /**
   * Loads a pact file from either a File or a URL
   * @param source a File or a URL
   * @param options to use when loading the pact
   */
  fun loadPact(source: Any, options: Map<String, Any>): Pact

  /**
   * Parses the JSON into a Pact model
   */
  fun pactFromJson(json: JsonValue.Object, source: PactSource): Pact
}

/**
 * Default implementation of PactReader
 */
object DefaultPactReader : PactReader, KLogging() {

  private const val CLASSPATH_URI_START = "classpath:"

  @JvmStatic
  lateinit var s3Client: Any

  override fun loadPact(source: Any) = loadPact(source, emptyMap())

  override fun loadPact(source: Any, options: Map<String, Any>): Pact {
    val json = loadFile(source, options)
    return pactFromJson(json.first, json.second)
  }

  override fun pactFromJson(json: JsonValue.Object, source: PactSource): Pact {
    val version = determineSpecVersion(json)
    val specVersion = Version.parse(version).expect { "'$version' is not a valid version" }
    return when (specVersion.major) {
      3 -> loadV3Pact(source, json)
      4 -> loadV4Pact(source, json)
      else -> loadV2Pact(source, json)
    }
  }

  @JvmStatic
  fun determineSpecVersion(pactInfo: JsonValue.Object): String {
    var version = "2.0.0"
    if (pactInfo.has("metadata")) {
      val metadata: JsonValue.Object = pactInfo["metadata"].downcast()
      version = when {
        metadata.has("pactSpecificationVersion") -> Json.toString(metadata["pactSpecificationVersion"])
        metadata.has("pactSpecification") -> specVersion(metadata["pactSpecification"], version)
        metadata.has("pact-specification") -> specVersion(metadata["pact-specification"], version)
        else -> version
      }
    }
    return version
  }

  private fun specVersion(specification: JsonValue, defaultVersion: String): String {
    return if (specification is JsonValue.Object && specification.has("version") &&
      specification["version"].isString) {
      specification["version"].asString()!!
    } else {
      return defaultVersion
    }
  }

  @JvmStatic
  fun loadV3Pact(source: PactSource, pactJson: JsonValue.Object): Pact {
    if (pactJson.has("messages")) {
      return MessagePact.fromJson(pactJson, source)
    } else {
      val transformedJson = transformJson(pactJson)
      val provider = Provider.fromJson(transformedJson["provider"])
      val consumer = Consumer.fromJson(transformedJson["consumer"])

      val interactions = transformedJson["interactions"].map { i ->
        val request = extractRequest(i["request"].asObject())
        val response = extractResponse(i["response"].asObject())
        val providerStates = mutableListOf<ProviderState>()
        if (i.has("providerStates")) {
          providerStates.addAll(i["providerStates"].asArray().map { ProviderState.fromJson(it) })
        } else if (i.has("providerState")) {
          providerStates.add(ProviderState(Json.toString(i["providerState"])))
        }
        RequestResponseInteraction(Json.toString(i["description"]), providerStates, request, response,
          Json.toString(i["_id"]))
      }

      return RequestResponsePact(provider, consumer, interactions.toMutableList(),
        BasePact.metaData(transformedJson["metadata"], PactSpecVersion.V3), source)
    }
  }

  @JvmStatic
  fun loadV2Pact(source: PactSource, pactJson: JsonValue.Object): RequestResponsePact {
    val transformedJson = transformJson(pactJson)
    val provider = Provider.fromJson(transformedJson["provider"])
    val consumer = Consumer.fromJson(transformedJson["consumer"])

    val interactions = if (transformedJson.has("interactions"))
      transformedJson["interactions"].asArray().map { i ->
        val request = extractRequest(i["request"].asObject())
        val response = extractResponse(i["response"].asObject())
        RequestResponseInteraction(Json.toString(i["description"]),
          if (i.has("providerState"))
            listOf(ProviderState(Json.toString(i["providerState"])))
          else
            emptyList(),
          request, response, Json.toString(i["_id"]))
      }
    else emptyList()

    return RequestResponsePact(provider, consumer, interactions.toMutableList(),
      BasePact.metaData(transformedJson["metadata"], PactSpecVersion.V2), source)
  }

  @JvmStatic
  fun loadV4Pact(source: PactSource, pactJson: JsonValue.Object): Pact {
    val provider = Provider.fromJson(pactJson["provider"])
    val consumer = Consumer.fromJson(pactJson["consumer"])

    val interactions = if (pactJson.has("interactions") && pactJson["interactions"].isArray)
      pactJson["interactions"].asArray()!!.values.mapIndexed { i, interaction ->
        V4Interaction.interactionFromJson(i, interaction, source).unwrap()
      }
    else emptyList()

    return V4Pact(consumer, provider, interactions.toMutableList(), BasePact.metaData(pactJson["metadata"],
      PactSpecVersion.V4), source)
  }

  @JvmStatic
  fun extractResponse(responseJson: JsonValue.Object?): Response {
    return if (responseJson != null) {
      formatBody(responseJson)
      Response.fromJson(responseJson)
    } else {
      Response()
    }
  }

  @JvmStatic
  fun extractRequest(requestJson: JsonValue.Object?): Request {
    return if (requestJson != null) {
      formatBody(requestJson)
      Request.fromJson(requestJson)
    } else {
      Request()
    }
  }

  private fun formatBody(json: JsonValue) {
    if (json is JsonValue.Object && json.has("body")) {
      val body = json["body"]
      if (body !is JsonValue.Null && body !is JsonValue.StringValue) {
        json["body"] = body.serialise()
      }
    }
  }

  @JvmStatic
  fun transformJson(pactJson: JsonValue.Object): JsonValue.Object {
    if (pactJson.has("interactions") && pactJson["interactions"] is JsonValue.Array) {
      pactJson["interactions"] = jsonArray(pactJson["interactions"].asArray().map { i ->
        if (i is JsonValue.Object) {
          val interaction = jsonObject(i.entries.entries.map { entry ->
            when (entry.key) {
              "provider_state" -> "providerState" to entry.value
              "request" -> "request" to transformRequestResponseJson(entry.value.asObject())
              "response" -> "response" to transformRequestResponseJson(entry.value.asObject())
              else -> entry.toPair()
            }
          })
          interaction
        } else {
          i
        }
      })
    }

    if (pactJson.has("metadata") && pactJson["metadata"] is JsonValue.Object) {
      pactJson["metadata"] = jsonObject(pactJson["metadata"].asObject()!!.entries.entries.map { entry ->
        when (entry.key) {
          "pact-specification" -> "pactSpecification" to entry.value
          else -> entry.toPair()
        }
      })
    }

    return pactJson
  }

  private fun transformRequestResponseJson(requestJson: JsonValue.Object?): JsonValue.Object? {
    return if (requestJson != null) {
      jsonObject(requestJson.entries.entries.map { (k, v) ->
        when (k) {
          "responseMatchingRules" -> "matchingRules" to v
          "requestMatchingRules" -> "matchingRules" to v
          "method" -> "method" to Json.toString(v).toUpperCase()
          else -> k to v
        }
      })
    } else {
      null
    }
  }

  @Suppress("ReturnCount")
  private fun loadFile(source: Any, options: Map<String, Any> = emptyMap()): Pair<JsonValue.Object, PactSource> {
    if (source is ClosurePactSource) {
      return loadFile(source.closure.get(), options)
    } else if (source is FileSource) {
      return source.file.bufferedReader().use { JsonParser.parseReader(it).downcast<JsonValue.Object>() to source }
    } else if (source is InputStream || source is Reader || source is File) {
      return loadPactFromFile(source)
    } else if (source is BrokerUrlSource) {
      val insecureTLS = Utils.lookupInMap(options, "insecureTLS", Boolean::class.java, false)
      return HttpClient.newHttpClient(
        options["authentication"],
        URI(source.pactBrokerUrl),
        insecureTLS = insecureTLS
      ).first.use {
        loadPactFromUrl(source, options, it)
      }
    } else if (source is PactBrokerResult) {
      val insecureTLS = Utils.lookupInMap(options, "insecureTLS", Boolean::class.java, false)
      return HttpClient.newHttpClient(
        options["authentication"],
        URI(source.pactBrokerUrl),
        insecureTLS = insecureTLS
      ).first.use {
        loadPactFromUrl(BrokerUrlSource.fromResult(source, options, source.tag), options, it)
      }
    } else if (source is URL || source is UrlPactSource) {
      val urlSource = if (source is URL) UrlSource(source.toString()) else source as UrlPactSource
      return loadPactFromUrl(urlSource, options, newHttpClient(urlSource.url, options))
    } else if (source is String && source.toLowerCase().matches(Regex("(https?|file)://?.*"))) {
      val urlSource = UrlSource(source)
      return loadPactFromUrl(urlSource, options, newHttpClient(urlSource.url, options))
    } else if (source is String && source.toLowerCase().matches(Regex("s3://.*"))) {
      return loadPactFromS3Bucket(source)
    } else if (source is String && source.startsWith(CLASSPATH_URI_START)) {
      return loadPactFromClasspath(source.substring(CLASSPATH_URI_START.length))
    } else if (source is String && fileExists(source)) {
      val file = File(source)
      return file.bufferedReader().use { JsonParser.parseReader(it).downcast<JsonValue.Object>() to FileSource(file) }
    } else {
      try {
        return JsonParser.parseString(source.toString()).downcast<JsonValue.Object>() to UnknownPactSource
      } catch (e: JsonException) {
        throw UnsupportedOperationException(
          "Unable to load pact file from '$source' as it is neither a json document, file, input stream, " +
          "reader or an URL", e)
      }
    }
  }

  private fun loadPactFromFile(source: Any): Pair<JsonValue.Object, PactSource> {
    return when (source) {
      is InputStream -> JsonParser.parseReader(InputStreamReader(source)).downcast<JsonValue.Object>() to
        InputStreamPactSource
      is Reader -> JsonParser.parseReader(source).downcast<JsonValue.Object>() to ReaderPactSource
      is File -> source.bufferedReader().use {
        JsonParser.parseReader(it).downcast<JsonValue.Object>() } to FileSource(source)
      else -> throw IllegalArgumentException("loadPactFromFile expects either an InputStream, Reader or File. " +
        "Got a ${source.javaClass.name} instead")
    }
  }

  private fun loadPactFromS3Bucket(source: String): Pair<JsonValue.Object, PactSource> {
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
    return JsonParser.parseReader(InputStreamReader(objectContent)).downcast<JsonValue.Object>() to S3PactSource(source)
  }

  private fun loadPactFromClasspath(source: String): Pair<JsonValue.Object, PactSource> {
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(source)
    if (inputStream == null) {
      throw IllegalStateException("not found on classpath: $source")
    }
    return inputStream.use { loadPactFromFile(it) }
  }

  private fun fileExists(path: String) = File(path).exists()
}
