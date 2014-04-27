package au.com.dius.pact.model

import au.com.dius.pact.consumer.{PactVerification, MockProviderConfig}
import au.com.dius.pact.consumer.PactVerification.PactVerified
import au.com.dius.pact.model.HttpMethod._
import org.json4s._
import scala.Some

case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        state: Option[String],
                        description: String,
                        request: Request,
                        response: Response) {
  def duringConsumerSpec(test: => Unit, config: MockProviderConfig = MockProviderConfig()): PactVerification.VerificationResult = {
    //TODO: integrate ConsumerPact
    PactVerified
  }
}



object PactFragment {
  def consumer(consumer: String) = {
    PactWithConsumer(Consumer(consumer))
  }

  case class PactWithConsumer(consumer: Consumer) {
    def hasPactWith(provider: String) = {
      case class PactWithProvider(provider: Provider) {
        case class PactInState(state: Option[String]) {
          def uponReceiving(description: String) = {
            DescribedRequest(consumer, provider, state, description)
          }
        }

        def given(state: String) = {
          PactInState(Some(state))
        }

        def uponReceiving(description: String) = {
          PactInState(None).uponReceiving(description)
        }
      }
      new PactWithProvider(Provider(provider))
    }
  }

  case class DescribedRequest(consumer: Consumer, provider: Provider, state: Option[String], description: String) {
    def matching(path: String,
                   method: String = Get,
                   headers: Option[Map[String, String]] = None,
                   body: Option[JValue] = None) = {
      class PactForRequest(request: Request) extends Optionals {
        def willRespondWith(status:Int = 200,
                            headers: Map[String,String] = Map(),
                            body: String = null) = {
          PactFragment(consumer,
            provider,
            state,
            description,
            request,
            Response(status, headers, body))
        }
      }
      new PactForRequest(Request(method, path, headers, body))
    }
  }
}
