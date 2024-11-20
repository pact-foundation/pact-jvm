package au.com.dius.pact.server

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.pactbroker.IPactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig
import au.com.dius.pact.core.pactbroker.RequestFailedException
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.json.JsonParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

object Publish {

  private val CrossSiteHeaders = mapOf("Access-Control-Allow-Origin" to listOf("*"))

  @JvmStatic
  fun apply(request: Request, oldState: ServerState, config: Config): Result {
    val jsonBody = Json.fromJson(JsonParser.parseString(request.body.valueAsString()))
    val consumer = getVarFromJson("consumer", jsonBody)
    val consumerVersion = getVarFromJson("consumerVersion", jsonBody)
    val provider = getVarFromJson("provider", jsonBody)
    val tags = getListFromJson("tags", jsonBody)
    val broker = getBrokerUrlFromConfig(config)
    val authToken = getVarFromConfig(config.authToken)

    var response = Response(500, CrossSiteHeaders.toMutableMap())
    if (broker != null) {
      if (consumer != null && consumerVersion != null && provider != null) {
        val options = getOptions(authToken)
        val brokerClient = PactBrokerClient(broker, options.toMutableMap(), PactBrokerClientConfig())
        response = publishPact(consumer, consumerVersion, provider, broker, brokerClient, tags)
      } else {
        val errorJson = "{\"error\": \"body should contain consumer, consumerVersion and provider.\"}"
        val body = OptionalBody.body(errorJson.toByteArray())
        response = Response(400, CrossSiteHeaders.toMutableMap(), body)
      }
    } else {
      val errorJson = "{\"error\" : \"Broker url not correctly configured please run server with -b or --broker 'http://pact-broker.adomain.com' option\" }"
      val body = OptionalBody.body(errorJson.toByteArray())
      response = Response(500, CrossSiteHeaders.toMutableMap(), body)
    }
    return Result(response, oldState)
  }

  fun publishPact(consumer: String, consumerVersion: String, provider: String, broker: String, brokerClient: IPactBrokerClient, tags: List<String>?): Response {
    val fileName = "$consumer-$provider.json"
    val pact = File("${System.getProperty("pact.rootDir", "target/pacts")}/$fileName")

    logger.debug { "Publishing pact with following details: " }
    logger.debug { "Consumer: $consumer" }
    logger.debug { "ConsumerVersion: $consumerVersion" }
    logger.debug { "Provider: $provider" }
    logger.debug { "Pact Broker: $broker" }
    logger.debug { "Tags: $tags" }

    return try {
      val res = brokerClient.uploadPactFile(pact, consumerVersion, tags.orEmpty())
      if (res.errorValue() == null) {
        logger.debug { "Pact successfully shared. deleting file.." }
        removePact(pact)
        Response(200, CrossSiteHeaders.toMutableMap(), OptionalBody.body(res.get()!!.toByteArray()))
      } else {
        Response(500, CrossSiteHeaders.toMutableMap(), OptionalBody.body(res.errorValue()!!.localizedMessage.toByteArray()))
      }
    } catch (e: IOException) {
      Response(500, CrossSiteHeaders.toMutableMap(), OptionalBody.body("{\"error\": \"Got IO Exception while reading file. ${e.message}\"}".toByteArray()))
    } catch (e: RequestFailedException) {
      Response(e.status, CrossSiteHeaders.toMutableMap(), OptionalBody.body(e.body?.toByteArray()))
    } catch (t: Throwable) {
      Response(500, CrossSiteHeaders.toMutableMap(), OptionalBody.body(t.message?.toByteArray()))
    }
  }

  fun getOptions(authToken: String?): Map<String, Any> {
    var options = mapOf<String, Any>()
    if (authToken != null) {
      options = mapOf("authentication" to listOf("bearer", authToken))
    }
    return options
  }

  private fun removePact(file: File) {
    if (file.exists()) {
      file.delete()
    }
  }

  fun getVarFromConfig(variable: String) =
    if (!variable.isEmpty()) variable
    else null

  fun getBrokerUrlFromConfig(config: Config) =
    if (config.broker.isNotEmpty() && config.broker.startsWith("http")) config.broker
    else null

  fun getVarFromJson(variable: String, json: Any?) = when(json) {
    is Map<*, *> -> {
      if (json.contains(variable)) json[variable].toString()
      else null
    }
    else -> null
  }

  fun getListFromJson(variable: String, json: Any?): List<String>? = when(json) {
    is Map<*, *> -> {
      if (json.contains(variable)) json[variable] as List<String>
      else null
    }
    else -> null
  }
}
