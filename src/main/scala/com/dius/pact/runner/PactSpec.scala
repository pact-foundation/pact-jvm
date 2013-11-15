package com.dius.pact.runner

import scala.concurrent.{Await, ExecutionContext}
import org.scalatest.{Assertions, Sequential, FreeSpec}
import scala.concurrent.duration.Duration
import scala.Predef._
import com.dius.pact.model.Pact
import com.dius.pact.model.Interaction
import scala.Some

class InteractionSpec(pact:Pact, setup: SetupHook, service:Service, interaction:Interaction)(implicit ec:ExecutionContext) extends FreeSpec with JsonComparator with Assertions {
  //TODO: improve reporting to group by provider state
  s"provider ${pact.provider.name}" - {
    s"in state: ${interaction.providerState}" -  {
      s"${interaction.description}" in {
        //TODO: setup configured test timeout
        assert(Await.result(setup.setup(interaction.providerState), Duration.Inf), s"server failed to enter state: ${interaction.providerState}")
        //TODO: setup configured test timeout
        val actual = Await.result(service.invoke(interaction.request), Duration.Inf)
        val expected = interaction.response

        assert(expected.status == actual.status)

        expected.headers.map(_.map { case (k:String, v:String) =>
          val value:String = actual.headers.flatMap(_.get(k)).getOrElse("")
          assert(value == v)
        })

        (expected.body, actual.body) match {
          case (None, _) => true
          case (Some(expectedBody), Some(actualBody)) => compareJson(expectedBody, actualBody)
          case _ => false
        }
      }
    }
  }
}

class PactSpec(config: PactConfiguration, pact:Pact)(implicit ec:ExecutionContext) extends Sequential(
  pact.interactions.map{ i =>
    new InteractionSpec(pact, config.setupHook(pact.provider), config.service(pact.provider), i)
  } :_*
)
