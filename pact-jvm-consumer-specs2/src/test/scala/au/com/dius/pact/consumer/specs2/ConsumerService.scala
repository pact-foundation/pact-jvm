package au.com.dius.pact.consumer.specs2

import java.util.concurrent.Executors

import au.com.dius.pact.model.OptionalBody

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

case class ConsumerService(serverUrl: String) {
  import Fixtures._
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  private def extractFrom(body: OptionalBody): Boolean = {
    body.orElse("") == "{\"responsetest\": true}"
  }

  def extractResponseTest(path: String = request.getPath): Future[Boolean] = {
    Future {
      TestHttpSupport.INSTANCE.extractResponseTest(serverUrl, path, request)
    }
  }

  def simpleGet(path: String): Future[(Int, String)] = {
    Future {
      val result = TestHttpSupport.INSTANCE.simpleGet(serverUrl, path)
      (result.getFirst, result.getSecond)
    }
  }

  def simpleGet(path: String, query: String): Future[(Int, String)] = {
    Future {
      val result = TestHttpSupport.INSTANCE.simpleGet(serverUrl, path, query)
      (result.getFirst, result.getSecond)
    }
  }

  def options(path: String): Future[(Int, String, Map[String, String])] = {
    Future {
      val result = TestHttpSupport.INSTANCE.options(serverUrl, path)
      (result.getFirst, result.getSecond, result.getThird.toMap)
    }
  }
}
