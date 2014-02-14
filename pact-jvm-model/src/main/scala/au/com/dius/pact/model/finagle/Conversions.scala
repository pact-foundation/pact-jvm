package au.com.dius.pact.model.finagle

import au.com.dius.pact.model.{Response, Request}

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.jboss.netty.handler.codec.http._
import com.twitter.finagle.http.RequestBuilder
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import java.nio.charset.Charset
import scala.collection.JavaConversions._

object Conversions {

  import scala.concurrent.{Future, Promise}
  import com.twitter.util.{Future => TwitterFuture, Throw, Return}

  implicit def fromTwitter[A](twitterFuture: TwitterFuture[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e)  => promise failure e
    }
    promise.future
  }

  def toMethod(method: String): HttpMethod = {
    HttpMethod.valueOf(method.toUpperCase)
  }

  implicit def toChannelBuffer(json: JValue): ChannelBuffer = {
    ChannelBuffers.copiedBuffer(compact(render(json)), Charset.forName("UTF-8"))
  }

  implicit def fromChannelBuffer(cb: ChannelBuffer): String = {
    cb.toString(Charset.forName("UTF-8"))
  }

  def toMap(headers: scala.collection.mutable.Iterable[java.util.Map.Entry[String, String]]): Map[String, String] = {
    headers.map {
      entry => entry.getKey -> entry.getValue
    }.toMap
  }

  implicit def pactToNettyRequest(request: Request): HttpRequest = {
    val builder = RequestBuilder()
      .url(request.path)
      .addHeaders(request.headers.getOrElse(Map()))
    builder.build(toMethod(request.method), request.body.map(toChannelBuffer))
  }

  implicit def nettyToPactRequest(request: HttpRequest): Request = {
    Request(
      request.getMethod.getName,
      request.getUri,
      toMap(request.getHeaders),
      fromChannelBuffer(request.getContent)
    )
  }

  def toStatus(status: Int) = HttpResponseStatus.valueOf(status)

  implicit def pactToNettyResponse(response: Response): HttpResponse = {
    val mutableResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, toStatus(response.status))

    response.headers.foreach {
      headers =>
        headers.foreach {
          case (key, value) => mutableResponse.addHeader(key, value)
        }
    }

    response.body.foreach {
      body =>
        mutableResponse.setContent(toChannelBuffer(body))
    }

    mutableResponse
  }

  implicit def nettyToPactResponse(response: HttpResponse): Response = {
    Response(response.getStatus.getCode, toMap(response.getHeaders), fromChannelBuffer(response.getContent))
  }
}
