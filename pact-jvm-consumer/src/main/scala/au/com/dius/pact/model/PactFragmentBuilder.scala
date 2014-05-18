package au.com.dius.pact.model

import au.com.dius.pact.model.HttpMethod._
import org.json4s._
import au.com.dius.pact.consumer.VerificationResult

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
    def matching(path: String, method: String, headers: java.util.Map[String, String], body: String): DescribingResponse = {
      import collection.JavaConversions._
      matching(path, method, optional(headers.toMap), optional(body))
    }

    def matching(path: String,
                 method: String = Get,
                 headers: Option[Map[String, String]] = None,
                 body: Option[JValue] = None): DescribingResponse = {
      DescribingResponse(Request(method, path, headers, body))
    }

    case class DescribingResponse(request: Request) {
      /**
       * supports java DSL
       */
      def willRespondWith(status: Int, headers: java.util.Map[String, String], body: String): PactWithAtLeastOneRequest = {
        import collection.JavaConversions._
        willRespondWith(status, headers.toMap, body)
      }

      def willRespondWith(status:Int = 200,
                          headers: Map[String,String] = Map(),
                          body: String = ""): PactWithAtLeastOneRequest = {
        builder(
          consumer,
          provider,
          state,
          Seq(Interaction(
            description,
            state.getOrElse(""), //TODO: state shoud be Optional in Interactions
            request,
            Response(status, headers, body))))
      }
    }
  }

  case class PactWithAtLeastOneRequest(consumer: Consumer, provider:Provider, state: Option[String], interactions: Seq[Interaction]) {
    def uponReceiving(description: String) = {
      DescribingRequest(consumer, provider, state, description, CanBuildPactFragment.additionalBuild(this))
    }

    def duringConsumerSpec(config: MockProviderConfig)(test: => Unit): VerificationResult = {
      PactFragment(consumer, provider, interactions).duringConsumerSpec(config)(test)
    }
  }

  object CanBuildPactFragment {
    type Builder = (Consumer, Provider, Option[String], Seq[Interaction]) => PactWithAtLeastOneRequest

    val firstBuild: Builder = PactWithAtLeastOneRequest.apply

    def additionalBuild(existing: PactWithAtLeastOneRequest): Builder = (_,_,_,i) => existing.copy(interactions = existing.interactions ++ i)
  }
}
