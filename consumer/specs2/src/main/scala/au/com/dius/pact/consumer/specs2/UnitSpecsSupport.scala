package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.{MockHttpServer, PactTestExecutionContext}
import au.com.dius.pact.core.model._
import au.com.dius.pact.core.model.matchingrules.{MatchingRules, MatchingRulesImpl}
import org.specs2.execute.Success
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

import scala.jdk.CollectionConverters._

trait UnitSpecsSupport extends Specification {

  def pactFragment: RequestResponsePact

  protected val providerConfig: MockProviderConfig = MockProviderConfig.createDefault(PactSpecVersion.V3)
  protected lazy val mockHttpServer = new MockHttpServer(pactFragment, providerConfig)
  protected val context = new PactTestExecutionContext()

  override def map(fragments: => Fragments): Fragments = {
    step(mockHttpServer.start()) ^
    step(mockHttpServer.waitForServer()) ^
      fragments ^
      step(mockHttpServer.stop()) ^
      fragmentFactory.example(
        "Should match all mock server records",
        VerificationResultAsResult(mockHttpServer.verifyResultAndWritePact(Success(), context, mockHttpServer.getPact, PactSpecVersion.V3))
      )
  }

  def buildRequest(path: String,
                   method: String = "GET",
                   query: String = "",
                   headers: Map[String, String] = Map(),
                   body: String = "",
                   matchers: MatchingRules = new MatchingRulesImpl()): Request =
    new Request(method, path, PactReaderKt.queryStringToMap(query), headers.view.mapValues(v => List(v).asJava).toMap.asJava,
      OptionalBody.body(body.getBytes), matchers)

  def buildResponse(status: Int = 200,
                    headers: Map[String, String] = Map(),
                    maybeBody: Option[String] = None,
                    matchers: MatchingRules = new MatchingRulesImpl()): Response = {
    val optionalBody = maybeBody match {
      case Some(body) => OptionalBody.body(body.getBytes)
      case None => OptionalBody.missing()
    }

    new Response(status, headers.view.mapValues(v => List(v).asJava).toMap.asJava, optionalBody, matchers)
  }

  def buildResponse(status: Int,
                    headers: Map[String, String],
                    bodyAndMatchers: DslPart): Response = {
    val matchers = new MatchingRulesImpl()
    matchers.addCategory(bodyAndMatchers.getMatchers)
    new Response(status, headers.view.mapValues(v => List(v).asJava).toMap.asJava, OptionalBody.body(bodyAndMatchers.toString.getBytes), matchers)
  }

  def buildInteraction(description: String, states: List[ProviderState], request: Request, response: Response): RequestResponseInteraction =
    new RequestResponseInteraction(description, states.asJava, request, response, null)

  def buildPactFragment(consumer: String, provider: String, interactions: List[RequestResponseInteraction]): RequestResponsePact =
    new RequestResponsePact(new Provider(provider), new Consumer(consumer), interactions.asJava)

  def description(pact: RequestResponsePact): String = s"Consumer '${pact.getConsumer.getName}' has a pact with Provider '${pact.getProvider.getName}': " +
    pact.getInteractions.asScala.map { i => i.getDescription }.mkString(" and ") + sys.props("line.separator")
}
