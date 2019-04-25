package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer.{MockHttpServer, MockHttpServerKt, PactTestExecutionContext}
import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model._
import au.com.dius.pact.core.model.matchingrules.{MatchingRules, MatchingRulesImpl}
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

import scala.collection.JavaConverters._

trait UnitSpecsSupport extends Specification {

  def pactFragment: RequestResponsePact

  protected lazy val pact = pactFragment
  protected val providerConfig = MockProviderConfig.createDefault(PactSpecVersion.V3)
  protected val server = new MockHttpServer(pact, providerConfig)
  protected val context = new PactTestExecutionContext()

  override def map(fragments: => Fragments) = {
    step(server.start()) ^
    step(server.waitForServer()) ^
      fragments ^
      step(server.stop()) ^
      fragmentFactory.example(
        "Should match all mock server records",
        VerificationResultAsResult(server.verifyResultAndWritePact(true, context, pact, PactSpecVersion.V3))
      )
  }

  def buildRequest(path: String,
                   method: String = "GET",
                   query: String = "",
                   headers: Map[String, String] = Map(),
                   body: String = "",
                   matchers: MatchingRules = new MatchingRulesImpl()): Request =
    new Request(method, path, PactReaderKt.queryStringToMap(query), headers.mapValues(v => List(v).asJava).asJava,
      OptionalBody.body(body.getBytes), matchers)

  def buildResponse(status: Int = 200,
                    headers: Map[String, String] = Map(),
                    maybeBody: Option[String] = None,
                    matchers: MatchingRules = new MatchingRulesImpl()): Response = {
    val optionalBody = maybeBody match {
      case Some(body) => OptionalBody.body(body.getBytes)
      case None => OptionalBody.missing()
    }

    new Response(status, headers.mapValues(v => List(v).asJava).asJava, optionalBody, matchers)
  }

  def buildResponse(status: Int,
                    headers: Map[String, String],
                    bodyAndMatchers: DslPart): Response = {
    val matchers = new MatchingRulesImpl()
    matchers.addCategory(bodyAndMatchers.getMatchers)
    new Response(status, headers.mapValues(v => List(v).asJava).asJava, OptionalBody.body(bodyAndMatchers.toString.getBytes), matchers)
  }

  def buildInteraction(description: String, states: List[ProviderState], request: Request, response: Response): RequestResponseInteraction =
    new RequestResponseInteraction(description, states.asJava, request, response)

  def buildPactFragment(consumer: String, provider: String, interactions: List[RequestResponseInteraction]): RequestResponsePact =
    new RequestResponsePact(new Provider(provider), new Consumer(consumer), interactions.asJava)

  def description(pact: RequestResponsePact) = s"Consumer '${pact.getConsumer.getName}' has a pact with Provider '${pact.getProvider.getName}': " +
    pact.getInteractions.asScala.map { i => i.getDescription }.mkString(" and ") + sys.props("line.separator")
}
