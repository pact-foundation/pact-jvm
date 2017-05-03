package au.com.dius.pact.model

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.{ConsumerTestVerification, VerificationResult}
import org.json.JSONObject

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
object PactFragmentBuilder {
  def apply(consumer: Consumer) = {
    WithConsumer(consumer)
  }

  case class WithConsumer(consumer: Consumer) {
    def hasPactWith(provider: String) = {
      WithProvider(new Provider(provider))
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

  case class DescribingRequest(consumer: Consumer, provider: Provider, state: Option[String], description: String,
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
                 matchers: Map[String, Map[String, String]] = Map()): DescribingResponse = {
      DescribingResponse(new Request(method, path, PactReader.queryStringToMap(query), headers, OptionalBody.body(body),
        CollectionUtils.scalaMMapToJavaMMap(matchers)))
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
                          matchers: Map[String, Map[String, String]] = Map()): PactWithAtLeastOneRequest = {
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
            state.orNull,
            request,
            new Response(status, headers, optionalBody, CollectionUtils.scalaMMapToJavaMMap(matchers)))))
      }

      def willRespondWith(status: Int,
                          headers: Map[String, String],
                          bodyAndMatchers: DslPart): PactWithAtLeastOneRequest = {
        builder(
          consumer,
          provider,
          state,
          Seq(new RequestResponseInteraction(
            description,
            state.orNull,
            request,
            new Response(status, headers, OptionalBody.body(bodyAndMatchers.toString), bodyAndMatchers.getMatchers))))
      }
    }
  }

  case class PactWithAtLeastOneRequest(consumer: Consumer, provider:Provider, state: Option[String], interactions: Seq[RequestResponseInteraction]) {
    def given() = {
      InState(None, this)
    }

    def given(newState: String) = {
      InState(Some(newState), this)
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

    case class InState(newState: Option[String], pactWithAtLeastOneRequest: PactWithAtLeastOneRequest) {
      def uponReceiving(description: String) = {
        DescribingRequest(consumer, provider, newState, description, CanBuildPactFragment.additionalBuild(pactWithAtLeastOneRequest))
      }
    }
  }

  object CanBuildPactFragment {
    type Builder = (Consumer, Provider, Option[String], Seq[RequestResponseInteraction]) => PactWithAtLeastOneRequest

    val firstBuild: Builder = PactWithAtLeastOneRequest.apply

    def additionalBuild(existing: PactWithAtLeastOneRequest): Builder = (_,_,_,i) => existing.copy(interactions = existing.interactions ++ i)
  }
}
