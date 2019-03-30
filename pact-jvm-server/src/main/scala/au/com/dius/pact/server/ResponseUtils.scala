package au.com.dius.pact.server

import scala.collection.JavaConverters._

object ResponseUtils {
  val CrossSiteHeaders = Map[String, java.util.List[String]]("Access-Control-Allow-Origin" -> List("*").asJava)
}
