package au.com.dius.pact.provider

import java.io.File

import _root_.org.apache.commons.io.FileUtils
import au.com.dius.pact.model._

object PactFileSource {
  def loadFiles(baseDir: File): Seq[RequestResponsePact] = {
    import scala.collection.JavaConversions._
    FileUtils.listFiles(baseDir, Array("json"), true).asInstanceOf[java.util.LinkedList[File]].map(PactReader.loadPact(_).asInstanceOf[RequestResponsePact])
  }
}
