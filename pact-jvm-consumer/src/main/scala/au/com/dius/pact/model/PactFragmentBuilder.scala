package au.com.dius.pact.model

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.{ConsumerTestVerification, VerificationResult}
import au.com.dius.pact.model.matchingrules.{MatchingRules, MatchingRulesImpl}
import org.json.JSONObject

import scala.collection.JavaConverters._

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
object PactFragmentBuilder {
  @Deprecated
  def apply(consumer: Consumer) = {
    WithConsumer(consumer)
  }

  @Deprecated
  case class WithConsumer(consumer: Consumer) {
    import scala.collection.JavaConversions._

    @Deprecated
    def hasPactWith(provider: String) = {
      WithProvider(new Provider(provider))
    }

    @Deprecated
    case class WithProvider(provider: Provider) {
      @Deprecated
      def given(state: String) = {
        InState(List(new ProviderState(state)))
      }

      @Deprecated
      def given(state: String, params: Map[String, String]) = {
        InState(List(new ProviderState(state)))
      }

      @Deprecated
      def uponReceiving(description: String) = {
        InState(List()).uponReceiving(description)
      }

      @Deprecated
      case class InState(state: List[ProviderState]) {

        @Deprecated
        def given(stateDesc: String, params: Map[String, String]) = {
          InState(state.+:(new ProviderState(stateDesc, params)))
        }

        @Deprecated
        def uponReceiving(description: String) = {
          DescribingRequest(consumer, provider, state, description)
        }
      }
    }
  }

  @Deprecated
  case class DescribingRequest(consumer: Consumer, provider: Provider, state: List[ProviderState], description: String,
                               builder: CanBuildPactFragment.Builder = CanBuildPactFragment.firstBuild) {
    import scala.collection.JavaConversions._

    /**
     * supports java DSL
     */
    @Deprecated
    def matching(path: String, method: String, query: String, headers: java.util.Map[String, String], body: String,
                 matchers: java.util.Map[String, Any]): DescribingResponse = {
      import collection.JavaConversions._
      matching(path, method, query, headers.toMap, body, matchers.toMap.asInstanceOf[Map[String, Map[String, String]]])
    }

    @Deprecated
    def matching(path: String,
                 method: String = "GET",
                 query: String = "",
                 headers: Map[String, String] = Map(),
                 body: String = "",
                 matchers: MatchingRules = new MatchingRulesImpl()): DescribingResponse = {
      DescribingResponse(new Request(method, path, PactReaderKt.queryStringToMap(query), headers, OptionalBody.body(body.getBytes),
        matchers))
    }

    @Deprecated
    case class DescribingResponse(request: Request) {
      /**
       * supports java DSL
       */
      @Deprecated
      def willRespondWith(status: Int, headers: java.util.Map[String, String], maybeBody: Option[String], matchers: JSONObject): PactWithAtLeastOneRequest = {
        import collection.JavaConversions._
        willRespondWith(status, headers.toMap, maybeBody, matchers)
      }

      @Deprecated
      def willRespondWith(status: Int = 200,
                          headers: Map[String, String] = Map(),
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
            new Response(status, headers, optionalBody, matchers))))
      }

      @Deprecated
      def willRespondWith(status: Int,
                          headers: Map[String, String],
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
            new Response(status, headers, OptionalBody.body(bodyAndMatchers.toString.getBytes), rules))))
      }
    }
  }

  @Deprecated
  case class PactWithAtLeastOneRequest(consumer: Consumer, provider:Provider, state: List[ProviderState], interactions: Seq[RequestResponseInteraction]) {
    import scala.collection.JavaConversions._

    def given() = {
      InState(List(), this)
    }

    def given(newState: String) = {
      InState(List(new ProviderState(newState)), this)
    }

    def given(state: String, params: Map[String, String]) = {
      InState(List(new ProviderState(state, params)), this)
    }

    def uponReceiving(description: String) = {
      DescribingRequest(consumer, provider, state, description, CanBuildPactFragment.additionalBuild(this))
    }

    def duringConsumerSpec[T](config: MockProviderConfig)(test: => T, verification: ConsumerTestVerification[T]): VerificationResult = {
      PactFragment(consumer, provider, interactions).duringConsumerSpec(config)(test, verification)
    }

    def asPactFragment() = {
      PactFragment(consumer, provider, interactions)
    }

    case class InState(newState: List[ProviderState], pactWithAtLeastOneRequest: PactWithAtLeastOneRequest) {
      def uponReceiving(description: String) = {
        DescribingRequest(consumer, provider, newState, description, CanBuildPactFragment.additionalBuild(pactWithAtLeastOneRequest))
      }
    }
  }

  @Deprecated
  object CanBuildPactFragment {
    type Builder = (Consumer, Provider, List[ProviderState], Seq[RequestResponseInteraction]) => PactWithAtLeastOneRequest

    val firstBuild: Builder = PactWithAtLeastOneRequest.apply

    def additionalBuild(existing: PactWithAtLeastOneRequest): Builder = (_,_,_,i) => existing.copy(interactions = existing.interactions ++ i)
  }
}
