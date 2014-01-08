package com.dius.pact.runner

import java.io.File
import org.apache.commons.io.FileUtils
import com.dius.pact.model._

object PactFileSource {
  def loadFiles(baseDir:String):Seq[Pact] = {
    import scala.collection.JavaConversions._
    FileUtils.listFiles(new File(baseDir), Array("json"), true).asInstanceOf[java.util.LinkedList[File]].map(Pact.from(_))
  }
}
