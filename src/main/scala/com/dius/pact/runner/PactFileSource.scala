package com.dius.pact.runner

import java.io.File
import org.apache.commons.io.FileUtils
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.dius.pact.model._

object PactFileSource {
  def loadFiles(baseDir:String):Seq[Pact] = {
    import scala.collection.JavaConversions._
    FileUtils.listFiles(new File(baseDir), Array("json"), true).asInstanceOf[java.util.LinkedList[File]].map(loadFile)
  }

  def loadFile(source:File):Pact = {
    def name[T](f:(String) => T) = (j:JsValue) => (j \ "name").validate[String].map(f)
    
    implicit val readProvider:Reads[Provider] = new Reads[Provider] {
      override def reads(j:JsValue) = name(Provider.apply)(j)
    }

    implicit val readConsumer:Reads[Consumer] = new Reads[Consumer] {
      override def reads(j:JsValue) = name(Consumer.apply)(j)
    }

    def read[T](n:String)(implicit r:Reads[T]) = (__ \ n).read[T](r)

    def readOp[T](n:String)(implicit r:Reads[T]) = (__ \ n).readNullable[T](r)

    implicit val readRequest:Reads[Request] = (
      read[String]("method") and
      read[String]("path") and
      readOp[Map[String,String]]("headers") and
      readOp[JsValue]("body")
      ) ((method:String, path:String, headers: Option[Map[String,String]], body:Option[JsValue]) =>
        Request(method, path, headers, body.map(Json.stringify)))

    implicit val readResponse:Reads[Response] = (
      read[Int]("status") and
      readOp[Map[String,String]]("headers") and
      readOp[JsValue]("body")
      )((status:Int, headers:Option[Map[String,String]], body:Option[JsValue]) =>
        Response(status, headers, body.map(Json.stringify)) )

    implicit val readInteraction:Reads[Interaction] = (
      read[String]("description") and
        read[String]("provider_state") and
        read[Request]("request") and
        read[Response]("response")
      )(Interaction.apply _)

    Json.parse(io.Source.fromFile(source).mkString).validate[Pact] ((
      (__ \ "provider").read[Provider] and
      (__ \ "consumer").read[Consumer] and
      (__ \ "interactions").read[Seq[Interaction]]
    ) ( Pact.apply _ )).recoverTotal {
      case e:JsError => throw new RuntimeException(
        s"invalid pact file: ${source.getAbsolutePath}\n${Json.stringify(JsError.toFlatJson(e))}")
    }
  }
}
