package au.com.dius.pact.model

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.{ConsumerTestVerification, VerificationResult}
import au.com.dius.pact.model.matchingrules.MatchingRules
import org.json.JSONObject

import scala.collection.JavaConverters._

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
object PactFragmentBuilder {
  def apply(consumer: Consumer) = {
    WithConsumer(consumer)
  }

  case class WithConsumer(consumer: Consumer) {
    import scala.collection.JavaConversions._

    def hasPactWith(provider: String) = {
      WithProvider(new Provider(provider))
    }

    case class WithProvider(provider: Provider) {
      def given(state: String) = {
        InState(List(new ProviderState(state)))
      }

      def given(state: String, params: Map[String, String]) = {
        InState(List(new ProviderState(state)))
      }

      def uponReceiving(description: String) = {
        InState(List()).uponReceiving(description)
      }

      case class InState(state: List[ProviderState]) {

        def given(stateDesc: String, params: Map[String, String]) = {
          InState(state.+:(new ProviderState(stateDesc, params)))
        }

        def uponReceiving(description: String) = {
          DescribingRequest(consumer, provider, state, description)
        }
      }
    }
  }

  case class DescribingRequest(consumer: Consumer, provider: Provider, state: List[ProviderState], description: String,
                               builder: CanBuildPactFragment.Builder = CanBuildPactFragment.firstBuild) {
    import scala.collection.JavaConversions._

    /**
     * supports java DSL
     */
    def matching(path: String, method: String, query: String, headers: java.util.Map[String, String], body: String,
                 matchers: java.util.Map[String, Any]): DescribingResponse = {
      import collection.JavaConversions._
      matching(path, method, query, headers.toMap, body, matchers.toMap.asInstanceOf[Map[String, Map[String, String]]])
    }

    def matching(path: String,
                 method: String = "GET",
                 query: String = "",
                 headers: Map[String, String] = Map(),
                 body: String = "",
                 matchers: MatchingRules = new MatchingRules()): DescribingResponse = {
      DescribingResponse(new Request(method, path, PactReader.queryStringToMap(query), headers, OptionalBody.body(body),
        matchers))
    }

    case class DescribingResponse(request: Request) {
      /**
       * supports java DSL
       */
      def willRespondWith(status: Int, headers: java.util.Map[String, String], maybeBody: Option[String], matchers: JSONObject): PactWithAtLeastOneRequest = {
        import collection.JavaConversions._
        willRespondWith(status, headers.toMap, maybeBody, matchers)
      }

      def willRespondWith(status: Int = 200,
                          headers: Map[String, String] = Map(),
                          maybeBody: Option[String] = None,
                          matchers: MatchingRules = new MatchingRules()): PactWithAtLeastOneRequest = {
        val optionalBody = maybeBody match {
          case Some(body) => OptionalBody.body(body)
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

      def willRespondWith(status: Int,
                          headers: Map[String, String],
                          bodyAndMatchers: DslPart): PactWithAtLeastOneRequest = {
        val rules = new MatchingRules()
        rules.addCategory(bodyAndMatchers.getMatchers)
        builder(
          consumer,
          provider,
          state,
          Seq(new RequestResponseInteraction(
            description,
            state.asJava,
            request,
            new Response(status, headers, OptionalBody.body(bodyAndMatchers.toString), rules))))
      }
    }
  }

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

  object CanBuildPactFragment {
    type Builder = (Consumer, Provider, List[ProviderState], Seq[RequestResponseInteraction]) => PactWithAtLeastOneRequest

    val firstBuild: Builder = PactWithAtLeastOneRequest.apply

    def additionalBuild(existing: PactWithAtLeastOneRequest): Builder = (_,_,_,i) => existing.copy(interactions = existing.interactions ++ i)
  }
}
