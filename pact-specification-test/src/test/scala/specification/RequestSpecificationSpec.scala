package specification

import au.com.dius.pact.model.{FullRequestMatch, Response, Interaction, RequestMatching}
import java.io.{FilenameFilter, File}
import org.specs2.SpecificationLike
import org.specs2.matcher.{StandardMatchResults, MustMatchers}
import org.specs2.execute.StandardResults
import org.specs2.specification.{Example, Fragments, FragmentsBuilder}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RequestSpecificationSpec extends SpecificationLike
  with MustMatchers
  with StandardResults
  with StandardMatchResults
  with FragmentsBuilder {

  val jsonFilter = new FilenameFilter {
    def accept(dir: File, name: String): Boolean = name.endsWith(".json")
  }

  def fragments: Seq[Example] = new File("src/main/resources/request").listFiles().flatMap { folder =>
    if(folder.isDirectory) {
      val dirName = folder.getName
      folder.listFiles(jsonFilter).map { testFile =>
        val fileName = testFile.getName
        implicit val formats = DefaultFormats
        val testJson = parse(testFile)
        var testData = testJson.transformField {
          case ("body", value) => ("body", JString(pretty(value)))
        }.extract[PactRequestSpecification]

        testData = testData.copy(expected = testData.expected.copy(matchers =
          ((testJson \ "expected") \ "requestMatchingRules").extract[Option[Map[String,Map[String,String]]]]),
          actual = testData.actual.copy(matchers =
            ((testJson \ "actual") \ "requestMatchingRules").extract[Option[Map[String,Map[String,String]]]])
        )

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

  def test(input: PactRequestSpecification) = {
    val fakeInteraction = Interaction("", None, input.expected, Response(200, Map[String, String](), "", null))
    val result = RequestMatching.compareRequest(fakeInteraction, input.actual)
    if(input.`match`) {
      result mustEqual FullRequestMatch(fakeInteraction)
    } else {
      result mustNotEqual FullRequestMatch(fakeInteraction)
    }
  }
}
