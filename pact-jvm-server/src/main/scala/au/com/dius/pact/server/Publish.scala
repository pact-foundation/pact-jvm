package au.com.dius.pact.server

import au.com.dius.pact.core.model.{OptionalBody, Request, Response}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import scala.io.Source
import java.io.{File, IOException}

import requests.{RequestAuth, RequestFailedException, headers}

object Publish extends StrictLogging {

  def apply(request: Request, oldState: ServerState, config: Config): Result = {
    def jsonBody = JsonUtils.parseJsonString(request.getBody.valueAsString())
    def consumer: Option[String] = getVarFromJson("consumer", jsonBody)
    def consumerVersion: Option[String] = getVarFromJson("consumerVersion", jsonBody)
    def provider: Option[String] = getVarFromJson("provider", jsonBody)
    def broker: Option[String] = getBrokerUrlFromConfig(config)
    def authToken: Option[String] = getVarFromConfig(config.authToken)

    var response = new Response(500, ResponseUtils.CrossSiteHeaders.asJava)
    if (broker.isDefined) {
      if (consumer.isDefined && consumerVersion.isDefined && provider.isDefined) {
        response = publishPact(consumer.get, consumerVersion.get, provider.get, broker.get, authToken)
      } else {
        def errorJson: String = "{\"error\": \"body should contain consumer, consumerVersion and provider.\"}"
        def body: OptionalBody = OptionalBody.body(errorJson.getBytes())
        response = new Response(400, ResponseUtils.CrossSiteHeaders.asJava, body)
      }
    } else {
      def errorJson: String = "{\"error\" : \"Broker url not correctly configured please run server with -b or --broker 'http://pact-broker.adomain.com' option\" }"
      def body: OptionalBody = OptionalBody.body(errorJson.getBytes())
      response = new Response(500, ResponseUtils.CrossSiteHeaders.asJava, body)
    }
    Result(response, oldState)
  }

  private def publishPact(consumer: String, consumerVersion: String, provider: String, broker: String, authToken: Option[String]) = {
    def fileName: String = s"${consumer}-${provider}.json"

    logger.debug("Publishing pact with following details: ")
    logger.debug("Consumer: " + consumer)
    logger.debug("ConsumerVersion: " + consumerVersion)
    logger.debug("Provider: " + provider)
    logger.debug("Pact Broker: " + broker)

    try {
      val content = readContract(fileName)
      def url = s"${broker}/pacts/provider/${provider}/consumer/${consumer}/version/${consumerVersion}"
      var auth: RequestAuth = RequestAuth.Empty
      if (authToken.isDefined) {
        auth = RequestAuth.Bearer(authToken.get)
      }
      def response = requests.put(
        url,
        auth,
        headers = Map("Content-Type" -> "application/json", "Accept" -> "application/hal+json, application/json, */*; q=0.01"),
        data = content
      )
      logger.debug("Statuscode from broker: " + response.statusCode)
      if(response.statusCode > 199 && response.statusCode < 400) {
        logger.debug("Pact succesfully shared. deleting file..")
        removePact(fileName)
      }
      new Response(response.statusCode, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(response.data.array))
    } catch {
      case e: IOException => new Response(500, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(s"""{"error": "Got IO Exception while reading file. ${e.getMessage}"}""".getBytes()))
      case e: RequestFailedException => new Response(e.response.statusCode, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(e.response.data.array))
      case _ => new Response(500, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body("Something unknown happened..".getBytes()))
    }
  }

  private def removePact(fileName: String): Unit = {
    def file = new File(s"${System.getProperty("pact.rootDir", "target/pacts")}/$fileName")
    if (file.exists()) {
      file.delete()
    }
  }

  private def getVarFromConfig(variable: String) = {
    if (!variable.isEmpty) Some(variable)
    else None
  }

  def getBrokerUrlFromConfig(config: Config): Option[String] = {
    if (!config.broker.isEmpty && config.broker.startsWith("http")) Some(config.broker)
    else None
  }

  private def getVarFromJson(variable: String, json: Any): Option[String] = json match {
    case map: Map[AnyRef, AnyRef] => {
      if (map.contains(variable)) Some(map(variable).toString)
      else None
    }
    case _ => None
  }

  def readContract(fileName: String): String = {
    def filePath = s"${System.getProperty("pact.rootDir", "target/pacts")}/$fileName"
    def fileReader = Source.fromFile(filePath)
    def content = fileReader.getLines().mkString
    fileReader.close()
    logger.debug(content)
    content
  }
}
