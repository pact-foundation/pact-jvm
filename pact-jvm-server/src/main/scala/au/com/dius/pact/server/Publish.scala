package au.com.dius.pact.server

import au.com.dius.pact.core.model.{OptionalBody, Request, Response}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._
import java.io.{File, IOException}

import au.com.dius.pact.core.pactbroker.{PactBrokerClient, RequestFailedException}

object Publish extends StrictLogging {

  def apply(request: Request, oldState: ServerState, config: Config): Result = {
    val jsonBody = JsonUtils.parseJsonString(request.getBody.valueAsString())
    val consumer: Option[String] = getVarFromJson("consumer", jsonBody)
    val consumerVersion: Option[String] = getVarFromJson("consumerVersion", jsonBody)
    val provider: Option[String] = getVarFromJson("provider", jsonBody)
    val tags: Option[::[String]] = getListFromJson("tags", jsonBody)
    val broker: Option[String] = getBrokerUrlFromConfig(config)
    val authToken: Option[String] = getVarFromConfig(config.authToken)

    var response = new Response(500, ResponseUtils.CrossSiteHeaders.asJava)
    if (broker.isDefined) {
      if (consumer.isDefined && consumerVersion.isDefined && provider.isDefined) {
        response = publishPact(consumer.get, consumerVersion.get, provider.get, broker.get, authToken, tags)
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

  private def publishPact(consumer: String, consumerVersion: String, provider: String, broker: String, authToken: Option[String], tags: Option[::[String]]) = {
    val fileName: String = s"$consumer-$provider.json"
    val pact = new File(s"${System.getProperty("pact.rootDir", "target/pacts")}/$fileName")

    logger.debug("Publishing pact with following details: ")
    logger.debug("Consumer: " + consumer)
    logger.debug("ConsumerVersion: " + consumerVersion)
    logger.debug("Provider: " + provider)
    logger.debug("Pact Broker: " + broker)
    logger.debug("Tags: " + tags.getOrElse(None))

    try {
      val options = getOptions(authToken)
      val brokerClient: PactBrokerClient = new PactBrokerClient(broker, options.asJava)
      val res = brokerClient.uploadPactFile(pact, consumerVersion, tags.getOrElse(List()).asJava)
      if( res.component2() == null) {
        logger.debug("Pact succesfully shared. deleting file..")
        removePact(pact)
        new Response(200, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(res.component1().getBytes()))
      } else {
        new Response(500, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(res.component2().getLocalizedMessage.getBytes()))
      }

    } catch {
      case e: IOException => new Response(500, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(s"""{"error": "Got IO Exception while reading file. ${e.getMessage}"}""".getBytes()))
      case e: RequestFailedException => new Response(e.getStatus.getStatusCode, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(e.getBody.getBytes()))
      case t: Throwable => new Response(500, ResponseUtils.CrossSiteHeaders.asJava, OptionalBody.body(t.getMessage.getBytes()))
    }
  }

  private def getOptions(authToken: Option[String]): Map[String, _] = {
    var options: Map[String, _]= Map()
    if(authToken.isDefined) {
      options = Map("authentication" -> List("bearer",authToken.get).asJava)
    }
    options
  }



  private def removePact(file: File): Unit = {
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

  private def getListFromJson(variable: String, json: Any): Option[::[String]] = json match {
    case map: Map[AnyRef, AnyRef] => {
      if (map.contains(variable)) Some(map(variable).asInstanceOf[::[String]])
      else None
    }
    case _ => None
  }

}
