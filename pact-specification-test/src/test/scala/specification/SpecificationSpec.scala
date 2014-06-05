package specification

import au.com.dius.pact.model.RequestMatching
import java.io.{FilenameFilter, File}
import org.specs2.SpecificationLike
import org.specs2.matcher.{StandardMatchResults, MustMatchers}
import org.specs2.execute.StandardResults
import org.specs2.specification.{Example, Fragments, FragmentsBuilder}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats
import au.com.dius.pact.model.Matching.MatchFound

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SpecificationSpec extends SpecificationLike
  with MustMatchers
  with StandardResults
  with StandardMatchResults
  with FragmentsBuilder {

  val jsonFilter = new FilenameFilter {
    def accept(dir: File, name: String): Boolean = name.endsWith(".json")
  }

  def fragments: Seq[Example] = new File("pact-specification-test/src/test/resources/request").listFiles().flatMap { folder =>
    if(folder.isDirectory) {
      val dirName = folder.getName
      folder.listFiles(jsonFilter).map { testFile =>
        val fileName = testFile.getName
        implicit val formats = DefaultFormats
        val testData = parse(testFile).extract[PactSpecification]

        val description = s"$dirName/$fileName ${testData.comment}"
        Example(description, {
          test(testData)
        })
      }
    } else {
      Seq()
    }
  }
  override def is: Fragments = Fragments.create(fragments :_*)

  def test(input: PactSpecification) = {
    val result = RequestMatching.compareRequests(input.expected, input.actual)
    if(input.`match`) {
      result mustEqual MatchFound
    } else {
      result mustNotEqual MatchFound
    }
  }
}