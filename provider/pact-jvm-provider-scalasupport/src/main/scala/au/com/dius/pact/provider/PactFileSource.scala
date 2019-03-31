package au.com.dius.pact.provider;

object PactFileSource {
  def loadFiles(baseDir: File): Seq[RequestResponsePact] = {
    import scala.collection.JavaConversions._
    FileUtils.listFiles(baseDir, Array("json"), true).asInstanceOf[java.util.LinkedList[File]].map(PactReader.loadPact(_).asInstanceOf[RequestResponsePact])
  }
}
