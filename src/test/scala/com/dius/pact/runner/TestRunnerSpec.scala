package com.dius.pact.runner

import org.specs2.mutable.Specification
import com.dius.pact.model.Fixtures._
import org.specs2.mock.Mockito
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class TestRunnerSpec extends Specification with Mockito {
  "test runner" should {
    "invoke collaborators" in {
      val setupHook = mock[SetupHook]
      val service = mock[Service]

      service.invoke(interaction.request) returns Future.successful(interaction.response)

      val result = new TestRunner(setupHook, service).run(interaction)

      result must beEqualTo(true).await
      there was one(setupHook).setup(interaction.providerState)
      there was one(service).invoke(interaction.request)
    }

    "failure" in {
      val setupHook = mock[SetupHook]
      val service = mock[Service]

      service.invoke(interaction.request) returns Future.successful(secondResponse)

      val result = new TestRunner(setupHook, service).run(interaction)

      result must beEqualTo(false).await
      there was one(setupHook).setup(interaction.providerState)
      there was one(service).invoke(interaction.request)
    }
  }
}
