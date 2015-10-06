package au.com.dius.pact.provider

import java.io.File
import org.apache.commons.io.FileUtils
import au.com.dius.pact.model._

object PactFileSource {
  def loadFiles(baseDir: File): Seq[Pact] = {
    import scala.collection.JavaConversions._
    FileUtils.listFiles(baseDir, Array("json"), true).asInstanceOf[java.util.LinkedList[File]].map(PactSerializer.from(_))
  }
}
