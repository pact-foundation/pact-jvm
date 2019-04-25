package au.com.dius.pact.provider

import java.io.File

import au.com.dius.pact.core.model.{PactReader, RequestResponsePact}
import org.apache.commons.io.FileUtils
;

object PactFileSource {
  def loadFiles(baseDir: File): Seq[RequestResponsePact] = {
    import scala.collection.JavaConverters._
    FileUtils.listFiles(baseDir, Array("json"), true).asScala
      .map(PactReader.loadPact(_).asInstanceOf[RequestResponsePact])
      .toSeq
  }
}
