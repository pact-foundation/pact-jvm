package au.com.dius.pact.model

import au.com.dius.pact.model.HttpMethod._
import org.json4s._
import au.com.dius.pact.consumer.{ConsumerTestVerification, VerificationResult}
import org.json.JSONObject

object PactFragmentBuilder {
  def apply(consumer: Consumer) = {
    WithConsumer(consumer)
  }

  case class WithConsumer(consumer: Consumer) {
    def hasPactWith(provider: String) = {
      WithProvider(Provider(provider))
    }

    case class WithProvider(provider: Provider) {
      def given(state: String) = {
        InState(Some(state))
      }

      def uponReceiving(description: String) = {
        InState(None).uponReceiving(description)
      }

      case class InState(state: Option[String]) {
        def uponReceiving(description: String) = {
          DescribingRequest(consumer, provider, state, description)
        }
      }
    }
  }

  case class DescribingRequest(consumer: Consumer, provider: Provider, state: Option[String], description: String, builder: CanBuildPactFragment.Builder = CanBuildPactFragment.firstBuild) extends Optionals {

    /**
     * supports java DSL
     */
    def matching(path: String, method: String, query: String, headers: java.util.Map[String, String], body: String, matchers: JSONObject): DescribingResponse = {
      import collection.JavaConversions._
      matching(path, method, Option(query), optional(headers.toMap), optional(body), optional(matchers))
    }

    def matching(path: String,
                 method: String = Get,
                 query: Option[String] = None,
                 headers: Option[Map[String, String]] = None,
                 body: Option[String] = None,
                 matchers: Option[JSONObject] = None): DescribingResponse = {
      DescribingResponse(Request(method, path, query, headers, body, matchers))
    }

    case class DescribingResponse(request: Request) {
      /**
       * supports java DSL
       */
      def willRespondWith(status: Int, headers: java.util.Map[String, String], body: String, matchers: JSONObject): PactWithAtLeastOneRequest = {
        import collection.JavaConversions._
        willRespondWith(status, headers.toMap, body, matchers)
      }

      def willRespondWith(status:Int = 200,
                          headers: Map[String,String] = Map(),
                          body: String = "",
                          matchers: JSONObject = null): PactWithAtLeastOneRequest = {
        builder(
          consumer,
          provider,
          state,
          Seq(Interaction(
            description,
            state,
            request,
            Response(status, headers, body, matchers))))
      }
    }
  }

  case class PactWithAtLeastOneRequest(consumer: Consumer, provider:Provider, state: Option[String], interactions: Seq[Interaction]) {
    def uponReceiving(description: String) = {
      DescribingRequest(consumer, provider, state, description, CanBuildPactFragment.additionalBuild(this))
    }

    def duringConsumerSpec[T](config: MockProviderConfig)(test: => T, verification: ConsumerTestVerification[T]): VerificationResult = {
      PactFragment(consumer, provider, interactions).duringConsumerSpec(config)(test, verification)
    }
  }

  object CanBuildPactFragment {
    type Builder = (Consumer, Provider, Option[String], Seq[Interaction]) => PactWithAtLeastOneRequest

    val firstBuild: Builder = PactWithAtLeastOneRequest.apply

    def additionalBuild(existing: PactWithAtLeastOneRequest): Builder = (_,_,_,i) => existing.copy(interactions = existing.interactions ++ i)
  }
}
