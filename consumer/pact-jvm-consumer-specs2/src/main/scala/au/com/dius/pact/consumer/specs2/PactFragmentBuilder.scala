package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.{ConsumerPactRunnerKt, PactTestRun, PactVerificationResult}
import au.com.dius.pact.core.model.matchingrules.{MatchingRules, MatchingRulesImpl}
import au.com.dius.pact.core.model._

import scala.collection.JavaConverters._

object PactFragmentBuilder {
  type ConsumerTestVerification[T] = T => Option[T]

  def apply(consumer: Consumer): WithConsumer = {
    WithConsumer(consumer)
  }

  case class WithConsumer(consumer: Consumer) {

    def hasPactWith(provider: String): WithProvider = {
      WithProvider(new Provider(provider))
    }

    case class WithProvider(provider: Provider) {
      def given(state: String): InState = {
        InState(List(new ProviderState(state)))
      }

      def given(state: String, params: Map[String, String]): InState = {
        InState(List(new ProviderState(state)))
      }

      def uponReceiving(description: String): DescribingRequest = {
        InState(List()).uponReceiving(description)
      }

      case class InState(state: List[ProviderState]) {

        def given(stateDesc: String, params: Map[String, String]): InState = {
          InState(state.+:(new ProviderState(stateDesc, params.asJava)))
        }

        def uponReceiving(description: String): DescribingRequest = {
          DescribingRequest(consumer, provider, state, description)
        }
      }
    }
  }

  case class DescribingRequest(consumer: Consumer, provider: Provider, state: List[ProviderState], description: String,
                               builder: CanBuildPactFragment.Builder = CanBuildPactFragment.firstBuild) {
    import scala.collection.JavaConverters._

    def matching(path: String,
                 method: String = "GET",
                 query: String = "",
                 headers: Map[String, List[String]] = Map(),
                 body: String = "",
                 matchers: MatchingRules = new MatchingRulesImpl()): DescribingResponse = {
      DescribingResponse(new Request(method, path, PactReaderKt.queryStringToMap(query), headers.mapValues(f => f.asJava).asJava, OptionalBody.body(body.getBytes),
        matchers))
    }

    case class DescribingResponse(request: Request) {
      import scala.collection.JavaConverters._

      def willRespondWith(status: Int = 200,
                          headers: Map[String, List[String]] = Map(),
                          maybeBody: Option[String] = None,
                          matchers: MatchingRules = new MatchingRulesImpl()): PactWithAtLeastOneRequest = {
        val optionalBody = maybeBody match {
          case Some(body) => OptionalBody.body(body.getBytes)
          case None => OptionalBody.missing()
        }

        builder(
          consumer,
          provider,
          state,
          Seq(new RequestResponseInteraction(
            description,
            state.asJava,
            request,
            new Response(status, headers.mapValues(f => f.asJava).asJava, optionalBody, matchers))))
      }

      def willRespondWith(status: Int,
                          headers: Map[String, List[String]],
                          bodyAndMatchers: DslPart): PactWithAtLeastOneRequest = {
        val rules = new MatchingRulesImpl()
        rules.addCategory(bodyAndMatchers.getMatchers)
        builder(
          consumer,
          provider,
          state,
          Seq(new RequestResponseInteraction(
            description,
            state.asJava,
            request,
            new Response(status, headers.mapValues(f => f.asJava).asJava, OptionalBody.body(bodyAndMatchers.toString.getBytes), rules))))
      }
    }
  }

  case class PactWithAtLeastOneRequest(consumer: Consumer, provider:Provider, state: List[ProviderState], interactions: Seq[RequestResponseInteraction]) {

    def given(): InState = {
      InState(List(), this)
    }

    def given(newState: String): InState = {
      InState(List(new ProviderState(newState)), this)
    }

    def given(state: String, params: Map[String, String]): InState = {
      InState(List(new ProviderState(state, params.asJava)), this)
    }

    def uponReceiving(description: String): DescribingRequest = {
      DescribingRequest(consumer, provider, state, description, CanBuildPactFragment.additionalBuild(this))
    }

    def duringConsumerSpec[R](config: MockProviderConfig)(test: PactTestRun[R]): PactVerificationResult = {
      ConsumerPactRunnerKt.runConsumerTest(asPact(), config, test)
    }

    def asPact(): RequestResponsePact = {
      new RequestResponsePact(provider, consumer, interactions.asJava)
    }

    case class InState(newState: List[ProviderState], pactWithAtLeastOneRequest: PactWithAtLeastOneRequest) {
      def uponReceiving(description: String): DescribingRequest = {
        DescribingRequest(consumer, provider, newState, description, CanBuildPactFragment.additionalBuild(pactWithAtLeastOneRequest))
      }
    }
  }

  object CanBuildPactFragment {
    type Builder = (Consumer, Provider, List[ProviderState], Seq[RequestResponseInteraction]) => PactWithAtLeastOneRequest

    val firstBuild: Builder = PactWithAtLeastOneRequest.apply

    def additionalBuild(existing: PactWithAtLeastOneRequest): Builder = (_,_,_,i) => existing.copy(interactions = existing.interactions ++ i)
  }
}
